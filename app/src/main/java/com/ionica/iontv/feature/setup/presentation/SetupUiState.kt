package com.ionica.iontv.feature.setup.presentation

data class SetupUiState(
    val backgroundActivityDone: Boolean = false,
    val autostartDone: Boolean = false,
) {
    val canContinue: Boolean
        get() = backgroundActivityDone && autostartDone
}

enum class SetupStep { BACKGROUND_ACTIVITY, AUTOSTART }

sealed interface SetupEvent {
    data object Finished : SetupEvent
}
