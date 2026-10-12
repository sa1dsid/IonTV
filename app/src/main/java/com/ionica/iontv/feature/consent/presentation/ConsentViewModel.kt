package com.ionica.iontv.feature.consent.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ionica.iontv.core.api.apiError
import com.ionica.iontv.feature.consent.data.ConsentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ConsentViewModel @Inject constructor(
    private val consent: ConsentRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ConsentUiState())
    val uiState: StateFlow<ConsentUiState> = _uiState.asStateFlow()

    private val eventChannel = Channel<ConsentEvent>(Channel.BUFFERED)
    val events: Flow<ConsentEvent> = eventChannel.receiveAsFlow()

    fun onAgreeClick() {
        if (_uiState.value.submitting) return

        viewModelScope.launch {
            _uiState.update { it.copy(submitting = true, error = null) }
            consent.grant()
                .onSuccess { eventChannel.send(ConsentEvent.Agreed) }
                .onFailure { error ->
                    val reason = if (error.apiError() == null) ConsentError.NO_CONNECTION else ConsentError.SERVER
                    _uiState.update { it.copy(submitting = false, error = reason) }
                }
        }
    }

    /** «Назад» на экране согласия — тоже отказ: без него дальше не пускаем. */
    fun onDeclineClick() {
        _uiState.update { it.copy(declining = true) }
    }

    fun onGoBackClick() {
        _uiState.update { it.copy(declining = false) }
    }

    fun onExitClick() {
        eventChannel.trySend(ConsentEvent.ExitApp)
    }
}
