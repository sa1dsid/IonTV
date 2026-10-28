package com.ionica.iontv.feature.sharing.data

import com.ionica.iontv.core.api.IonTvApi
import com.ionica.iontv.core.api.ServerClock
import com.ionica.iontv.core.api.dto.HeartbeatDto
import com.ionica.iontv.core.common.runCatchingCancellable
import com.ionica.iontv.core.network.Connection
import java.util.UUID
import javax.inject.Inject

private const val CONNECTION_ETHERNET = "ethernet"

private const val CONNECTION_WIFI = "wifi"

sealed interface HeartbeatOutcome {
    data class Accepted(val status: String, val reason: String?, val username: String?) : HeartbeatOutcome

    data object Unbound : HeartbeatOutcome

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
                    timestamp = serverClock.nowIso(),
                    batteryOk = batteryOk,
                    serviceOk = serviceOk,
                    connection = connection.apiValue(),
                )
            )
        }.fold(
            onSuccess = { result ->
                if (result.bound) {
                    HeartbeatOutcome.Accepted(result.status, result.reason, result.username)
                } else {
                    HeartbeatOutcome.Unbound
                }
            },
            onFailure = { HeartbeatOutcome.Failed },
        )

    private fun Connection.apiValue(): String =
        if (this == Connection.ETHERNET) CONNECTION_ETHERNET else CONNECTION_WIFI
}
