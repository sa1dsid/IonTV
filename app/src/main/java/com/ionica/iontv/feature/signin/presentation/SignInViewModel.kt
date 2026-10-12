package com.ionica.iontv.feature.signin.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ionica.iontv.feature.signin.data.PairingRepository
import com.ionica.iontv.feature.signin.domain.model.PairingCode
import com.ionica.iontv.feature.signin.domain.model.PairingPoll
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

private val POLL_INTERVAL = 3.seconds

private val CODE_RETRY_DELAY = 5.seconds

@HiltViewModel
class SignInViewModel @Inject constructor(
    private val pairing: PairingRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SignInUiState())
    val uiState: StateFlow<SignInUiState> = _uiState.asStateFlow()

    private val eventChannel = Channel<SignInEvent>(Channel.BUFFERED)
    val events: Flow<SignInEvent> = eventChannel.receiveAsFlow()

    private var pairingJob: Job? = null

    init {
        startPairing()
    }

    fun onRefreshClick() {
        if (_uiState.value.pairing == PairingUi.Expired) startPairing()
    }

    private fun startPairing() {
        pairingJob?.cancel()
        pairingJob = viewModelScope.launch {
            _uiState.update { it.copy(pairing = PairingUi.Loading) }
            val code = awaitCode()
            _uiState.update { it.copy(pairing = PairingUi.Ready(code.code)) }

            when (withTimeoutOrNull(code.lifetime) { pollUntilSettled(code) }) {
                PairingPoll.Linked -> eventChannel.send(SignInEvent.Linked)
                else -> _uiState.update { it.copy(pairing = PairingUi.Expired) }
            }
        }
    }

    private suspend fun awaitCode(): PairingCode {
        while (true) {
            pairing.requestCode().onSuccess { return it }
            _uiState.update { it.copy(pairing = PairingUi.Offline) }
            delay(CODE_RETRY_DELAY)
        }
    }

    private suspend fun pollUntilSettled(code: PairingCode): PairingPoll {
        while (true) {
            delay(POLL_INTERVAL)
            val poll = pairing.poll(code)
            if (poll != PairingPoll.Pending) return poll
        }
    }
}
