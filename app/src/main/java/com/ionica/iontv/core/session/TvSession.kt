package com.ionica.iontv.core.session

/**
 * Сессия телевизора. Это ограниченная TV-сессия бэкенда: она не может привязывать
 * другие устройства и управлять аккаунтом. Аккаунтный токен на ТВ не хранится.
 */
data class TvSession(
    val accessToken: String,
    val refreshToken: String,
    val bindingId: String?,
    /** Имя аккаунта для «linked to @…»: есть после входа через Google или почту, после QR — нет. */
    val username: String?,
)

/** Токены TV-сессии, как их отдают pairing/poll, devices/register и auth/refresh. */
data class SessionTokens(
    val accessToken: String,
    val refreshToken: String,
    val bindingId: String?,
)
