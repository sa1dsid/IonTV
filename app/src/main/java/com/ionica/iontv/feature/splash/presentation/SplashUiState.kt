package com.ionica.iontv.feature.splash.presentation

import com.ionica.iontv.feature.splash.domain.StartDestination

data class SplashUiState(
    val offline: Boolean = false,
    val destination: StartDestination? = null,
)
