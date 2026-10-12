package com.ionica.iontv.feature.consent.presentation

data class ConsentUiState(
    val submitting: Boolean = false,
    /** Попап 3b: без согласия телевизор дальше не пускает. */
    val declining: Boolean = false,
    val error: ConsentError? = null,
)

enum class ConsentError { NO_CONNECTION, SERVER }

sealed interface ConsentEvent {
    data object Agreed : ConsentEvent
    data object ExitApp : ConsentEvent
}
