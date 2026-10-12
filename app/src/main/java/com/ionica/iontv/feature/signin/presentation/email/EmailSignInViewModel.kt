package com.ionica.iontv.feature.signin.presentation.email

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ionica.iontv.feature.signin.data.AccountLinkRepository
import com.ionica.iontv.feature.signin.domain.model.LinkException
import com.ionica.iontv.feature.signin.domain.model.LinkFailure
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
import javax.inject.Inject

private const val CODE_LENGTH = 6

private const val SECOND_MILLIS = 1_000L

// Без android.util.Patterns: он недоступен в юнит-тестах, а проверка тут только
// от опечаток — настоящую делает бэкенд и отвечает 422.
private val EMAIL_PATTERN = Regex("""^[^@\s]+@[^@\s]+\.[^@\s]+$""")

@HiltViewModel
class EmailSignInViewModel @Inject constructor(
    private val accountLink: AccountLinkRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(EmailSignInUiState())
    val uiState: StateFlow<EmailSignInUiState> = _uiState.asStateFlow()

    private val eventChannel = Channel<EmailSignInEvent>(Channel.BUFFERED)
    val events: Flow<EmailSignInEvent> = eventChannel.receiveAsFlow()

    private var countdown: Job? = null

    fun onEmailChange(email: String) {
        _uiState.update { it.copy(email = email, error = null) }
    }

    fun onCodeChange(code: String) {
        val digits = code.filter(Char::isDigit).take(CODE_LENGTH)
        _uiState.update { it.copy(code = digits, error = null) }
    }

    fun onSendCodeClick() {
        val state = _uiState.value
        if (state.submitting) return

        val email = state.email.trim()
        if (!EMAIL_PATTERN.matches(email)) {
            _uiState.update { it.copy(error = EmailError.EMAIL_INVALID) }
            return
        }
        requestCode(email)
    }

    fun onResendClick() {
        val state = _uiState.value
        if (state.submitting || state.resendInSeconds > 0) return
        requestCode(state.email.trim())
    }

    fun onSignInClick() {
        val state = _uiState.value
        if (state.submitting) return
        if (state.code.length != CODE_LENGTH) {
            _uiState.update { it.copy(error = EmailError.CODE_INVALID) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(submitting = true, error = null) }
            accountLink.linkWithEmailCode(state.email.trim(), state.code)
                .onSuccess { eventChannel.send(EmailSignInEvent.Linked) }
                .onFailure { error ->
                    _uiState.update { it.copy(submitting = false, error = error.toEmailError()) }
                }
        }
    }

    /** «Назад» с шага кода возвращает к адресу — вдруг в нём опечатка. */
    fun onBack() {
        if (_uiState.value.step == EmailStep.CODE) {
            _uiState.update { it.copy(step = EmailStep.EMAIL, code = "", error = null) }
        } else {
            eventChannel.trySend(EmailSignInEvent.Back)
        }
    }

    private fun requestCode(email: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(submitting = true, error = null) }
            accountLink.requestEmailCode(email)
                .onSuccess { resendAfter ->
                    _uiState.update { it.copy(step = EmailStep.CODE, submitting = false) }
                    startCountdown(resendAfter)
                }
                .onFailure { error ->
                    _uiState.update { it.copy(submitting = false, error = error.toEmailError()) }
                }
        }
    }

    private fun startCountdown(seconds: Int) {
        countdown?.cancel()
        countdown = viewModelScope.launch {
            for (left in seconds downTo 1) {
                _uiState.update { it.copy(resendInSeconds = left) }
                delay(SECOND_MILLIS)
            }
            _uiState.update { it.copy(resendInSeconds = 0) }
        }
    }
}

private fun Throwable.toEmailError(): EmailError = when ((this as? LinkException)?.failure) {
    LinkFailure.EMAIL_INVALID -> EmailError.EMAIL_INVALID
    LinkFailure.CODE_INVALID -> EmailError.CODE_INVALID
    LinkFailure.RESEND_TOO_SOON -> EmailError.RESEND_TOO_SOON
    LinkFailure.NO_CONNECTION, null -> EmailError.NO_CONNECTION
    else -> EmailError.SERVER
}
