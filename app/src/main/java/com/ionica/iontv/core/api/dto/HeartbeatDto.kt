package com.ionica.iontv.core.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Heartbeat телевизора. [eventId] новый на каждый heartbeat: тот же id с другим телом
 * бэкенд отклоняет как подмену. [timestamp] — по часам сервера, а не приставки.
 */
@Serializable
data class HeartbeatDto(
    @SerialName("event_id") val eventId: String,
    @SerialName("timestamp") val timestamp: String,
    @SerialName("battery_ok") val batteryOk: Boolean,
    @SerialName("service_ok") val serviceOk: Boolean,
    @SerialName("connection") val connection: String,
)

/** bound=false — ТВ отвязали в IO Aura или на сайте Check, status тогда «unbound». */
@Serializable
data class HeartbeatResultDto(
    @SerialName("bound") val bound: Boolean,
    @SerialName("status") val status: String,
    @SerialName("reason") val reason: String? = null,
)
