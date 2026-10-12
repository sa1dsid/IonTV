package com.ionica.iontv.feature.signin.domain.model

import kotlin.time.Duration

/** Код сопряжения: [code] видит пользователь, [pollToken] остаётся на телевизоре. */
data class PairingCode(
    val code: String,
    val pollToken: String,
    val lifetime: Duration,
)

sealed interface PairingPoll {
    /** Код ещё не ввели — или опрос не дошёл до сервера, и код может быть жив. */
    data object Pending : PairingPoll

    /** Телевизор привязан, TV-сессия уже сохранена. */
    data object Linked : PairingPoll

    data object Expired : PairingPoll
}
