package com.ionica.iontv.core.api.dto

import com.ionica.iontv.core.session.SessionTokens
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class HealthDto(
    @SerialName("status") val status: String,
    @SerialName("enabled") val enabled: Boolean = true,
)

@Serializable
data class TokenInputDto(
    @SerialName("token") val token: String,
)

@Serializable
data class SessionTokensDto(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String,
    @SerialName("expires_in") val expiresIn: Long = 0,
    @SerialName("binding_id") val bindingId: String? = null,
)

fun SessionTokensDto.toTokens() = SessionTokens(
    accessToken = accessToken,
    refreshToken = refreshToken,
    bindingId = bindingId,
)

@Serializable
data class ConsentDto(
    @SerialName("granted") val granted: Boolean,
    @SerialName("version") val version: String,
    @SerialName("screen") val screen: String,
    @SerialName("timestamp") val timestamp: String? = null,
    @SerialName("device_info") val deviceInfo: ConsentDeviceInfoDto? = null,
)

@Serializable
data class ConsentDeviceInfoDto(
    @SerialName("manufacturer") val manufacturer: String?,
    @SerialName("model") val model: String?,
    @SerialName("os_version") val osVersion: String?,
    @SerialName("app_version") val appVersion: String?,
)

@Serializable
data class ConsentStateDto(
    @SerialName("granted") val granted: Boolean,
)
