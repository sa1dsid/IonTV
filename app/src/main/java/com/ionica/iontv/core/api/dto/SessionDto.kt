package com.ionica.iontv.core.api.dto

import com.ionica.iontv.core.session.SessionTokens
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class HealthDto(
    @SerialName("status") val status: String,
    @SerialName("enabled") val enabled: Boolean = true,
)

/** Тело auth/refresh: у нативного клиента refresh-токен идёт в JSON, а не в cookie. */
@Serializable
data class TokenInputDto(
    @SerialName("token") val token: String,
)

/** Токены TV-сессии. Нативные эндпоинты IONTV отвечают в snake_case. */
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
