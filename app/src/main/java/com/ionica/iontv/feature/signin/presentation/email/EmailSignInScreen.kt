package com.ionica.iontv.feature.signin.presentation.email

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ionica.iontv.R
import com.ionica.iontv.core.designsystem.component.TvButton
import com.ionica.iontv.core.designsystem.component.TvButtonTone
import com.ionica.iontv.core.designsystem.component.TvTextField
import com.ionica.iontv.core.designsystem.component.rememberFocusedInteraction
import com.ionica.iontv.core.designsystem.theme.IonTvTheme

@Composable
fun EmailSignInRoute(
    onLinked: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EmailSignInViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                EmailSignInEvent.Linked -> onLinked()
                EmailSignInEvent.Back -> onBack()
            }
        }
    }

    BackHandler(onBack = viewModel::onBack)

    EmailSignInScreen(
        uiState = uiState,
        onEmailChange = viewModel::onEmailChange,
        onCodeChange = viewModel::onCodeChange,
        onSendCodeClick = viewModel::onSendCodeClick,
        onSignInClick = viewModel::onSignInClick,
        onResendClick = viewModel::onResendClick,
        modifier = modifier,
    )
}

/**
 * Кадр 2b. Клавиатура системная: поле в фокусе сразу её открывает, а кнопка
 * «готово» на клавиатуре отправляет шаг, чтобы не ходить пультом до кнопки.
 */
@Composable
fun EmailSignInScreen(
    uiState: EmailSignInUiState,
    onEmailChange: (String) -> Unit,
    onCodeChange: (String) -> Unit,
    onSendCodeClick: () -> Unit,
    onSignInClick: () -> Unit,
    onResendClick: () -> Unit,
    modifier: Modifier = Modifier,
    fieldInteraction: MutableInteractionSource? = null,
) {
    val colors = IonTvTheme.colors
    val emailFocus = remember { FocusRequester() }
    val codeFocus = remember { FocusRequester() }
    val onCodeStep = uiState.step == EmailStep.CODE
    val error = uiState.error?.let { stringResource(it.message()) }

    LaunchedEffect(uiState.step) {
        if (onCodeStep) codeFocus.requestFocus() else emailFocus.requestFocus()
    }

    // После «Resend» кнопку сразу выключает отсчёт, и фокус ушёл бы в никуда —
    // возвращаем его в поле, куда сейчас вводить новый код.
    val resendLocked = uiState.resendInSeconds > 0
    LaunchedEffect(resendLocked) {
        if (onCodeStep && resendLocked) codeFocus.requestFocus()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(horizontal = 79.dp, vertical = 54.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(R.string.email_title),
            style = IonTvTheme.typography.title,
            color = colors.textPrimary,
        )
        Spacer(Modifier.height(20.dp))

        // На шаге кода адрес остаётся на виду, но фокус на него не встаёт:
        // исправить опечатку — «Назад» на пульте.
        TvTextField(
            value = uiState.email,
            onValueChange = onEmailChange,
            label = stringResource(R.string.email_label),
            placeholder = stringResource(R.string.email_placeholder),
            error = error.takeUnless { onCodeStep },
            enabled = !onCodeStep,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { onSendCodeClick() }),
            focusRequester = emailFocus,
            interactionSource = fieldInteraction.takeUnless { onCodeStep },
        )

        if (onCodeStep) {
            Spacer(Modifier.height(17.dp))
            TvTextField(
                value = uiState.code,
                onValueChange = onCodeChange,
                label = stringResource(R.string.email_code_label),
                placeholder = stringResource(R.string.email_code_placeholder),
                error = error,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onSignInClick() }),
                focusRequester = codeFocus,
                interactionSource = fieldInteraction,
            )
        }

        Spacer(Modifier.height(32.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(17.dp)) {
            if (onCodeStep) {
                TvButton(
                    text = stringResource(R.string.email_sign_in),
                    onClick = onSignInClick,
                    busy = uiState.submitting,
                )
                TvButton(
                    text = if (uiState.resendInSeconds > 0) {
                        stringResource(R.string.email_resend_in, uiState.resendInSeconds)
                    } else {
                        stringResource(R.string.email_resend)
                    },
                    onClick = onResendClick,
                    enabled = uiState.resendInSeconds == 0,
                    busy = uiState.submitting,
                    tone = TvButtonTone.MUTED,
                )
            } else {
                TvButton(
                    text = stringResource(R.string.email_send_code),
                    onClick = onSendCodeClick,
                    busy = uiState.submitting,
                )
            }
        }
    }
}

@StringRes
private fun EmailError.message(): Int = when (this) {
    EmailError.EMAIL_INVALID -> R.string.email_error_invalid
    EmailError.CODE_INVALID -> R.string.email_error_code
    EmailError.RESEND_TOO_SOON -> R.string.email_error_too_soon
    EmailError.NO_CONNECTION -> R.string.error_no_connection
    EmailError.SERVER -> R.string.error_server
}

@Preview(widthDp = 960, heightDp = 540)
@Composable
private fun EmailSignInScreenPreview() {
    IonTvTheme {
        EmailSignInScreen(
            uiState = EmailSignInUiState(),
            onEmailChange = {},
            onCodeChange = {},
            onSendCodeClick = {},
            onSignInClick = {},
            onResendClick = {},
            fieldInteraction = rememberFocusedInteraction(),
        )
    }
}

@Preview(widthDp = 960, heightDp = 540)
@Composable
private fun EmailSignInCodeStepPreview() {
    IonTvTheme {
        EmailSignInScreen(
            uiState = EmailSignInUiState(
                step = EmailStep.CODE,
                email = "syrex@example.com",
                code = "4071",
                resendInSeconds = 42,
            ),
            onEmailChange = {},
            onCodeChange = {},
            onSendCodeClick = {},
            onSignInClick = {},
            onResendClick = {},
            fieldInteraction = rememberFocusedInteraction(),
        )
    }
}

@Preview(widthDp = 960, heightDp = 540)
@Composable
private fun EmailSignInWrongCodePreview() {
    IonTvTheme {
        EmailSignInScreen(
            uiState = EmailSignInUiState(
                step = EmailStep.CODE,
                email = "syrex@example.com",
                code = "407131",
                error = EmailError.CODE_INVALID,
            ),
            onEmailChange = {},
            onCodeChange = {},
            onSendCodeClick = {},
            onSignInClick = {},
            onResendClick = {},
        )
    }
}
