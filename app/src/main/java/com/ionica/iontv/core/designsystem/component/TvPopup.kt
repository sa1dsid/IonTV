package com.ionica.iontv.core.designsystem.component

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ionica.iontv.core.designsystem.theme.IonTvTheme

/** Прозрачность экрана под попапом, как в кадрах 3b и 3c. */
const val POPUP_BACKDROP_ALPHA = 0.3f

private val PopupCorner = 17.dp

private val PopupWidth = 540.dp

/**
 * Попап подтверждения. По ТЗ их в приложении два, и оба — на разрушительных действиях,
 * поэтому фокус встаёт на безопасную кнопку, а «Назад» на пульте означает её же.
 * Пульт не может увести фокус из попапа на экран под ним.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun TvPopup(
    title: String,
    message: String,
    safeActionText: String,
    onSafeAction: () -> Unit,
    riskyActionText: String,
    onRiskyAction: () -> Unit,
    modifier: Modifier = Modifier,
    safeActionInteraction: MutableInteractionSource? = null,
) {
    val colors = IonTvTheme.colors
    val typography = IonTvTheme.typography
    val safeFocus = remember { FocusRequester() }
    val shape = RoundedCornerShape(PopupCorner)

    BackHandler(onBack = onSafeAction)

    LaunchedEffect(Unit) { safeFocus.requestFocus() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.scrim),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .width(PopupWidth)
                .background(colors.fieldSurface, shape)
                .border(1.dp, colors.outline, shape)
                .padding(start = 34.dp, top = 34.dp, end = 34.dp, bottom = 30.dp)
                .focusProperties { exit = { FocusRequester.Cancel } }
                .focusGroup(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = title,
                style = typography.popupTitle,
                color = colors.textPrimary,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(15.dp))
            Text(
                text = message,
                style = typography.popupBody,
                color = colors.textSecondary,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(27.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(15.dp)) {
                TvButton(
                    text = safeActionText,
                    onClick = onSafeAction,
                    modifier = Modifier.focusRequester(safeFocus),
                    interactionSource = safeActionInteraction,
                )
                TvButton(
                    text = riskyActionText,
                    onClick = onRiskyAction,
                    tone = TvButtonTone.MUTED,
                )
            }
        }
    }
}

@Preview(widthDp = 960, heightDp = 540)
@Composable
private fun TvPopupPreview() {
    IonTvTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(IonTvTheme.colors.background),
        ) {
            TvPopup(
                title = "Unlink this TV?",
                message = "Sharing stops immediately. The TV returns to the sign-in screen.",
                safeActionText = "Keep linked",
                onSafeAction = {},
                riskyActionText = "Unlink",
                onRiskyAction = {},
                safeActionInteraction = rememberFocusedInteraction(),
            )
        }
    }
}
