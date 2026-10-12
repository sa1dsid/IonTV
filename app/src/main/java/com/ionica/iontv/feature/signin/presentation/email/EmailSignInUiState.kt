package com.ionica.iontv.feature.signin.presentation.email

data class EmailSignInUiState(
    val step: EmailStep = EmailStep.EMAIL,
    val email: String = "",
    val code: String = "",
    val submitting: Boolean = false,
    val error: EmailError? = null,
    /** Сколько ещё ждать, прежде чем можно попросить новый код. */
    val resendInSeconds: Int = 0,
)

/** Сначала адрес, потом код из письма — пароля у входа IONTV нет. */
enum class EmailStep { EMAIL, CODE }

/** Ошибка показывается под полем текущего шага красной строкой и красной рамкой. */
enum class EmailError {
    EMAIL_INVALID,
    CODE_INVALID,
    RESEND_TOO_SOON,
    NO_CONNECTION,
    SERVER,
}

sealed interface EmailSignInEvent {
    data object Linked : EmailSignInEvent
    data object Back : EmailSignInEvent
}
