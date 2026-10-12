package com.ionica.iontv.feature.splash.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ionica.iontv.core.api.BackendHealth
import com.ionica.iontv.core.common.TimeSource
import com.ionica.iontv.feature.splash.domain.ResolveStartDestinationUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// Проверка обычно укладывается в доли секунды; без минимума заставка мигнула бы кадром.
private const val MIN_VISIBLE_MILLIS = 600L

private val RETRY_DELAYS_MILLIS = listOf(2_000L, 3_000L, 5_000L)

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val backendHealth: BackendHealth,
    private val resolveStartDestination: ResolveStartDestinationUseCase,
    private val timeSource: TimeSource,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SplashUiState())
    val uiState: StateFlow<SplashUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val startedAt = timeSource.nowMillis()

            awaitBackend()
            val destination = resolveStartDestination()

            val elapsed = timeSource.nowMillis() - startedAt
            if (elapsed < MIN_VISIBLE_MILLIS) delay(MIN_VISIBLE_MILLIS - elapsed)
            _uiState.update { it.copy(destination = destination) }
        }
    }

    /** По макету кнопок на заставке нет: пока сервер недоступен, повторяем сами. */
    private suspend fun awaitBackend() {
        var attempt = 0
        while (!backendHealth.isServing()) {
            _uiState.update { it.copy(offline = true) }
            delay(RETRY_DELAYS_MILLIS[attempt.coerceAtMost(RETRY_DELAYS_MILLIS.lastIndex)])
            attempt++
        }
        _uiState.update { it.copy(offline = false) }
    }
}
