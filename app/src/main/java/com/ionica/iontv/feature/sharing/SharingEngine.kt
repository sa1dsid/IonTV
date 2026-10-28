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
import com.ionica.iontv.feature.sharing.proxy.ProxyClient
import io.proxysdk.ProxyCommand
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

private val HEARTBEAT_INTERVAL = 10.minutes

private val RETRY_INTERVAL = 1.minutes

private val LOCAL_CHECK_INTERVAL = 30.seconds

private const val STATUS_PAUSED = "paused"

private const val STATUS_OFFLINE = "offline"

private val NO_PROXY_REASONS = setOf(ProxyCommand.REASON_NO_CAPACITY, ProxyCommand.REASON_CONNECT_FAILED)

@Singleton
class SharingEngine @Inject constructor(
    private val heartbeats: HeartbeatRepository,
    private val networkMonitor: NetworkMonitor,
    private val permissions: BackgroundPermissions,
    private val sessionStore: SessionStore,
    private val proxy: ProxyClient,
) : SharingMonitor {

    private val running = Mutex()

    private val batteryOk = MutableStateFlow(true)

    private val server = MutableStateFlow<ServerView?>(null)

    private val refreshes = Channel<Unit>(Channel.CONFLATED)

    override val state: Flow<SharingState> =
        combine(
            networkMonitor.status,
            batteryOk,
            server,
            proxy.stopReason,
            sessionStore.username,
        ) { network, battery, server, proxyStop, username ->
            SharingState(
                status = resolveStatus(network, battery, server, proxyStop),
                connection = network.connection,
                username = username,
            )
        }

    suspend fun run() = running.withLock {
        refreshes.tryReceive()
        while (sessionStore.current() != null) {
            val conditions = readConditions()
            if (!conditions.network.isOnline) {
                awaitChange(conditions, timeout = null)
                continue
            }

            val outcome = heartbeats.send(
                batteryOk = conditions.batteryOk,
                serviceOk = conditions.proxyServing,
                connection = conditions.network.connection,
            )
            when (outcome) {
                is HeartbeatOutcome.Accepted -> {
                    server.value = ServerView.Answered(outcome.status, outcome.reason)
                    outcome.username?.takeIf { it.isNotBlank() }?.let { sessionStore.saveUsername(it) }
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

    override fun refresh() {
        server.value = null
        refreshes.trySend(Unit)
    }

    private suspend fun awaitChange(sent: Conditions, timeout: Duration?) {
        val changed = suspend {
            val conditionsChanged = merge(
                networkMonitor.status.map { },
                proxy.serving.map { },
                ticker(LOCAL_CHECK_INTERVAL),
            )
                .map { readConditions() }
                .filter { it != sent }
                .map { }
            merge(conditionsChanged, refreshes.receiveAsFlow()).first()
        }
        if (timeout == null) changed() else withTimeoutOrNull(timeout) { changed() }
    }

    private fun readConditions(): Conditions {
        val conditions = Conditions(
            network = networkMonitor.current(),
            batteryOk = isBatteryOk(),
            proxyServing = proxy.isServing(),
        )
        batteryOk.value = conditions.batteryOk
        return conditions
    }

    private fun isBatteryOk(): Boolean =
        permissions.isBatteryOptimizationIgnored() || !permissions.canRequestBatteryExemption()

    private fun ticker(period: Duration): Flow<Unit> = flow {
        while (true) {
            delay(period)
            emit(Unit)
        }
    }

    private data class Conditions(val network: NetworkStatus, val batteryOk: Boolean, val proxyServing: Boolean)

    private sealed interface ServerView {
        data class Answered(val status: String, val reason: String?) : ServerView
        data object Unreachable : ServerView
    }

    private fun resolveStatus(
        network: NetworkStatus,
        batteryOk: Boolean,
        server: ServerView?,
        proxyStop: String?,
    ): SharingStatus {
        val answered = server.toStatus()
        return when {
            !network.isOnline -> SharingStatus.Offline
            !batteryOk -> SharingStatus.Paused(PauseReason.BATTERY_OPTIMIZATION)
            answered == SharingStatus.Paused(PauseReason.CONSENT_REQUIRED) -> answered
            proxyStop == ProxyCommand.REASON_VPN_OR_PROXY -> SharingStatus.Paused(PauseReason.VPN_OR_PROXY)
            answered != null -> answered
            proxyStop in NO_PROXY_REASONS -> SharingStatus.Paused(PauseReason.NO_PROXY)
            else -> SharingStatus.Active
        }
    }

    private fun ServerView?.toStatus(): SharingStatus? = when {
        this == ServerView.Unreachable -> SharingStatus.Offline
        this is ServerView.Answered && status == STATUS_PAUSED -> SharingStatus.Paused(reason.toPauseReason())
        this is ServerView.Answered && status == STATUS_OFFLINE -> SharingStatus.Offline
        else -> null
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
