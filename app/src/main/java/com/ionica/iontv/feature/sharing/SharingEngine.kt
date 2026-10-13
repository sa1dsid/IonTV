package com.ionica.iontv.feature.sharing

import com.ionica.iontv.core.network.NetworkMonitor
import com.ionica.iontv.core.network.NetworkStatus
import com.ionica.iontv.core.session.SessionStore
import com.ionica.iontv.core.system.BackgroundPermissions
import com.ionica.iontv.feature.sharing.data.HeartbeatOutcome
import com.ionica.iontv.feature.sharing.data.HeartbeatRepository
import com.ionica.iontv.feature.sharing.domain.model.PauseReason
import com.ionica.iontv.feature.sharing.domain.model.SharingState
import com.ionica.iontv.feature.sharing.domain.model.SharingStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

// Ориентир бэкенд-ТЗ — heartbeat раз в 10–15 минут. Берём нижнюю границу: начисление
// идёт по подтверждённым интервалам, и короткий интервал меньше теряет при простое.
private val HEARTBEAT_INTERVAL = 10.minutes

private val RETRY_INTERVAL = 1.minutes

// Оптимизацию батареи система не присылает событием — проверяем её сами.
private val LOCAL_CHECK_INTERVAL = 30.seconds

private const val STATUS_PAUSED = "paused"

private const val STATUS_OFFLINE = "offline"

/**
 * Раздача трафика на телевизоре: шлёт heartbeat, пока жива TV-сессия, и собирает
 * статус для дашборда. Работает внутри SharingService.
 *
 * Модуль прокси переиспользуется из другого продукта и сюда ещё не подключён: до тех
 * пор service_ok означает, что запущен сам сервис раздачи.
 */
@Singleton
class SharingEngine @Inject constructor(
    private val heartbeats: HeartbeatRepository,
    private val networkMonitor: NetworkMonitor,
    private val permissions: BackgroundPermissions,
    private val sessionStore: SessionStore,
) : SharingMonitor {

    private val running = Mutex()

    private val batteryOk = MutableStateFlow(true)

    private val server = MutableStateFlow<ServerView?>(null)

    override val state: Flow<SharingState> = combine(networkMonitor.status, batteryOk, server) { network, battery, server ->
        SharingState(status = resolveStatus(network, battery, server), connection = network.connection)
    }

    /** Возвращается, когда сессия кончилась: ТВ отвязали или бэкенд отклонил токен. */
    suspend fun run() = running.withLock {
        while (sessionStore.current() != null) {
            val conditions = readConditions()
            if (!conditions.network.isOnline) {
                awaitChange(conditions, timeout = null)
                continue
            }

            val outcome = heartbeats.send(
                batteryOk = conditions.batteryOk,
                serviceOk = true,
                connection = conditions.network.connection,
            )
            when (outcome) {
                is HeartbeatOutcome.Accepted -> {
                    server.value = ServerView.Answered(outcome.status, outcome.reason)
                    awaitChange(conditions, timeout = HEARTBEAT_INTERVAL)
                }

                HeartbeatOutcome.Unbound -> {
                    sessionStore.close()
                    server.value = null
                }

                HeartbeatOutcome.Failed -> {
                    server.value = ServerView.Unreachable
                    awaitChange(conditions, timeout = RETRY_INTERVAL)
                }
            }
        }
    }

    override fun recheckBattery() {
        batteryOk.value = isBatteryOk()
    }

    /**
     * Ждёт следующего heartbeat, но просыпается раньше, если пропала или вернулась сеть,
     * сменился тип подключения или оптимизация батареи: бэкенд должен узнать об этом сразу.
     */
    private suspend fun awaitChange(sent: Conditions, timeout: Duration?) {
        val changed = suspend {
            merge(networkMonitor.status.map { }, ticker(LOCAL_CHECK_INTERVAL))
                .map { readConditions() }
                .first { it != sent }
        }
        if (timeout == null) changed() else withTimeoutOrNull(timeout) { changed() }
    }

    private fun readConditions(): Conditions {
        val conditions = Conditions(network = networkMonitor.current(), batteryOk = isBatteryOk())
        batteryOk.value = conditions.batteryOk
        return conditions
    }

    // Многие телевизоры не дают снять оптимизацию батареи — экрана для этого нет. Они
    // работают от сети, и мешать раздаче такая «оптимизация» не может.
    private fun isBatteryOk(): Boolean =
        permissions.isBatteryOptimizationIgnored() || !permissions.canRequestBatteryExemption()

    private fun ticker(period: Duration): Flow<Unit> = flow {
        while (true) {
            delay(period)
            emit(Unit)
        }
    }

    private data class Conditions(val network: NetworkStatus, val batteryOk: Boolean)

    private sealed interface ServerView {
        data class Answered(val status: String, val reason: String?) : ServerView
        data object Unreachable : ServerView
    }

    private fun resolveStatus(network: NetworkStatus, batteryOk: Boolean, server: ServerView?): SharingStatus =
        when {
            !network.isOnline -> SharingStatus.Offline
            // Своё знание свежее: не ждём следующего heartbeat, чтобы показать паузу.
            !batteryOk -> SharingStatus.Paused(PauseReason.BATTERY_OPTIMIZATION)
            server == ServerView.Unreachable -> SharingStatus.Offline
            server is ServerView.Answered && server.status == STATUS_PAUSED ->
                SharingStatus.Paused(server.reason.toPauseReason())

            server is ServerView.Answered && server.status == STATUS_OFFLINE -> SharingStatus.Offline
            else -> SharingStatus.Active
        }

    private fun String?.toPauseReason(): PauseReason {
        val reason = this.orEmpty().lowercase()
        return when {
            "battery" in reason -> PauseReason.BATTERY_OPTIMIZATION
            "consent" in reason -> PauseReason.CONSENT_REQUIRED
            else -> PauseReason.OTHER
        }
    }
}
