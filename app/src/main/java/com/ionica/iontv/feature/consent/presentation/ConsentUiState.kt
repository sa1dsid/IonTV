package com.ionica.iontv.feature.consent.presentation

data class ConsentUiState(
    val submitting: Boolean = false,
    val declining: Boolean = false,
    val error: ConsentError? = null,
)

enum class ConsentError { NO_CONNECTION, SERVER }

sealed interface ConsentEvent {
    data class Agreed(val setupPending: Boolean) : ConsentEvent
    data object ExitApp : ConsentEvent
}
