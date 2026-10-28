package com.ionica.iontv.core.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProxyHeartbeatDto(
    @SerialName("app_name") val appName: String,
    @SerialName("app_version") val appVersion: String,
    @SerialName("sdk_version") val sdkVersion: String,
    @SerialName("os_version") val osVersion: String,
    @SerialName("hardware_id") val hardwareId: String? = null,
    @SerialName("ping") val ping: Int? = null,
    @SerialName("proxy_version") val proxyVersion: String,
    @SerialName("proxy_versions") val proxyVersions: List<String>,
)

@Serializable
data class ProxyDisconnectedDto(
    @SerialName("disconnected") val disconnected: Boolean,
)
