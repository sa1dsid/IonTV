package com.ionica.iontv.feature.consent.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ionica.iontv.R
import com.ionica.iontv.core.designsystem.component.IonTvLogo
import com.ionica.iontv.core.designsystem.component.POPUP_BACKDROP_ALPHA
import com.ionica.iontv.core.designsystem.component.QrCode
import com.ionica.iontv.core.designsystem.component.TvButton
import com.ionica.iontv.core.designsystem.component.TvPopup
import com.ionica.iontv.core.designsystem.component.emphasizedString
import com.ionica.iontv.core.designsystem.component.rememberFocusedInteraction
import com.ionica.iontv.core.designsystem.theme.IonTvTheme

private val TextMaxWidth = 620.dp

private val LegalQrSize = 60.dp

private val ScanHintMaxWidth = 152.dp

@Composable
fun ConsentRoute(
    onAgreed: (setupPending: Boolean) -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ConsentViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is ConsentEvent.Agreed -> onAgreed(event.setupPending)
                ConsentEvent.ExitApp -> onExit()
            }
        }
    }

    ConsentScreen(
        uiState = uiState,
        onAgreeClick = viewModel::onAgreeClick,
        onDeclineClick = viewModel::onDeclineClick,
        onGoBackClick = viewModel::onGoBackClick,
        onExitClick = viewModel::onExitClick,
        modifier = modifier,
    )
}

@Composable
fun ConsentScreen(
    uiState: ConsentUiState,
    onAgreeClick: () -> Unit,
    onDeclineClick: () -> Unit,
    onGoBackClick: () -> Unit,
    onExitClick: () -> Unit,
    modifier: Modifier = Modifier,
    agreeInteraction: MutableInteractionSource? = null,
    popupInteraction: MutableInteractionSource? = null,
) {
    val colors = IonTvTheme.colors
    val typography = IonTvTheme.typography
    val agreeFocus = remember { FocusRequester() }

    BackHandler(enabled = !uiState.declining, onBack = onDeclineClick)

    LaunchedEffect(uiState.declining) {
        if (!uiState.declining) agreeFocus.requestFocus()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .alpha(if (uiState.declining) POPUP_BACKDROP_ALPHA else 1f),
        ) {
            IonTvLogo(
                fontSize = 15.sp,
                modifier = Modifier.padding(start = 37.dp, top = 30.dp),
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 37.dp, vertical = 54.dp),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = stringResource(R.string.consent_title),
                    style = typography.consentTitle,
                    color = colors.textPrimary,
                )
                Spacer(Modifier.height(15.dp))
                Text(
                    text = stringResource(R.string.consent_text),
                    style = typography.consentBody,
                    color = colors.textBody,
                    modifier = Modifier.widthIn(max = TextMaxWidth),
                )
                Spacer(Modifier.height(34.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(18.5.dp)) {
                    TvButton(
                        text = stringResource(R.string.consent_agree),
                        onClick = onAgreeClick,
                        busy = uiState.submitting,
                        modifier = Modifier.focusRequester(agreeFocus),
                        interactionSource = agreeInteraction,
                    )
                    TvButton(
                        text = stringResource(R.string.consent_decline),
                        onClick = onDeclineClick,
                    )
                }
                uiState.error?.let { error ->
                    Spacer(Modifier.height(15.dp))
                    Text(
                        text = stringResource(
                            when (error) {
                                ConsentError.NO_CONNECTION -> R.string.error_no_connection
                                ConsentError.SERVER -> R.string.error_server
                            }
                        ),
                        style = typography.statusReason,
                        color = colors.danger,
                    )
                }
                Spacer(Modifier.height(25.5.dp))
                Text(
                    text = emphasizedString(
                        R.string.consent_legal,
                        SpanStyle(color = colors.textPrimary, fontWeight = FontWeight.SemiBold),
                    ),
                    style = typography.legal,
                    color = colors.textBody,
                )
                Spacer(Modifier.height(28.5.dp))
                LegalLinks()
            }
        }

        if (uiState.declining) {
            TvPopup(
                title = stringResource(R.string.consent_decline_title),
                message = stringResource(R.string.consent_decline_text),
                safeActionText = stringResource(R.string.consent_go_back),
                onSafeAction = onGoBackClick,
                riskyActionText = stringResource(R.string.consent_exit),
                onRiskyAction = onExitClick,
                safeActionInteraction = popupInteraction,
            )
        }
    }
}

@Composable
private fun LegalLinks() {
    Row(
        horizontalArrangement = Arrangement.spacedBy(25.5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LegalLink(label = stringResource(R.string.consent_terms), url = stringResource(R.string.terms_url))
        LegalLink(label = stringResource(R.string.consent_privacy), url = stringResource(R.string.privacy_url))
        Text(
            text = stringResource(R.string.consent_legal_scan),
            style = IonTvTheme.typography.legal,
            color = IonTvTheme.colors.textSecondary,
            modifier = Modifier.widthIn(max = ScanHintMaxWidth),
        )
    }
}

@Composable
private fun LegalLink(label: String, url: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        QrCode(content = url, size = LegalQrSize, cornerRadius = 0.dp)
        Text(text = label, style = IonTvTheme.typography.legalLabel, color = IonTvTheme.colors.textPrimary)
    }
}

@Preview(widthDp = 960, heightDp = 540)
@Composable
private fun ConsentScreenPreview() {
    IonTvTheme {
        ConsentScreen(
            uiState = ConsentUiState(),
            onAgreeClick = {},
            onDeclineClick = {},
            onGoBackClick = {},
            onExitClick = {},
            agreeInteraction = rememberFocusedInteraction(),
        )
    }
}

@Preview(widthDp = 960, heightDp = 540)
@Composable
private fun ConsentDeclinedPreview() {
    IonTvTheme {
        ConsentScreen(
            uiState = ConsentUiState(declining = true),
            onAgreeClick = {},
            onDeclineClick = {},
            onGoBackClick = {},
            onExitClick = {},
            popupInteraction = rememberFocusedInteraction(),
        )
    }
}
