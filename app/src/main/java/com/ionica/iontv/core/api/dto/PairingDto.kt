package com.ionica.iontv.core.api.dto

import com.ionica.iontv.core.session.SessionTokens
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

private const val STATUS_PAIRED = "paired"

/** Тело pairing/code и devices/register: оба привязывают один и тот же телевизор. */
@Serializable
data class DeviceDto(
    @SerialName("device_id") val deviceId: String,
    @SerialName("name") val name: String,
)

@Serializable
data class PairingCodeDto(
    @SerialName("code") val code: String,
    @SerialName("poll_token") val pollToken: String,
    @SerialName("expires_in") val expiresIn: Long,
)

@Serializable
data class PairingPollRequestDto(
    @SerialName("code") val code: String,
    @SerialName("poll_token") val pollToken: String,
)

/** pending — код ещё не ввели; paired — привязали, в ответе токены TV-сессии. */
@Serializable
data class PairingPollDto(
    @SerialName("status") val status: String,
    @SerialName("access_token") val accessToken: String? = null,
    @SerialName("refresh_token") val refreshToken: String? = null,
    @SerialName("binding_id") val bindingId: String? = null,
) {
    fun tokensOrNull(): SessionTokens? {
        if (status != STATUS_PAIRED) return null
        return SessionTokens(
            accessToken = accessToken ?: return null,
            refreshToken = refreshToken ?: return null,
            bindingId = bindingId,
        )
    }
}
