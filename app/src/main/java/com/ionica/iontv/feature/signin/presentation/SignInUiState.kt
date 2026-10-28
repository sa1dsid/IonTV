package com.ionica.iontv.feature.signin.presentation

data class SignInUiState(
    val pairing: PairingUi = PairingUi.Loading,
)

sealed interface PairingUi {
    data object Loading : PairingUi

    data object Offline : PairingUi

    data class Ready(val code: String, val qrContent: String) : PairingUi
}

sealed interface SignInEvent {
    data object Linked : SignInEvent
}
