package com.ionica.iontv.feature.sharing.data

import com.ionica.iontv.core.api.IonTvApi
import com.ionica.iontv.core.api.ServerClock
import com.ionica.iontv.core.api.dto.HeartbeatDto
import com.ionica.iontv.core.common.runCatchingCancellable
import com.ionica.iontv.core.network.Connection
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import javax.inject.Inject

private const val CONNECTION_ETHERNET = "ethernet"

private const val CONNECTION_WIFI = "wifi"

sealed interface HeartbeatOutcome {
    /** [status] — active, paused или offline, как его видит бэкенд; [reason] — что исправить. */
    data class Accepted(val status: String, val reason: String?) : HeartbeatOutcome

    /** Телевизор отвязали в IO Aura или на сайте Check. */
    data object Unbound : HeartbeatOutcome

    /** До бэкенда не достучались или он ответил ошибкой — попробуем позже. */
    data object Failed : HeartbeatOutcome
}

class HeartbeatRepository @Inject constructor(
    private val api: IonTvApi,
    private val serverClock: ServerClock,
) {

    suspend fun send(batteryOk: Boolean, serviceOk: Boolean, connection: Connection): HeartbeatOutcome =
        runCatchingCancellable {
            api.heartbeat(
                HeartbeatDto(
                    eventId = UUID.randomUUID().toString(),
                    timestamp = isoUtc(serverClock.nowMillis()),
                    batteryOk = batteryOk,
                    serviceOk = serviceOk,
                    connection = connection.apiValue(),
                )
            )
        }.fold(
            onSuccess = { result ->
                if (result.bound) HeartbeatOutcome.Accepted(result.status, result.reason) else HeartbeatOutcome.Unbound
            },
            onFailure = { HeartbeatOutcome.Failed },
        )

    // Бэкенд знает только кабель и Wi-Fi; редкий ТВ на мобильном модеме считаем Wi-Fi.
    private fun Connection.apiValue(): String =
        if (this == Connection.ETHERNET) CONNECTION_ETHERNET else CONNECTION_WIFI

    private fun isoUtc(millis: Long): String =
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
            .apply { timeZone = TimeZone.getTimeZone("UTC") }
            .format(millis)
}
