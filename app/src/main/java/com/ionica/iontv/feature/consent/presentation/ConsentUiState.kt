package com.ionica.iontv.feature.consent.presentation

data class ConsentUiState(
    val submitting: Boolean = false,
    /** Попап 3b: без согласия телевизор дальше не пускает. */
    val declining: Boolean = false,
    val error: ConsentError? = null,
)

enum class ConsentError { NO_CONNECTION, SERVER }

sealed interface ConsentEvent {
    /** [setupPending] — чеклист на этом телевизоре ещё не проходили. */
    data class Agreed(val setupPending: Boolean) : ConsentEvent
    data object ExitApp : ConsentEvent
}
