package com.ionica.iontv.feature.signin.presentation

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ionica.iontv.R
import com.ionica.iontv.core.designsystem.component.QrCode
import com.ionica.iontv.core.designsystem.component.TvButton
import com.ionica.iontv.core.designsystem.component.TvSpinner
import com.ionica.iontv.core.designsystem.component.emphasizedString
import com.ionica.iontv.core.designsystem.component.rememberFocusedInteraction
import com.ionica.iontv.core.designsystem.theme.IonTvTheme

private val QrSize = 196.dp

private val QrCorner = 10.dp

private val PathsWidth = 369.dp

// Строка «or enter code» шире самого QR; колонка держит её ширину во всех состояниях,
// чтобы правая часть не съезжала, когда код протухает.
private val PairingWidth = 280.dp

// Под QR место для строки с кодом или кнопки Refresh: высота постоянная,
// чтобы при смене состояния колонка не прыгала.
private val BelowQrHeight = 56.dp

@Composable
fun SignInRoute(
    onLinked: () -> Unit,
    onEmailClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SignInViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    // Credential Manager показывает окно выбора аккаунта поверх активити — нужен её контекст.
    val activityContext = LocalContext.current

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                SignInEvent.Linked -> onLinked()
            }
        }
    }

    SignInScreen(
        uiState = uiState,
        onRefreshClick = viewModel::onRefreshClick,
        onGoogleClick = { viewModel.onGoogleClick(activityContext) },
        onEmailClick = onEmailClick,
        modifier = modifier,
    )
}

@Composable
fun SignInScreen(
    uiState: SignInUiState,
    onRefreshClick: () -> Unit,
    onGoogleClick: () -> Unit,
    onEmailClick: () -> Unit,
    modifier: Modifier = Modifier,
    primaryInteraction: MutableInteractionSource? = null,
) {
    val colors = IonTvTheme.colors
    val typography = IonTvTheme.typography
    val primaryFocus = remember { FocusRequester() }

    // Refresh пропадает сразу после нажатия — фокус возвращается на основную кнопку.
    val expired = uiState.pairing == PairingUi.Expired
    LaunchedEffect(expired) {
        if (!expired) primaryFocus.requestFocus()
    }

    Row(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(horizontal = 59.dp, vertical = 54.dp),
        horizontalArrangement = Arrangement.spacedBy(54.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PairingCodeBox(pairing = uiState.pairing, onRefreshClick = onRefreshClick)

        Column(modifier = Modifier.width(PathsWidth)) {
            Text(
                text = stringResource(R.string.signin_title),
                style = typography.title,
                color = colors.textPrimary,
            )
            Spacer(Modifier.height(15.dp))
            Text(
                text = emphasizedString(
                    id = R.string.signin_how,
                    emphasis = SpanStyle(color = colors.textPrimary, fontWeight = FontWeight.SemiBold),
                ),
                style = typography.body,
                color = colors.textSecondary,
            )
            Spacer(Modifier.height(25.dp))
            // Без Google Play services (Fire TV, безымянные боксы) остаётся только почта,
            // и фокус по умолчанию переходит на неё.
            val emailIsPrimary = !uiState.googleAvailable
            if (uiState.googleAvailable) {
                TvButton(
                    text = stringResource(R.string.signin_google),
                    onClick = onGoogleClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(primaryFocus),
                    interactionSource = primaryInteraction,
                )
                Text(
                    text = stringResource(R.string.signin_or),
                    style = typography.separator,
                    color = colors.textTertiary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 15.dp),
                )
            }
            TvButton(
                text = stringResource(R.string.signin_email),
                onClick = onEmailClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (emailIsPrimary) Modifier.focusRequester(primaryFocus) else Modifier),
                interactionSource = primaryInteraction.takeIf { emailIsPrimary },
            )
            uiState.error?.let { error ->
                Spacer(Modifier.height(15.dp))
                Text(
                    text = stringResource(error.message()),
                    style = typography.statusReason,
                    color = colors.danger,
                )
            }
        }
    }
}

