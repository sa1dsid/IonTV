package com.ionica.iontv.feature.signin.presentation

data class SignInUiState(
    val pairing: PairingUi = PairingUi.Loading,
    val googleAvailable: Boolean = false,
    val linking: Boolean = false,
    val error: SignInError? = null,
)

sealed interface PairingUi {
    data object Loading : PairingUi

    /** Код не получить: нет связи. Повторяем сами, как на заставке. */
    data object Offline : PairingUi

    data class Ready(val code: String) : PairingUi

    /** Код протух — «Code expired» и кнопка Refresh на месте QR. */
    data object Expired : PairingUi
}

/** Ошибки показываются строкой под кнопками — попапов на экране входа нет. */
enum class SignInError { NO_CONNECTION, SERVER, GOOGLE_UNAVAILABLE }

sealed interface SignInEvent {
    data object Linked : SignInEvent
}
