package com.ionica.iontv.core.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class HeartbeatDto(
    @SerialName("event_id") val eventId: String,
    @SerialName("timestamp") val timestamp: String,
    @SerialName("battery_ok") val batteryOk: Boolean,
    @SerialName("service_ok") val serviceOk: Boolean,
    @SerialName("connection") val connection: String,
)

@Serializable
data class HeartbeatResultDto(
    @SerialName("bound") val bound: Boolean,
    @SerialName("status") val status: String,
    @SerialName("reason") val reason: String? = null,
    @SerialName("username") val username: String? = null,
)