@StringRes
private fun SignInError.message(): Int = when (this) {
    SignInError.NO_CONNECTION -> R.string.error_no_connection
    SignInError.SERVER -> R.string.error_server
    SignInError.GOOGLE_UNAVAILABLE -> R.string.error_google_unavailable
}

@Composable
private fun PairingCodeBox(pairing: PairingUi, onRefreshClick: () -> Unit) {
    Column(
        modifier = Modifier.width(PairingWidth),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        when (pairing) {
            is PairingUi.Ready -> QrCode(content = pairing.code, size = QrSize, cornerRadius = QrCorner)
            PairingUi.Expired -> QrPlaceholder {
                Text(
                    text = stringResource(R.string.signin_code_expired),
                    style = IonTvTheme.typography.body,
                    color = IonTvTheme.colors.textSecondary,
                )
            }

            PairingUi.Loading, PairingUi.Offline -> QrPlaceholder { TvSpinner() }
        }

        Spacer(Modifier.height(15.dp))

        Box(modifier = Modifier.height(BelowQrHeight), contentAlignment = Alignment.TopCenter) {
            when (pairing) {
                is PairingUi.Ready -> PairingCodeLine(pairing.code)
                PairingUi.Offline -> Text(
                    text = stringResource(R.string.no_connection_retrying),
                    style = IonTvTheme.typography.notice,
                    color = IonTvTheme.colors.textSecondary,
                )

                PairingUi.Expired -> RefreshButton(onRefreshClick)
                PairingUi.Loading -> Unit
            }
        }
    }
}

@Composable
private fun QrPlaceholder(content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(QrCorner)
    Box(
        modifier = Modifier
            .size(QrSize)
            .background(IonTvTheme.colors.fieldSurface, shape)
            .border(1.dp, IonTvTheme.colors.outline, shape),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@Composable
private fun PairingCodeLine(code: String) {
    val colors = IonTvTheme.colors
    val label = stringResource(R.string.signin_enter_code)

    Text(
        text = buildAnnotatedString {
            append(label)
            append("  ")
            withStyle(SpanStyle(color = colors.textPrimary, fontWeight = FontWeight.Medium)) {
                append(code)
            }
        },
        style = IonTvTheme.typography.pairingCode,
        color = colors.textSecondary,
    )
}

/** Когда код протух, QR бесполезен: фокус сразу на Refresh. */
@Composable
private fun RefreshButton(onClick: () -> Unit) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }

    TvButton(
        text = stringResource(R.string.signin_refresh),
        onClick = onClick,
        modifier = Modifier.focusRequester(focus),
    )
}

@Preview(widthDp = 960, heightDp = 540)
@Composable
private fun SignInScreenPreview() {
    IonTvTheme {
        SignInScreen(
            uiState = SignInUiState(pairing = PairingUi.Ready("7K2M4Q"), googleAvailable = true),
            onRefreshClick = {},
            onGoogleClick = {},
            onEmailClick = {},
            primaryInteraction = rememberFocusedInteraction(),
        )
    }
}

@Preview(widthDp = 960, heightDp = 540)
@Composable
private fun SignInScreenExpiredPreview() {
    IonTvTheme {
        SignInScreen(
            uiState = SignInUiState(pairing = PairingUi.Expired, googleAvailable = true),
            onRefreshClick = {},
            onGoogleClick = {},
            onEmailClick = {},
        )
    }
}

@Preview(widthDp = 960, heightDp = 540)
@Composable
private fun SignInScreenOfflinePreview() {
    IonTvTheme {
        SignInScreen(
            uiState = SignInUiState(pairing = PairingUi.Offline),
            onRefreshClick = {},
            onGoogleClick = {},
            onEmailClick = {},
        )
    }
}

@Preview(widthDp = 960, heightDp = 540)
@Composable
private fun SignInScreenGoogleFailedPreview() {
    IonTvTheme {
        SignInScreen(
            uiState = SignInUiState(
                pairing = PairingUi.Ready("7K2M4Q"),
                googleAvailable = true,
                error = SignInError.SERVER,
            ),
            onRefreshClick = {},
            onGoogleClick = {},
            onEmailClick = {},
        )
    }
}
