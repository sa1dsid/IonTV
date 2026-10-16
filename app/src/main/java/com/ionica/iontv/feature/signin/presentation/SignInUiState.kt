package com.ionica.iontv.feature.signin.presentation

data class SignInUiState(
    val pairing: PairingUi = PairingUi.Loading,
)

sealed interface PairingUi {
    /** Берём привязку — в том числе новую, когда прежняя истекла. */
    data object Loading : PairingUi

    /** Привязку не получить: нет связи. Повторяем сами, как на заставке. */
    data object Offline : PairingUi

    /** [qrContent] — ссылка на сайт с токеном для QR, [code] — для ручного ввода. */
    data class Ready(val code: String, val qrContent: String) : PairingUi
}

sealed interface SignInEvent {
    data object Linked : SignInEvent
}
