package com.ionica.iontv.feature.signin.domain.model

import kotlin.time.Duration

/**
 * Привязка, которую подтверждают на сайте IonTV Pulse. [qrContent] — ссылка на сайт
 * с одноразовым токеном, [code] — короткий код для ручного ввода, а [pollToken]
 * остаётся на телевизоре.
 */
data class PairingCode(
    val pairId: String,
    val code: String,
    val qrContent: String,
    val pollToken: String,
    val lifetime: Duration,
    val pollInterval: Duration,
)

sealed interface PairingPoll {
    /** На сайте ещё не подтвердили — или опрос не дошёл до сервера, и привязка может быть жива. */
    data object Pending : PairingPoll

    /** Телевизор привязан, TV-сессия уже сохранена. */
    data object Linked : PairingPoll

    data object Expired : PairingPoll
}
