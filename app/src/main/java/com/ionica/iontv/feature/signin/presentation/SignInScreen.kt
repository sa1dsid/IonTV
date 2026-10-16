package com.ionica.iontv.feature.signin.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ionica.iontv.R
import com.ionica.iontv.core.designsystem.component.QrCode
import com.ionica.iontv.core.designsystem.component.TvSpinner
import com.ionica.iontv.core.designsystem.component.emphasizedString
import com.ionica.iontv.core.designsystem.theme.IonTvTheme

// По ТЗ QR большой: в нём теперь ссылка с токеном, модулей много, и мелкий QR камера
// телефона с дивана не возьмёт.
private val QrSize = 260.dp

private val QrCorner = 10.dp

private val PathsWidth = 369.dp

// Строка «or enter code» шире самого QR, а код бывает до восьми символов: колонка держит
// ширину во всех состояниях, чтобы правая часть не съезжала, пока код обновляется.
private val PairingWidth = 312.dp

// Под QR место для строки с кодом: высота постоянная, чтобы колонка не прыгала.
private val BelowQrHeight = 56.dp

@Composable
fun SignInRoute(
    onLinked: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SignInViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                SignInEvent.Linked -> onLinked()
            }
        }
    }

    SignInScreen(uiState = uiState, modifier = modifier)
}

/**
 * Кадр 2: входа на ТВ нет. Человек сканирует QR на сайте IonTV Pulse или камерой телефона,
 * либо вводит код вручную. Нажимать на телевизоре нечего: QR сам обновляется, когда истекает.
 */
@Composable
fun SignInScreen(
    uiState: SignInUiState,
    modifier: Modifier = Modifier,
) {
    val colors = IonTvTheme.colors
    val typography = IonTvTheme.typography

    Row(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(horizontal = 59.dp, vertical = 54.dp),
        horizontalArrangement = Arrangement.spacedBy(54.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PairingCodeBox(pairing = uiState.pairing)

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
        }
    }
}

@Composable
private fun PairingCodeBox(pairing: PairingUi) {
    Column(
        modifier = Modifier.width(PairingWidth),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        when (pairing) {
            is PairingUi.Ready -> QrCode(content = pairing.qrContent, size = QrSize, cornerRadius = QrCorner)
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
        maxLines = 1,
    )
}

private const val PREVIEW_QR_URL = "https://iontvcheck.ionika.cc/link#token=example-qr-token-01234567890123456789"

@Preview(widthDp = 960, heightDp = 540)
@Composable
private fun SignInScreenPreview() {
    IonTvTheme {
        SignInScreen(uiState = SignInUiState(pairing = PairingUi.Ready(code = "7K2M4Q", qrContent = PREVIEW_QR_URL)))
    }
}

/** По ТЗ код бывает до восьми символов — строка под QR не должна переноситься. */
@Preview(widthDp = 960, heightDp = 540)
@Composable
private fun SignInScreenLongCodePreview() {
    IonTvTheme {
        SignInScreen(uiState = SignInUiState(pairing = PairingUi.Ready(code = "7K2M4QXW", qrContent = PREVIEW_QR_URL)))
    }
}

@Preview(widthDp = 960, heightDp = 540)
@Composable
private fun SignInScreenOfflinePreview() {
    IonTvTheme {
        SignInScreen(uiState = SignInUiState(pairing = PairingUi.Offline))
    }
}
