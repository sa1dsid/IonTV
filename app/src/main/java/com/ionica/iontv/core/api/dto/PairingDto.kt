package com.ionica.iontv.core.api.dto

import com.ionica.iontv.core.session.SessionTokens
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

private const val STATUS_PAIRED = "paired"

// По ТЗ ТВ спрашивает статус привязки раз в 2 секунды; сервер присылает то же число.
private const val DEFAULT_POLL_INTERVAL_SECONDS = 2L

/** Тело pairing/code: какой телевизор привязываем и как он будет называться в кабинете. */
@Serializable
data class DeviceDto(
    @SerialName("device_id") val deviceId: String,
    @SerialName("name") val name: String,
)

/**
 * [qrUrl] — ссылка на сайт с одноразовым токеном, её и рисуем в QR; адрес сайта сервер
 * берёт из своего конфига. [code] — запасной ручной ввод. [pollToken] не показываем.
 */
@Serializable
data class PairingCodeDto(
    @SerialName("pair_id") val pairId: String,
    @SerialName("code") val code: String,
    @SerialName("qr_url") val qrUrl: String,
    @SerialName("poll_token") val pollToken: String,
    @SerialName("expires_in") val expiresIn: Long,
    @SerialName("poll_interval") val pollInterval: Long = DEFAULT_POLL_INTERVAL_SECONDS,
)

@Serializable
data class PairingPollRequestDto(
    @SerialName("pair_id") val pairId: String,
    @SerialName("poll_token") val pollToken: String,
)

/** pending — на сайте ещё не подтвердили; paired — привязали, в ответе токены TV-сессии. */
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
