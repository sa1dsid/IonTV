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
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ionica.iontv.core.designsystem.theme.IonTvTheme

const val POPUP_BACKDROP_ALPHA = 0.3f

private val PopupCorner = 18.dp

private val PopupWidth = 542.dp

private val MessageMaxWidth = 323.dp

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
    busy: Boolean = false,
    safeActionInteraction: MutableInteractionSource? = null,
) {
    val colors = IonTvTheme.colors
    val typography = IonTvTheme.typography
    val safeFocus = remember { FocusRequester() }
    val shape = RoundedCornerShape(PopupCorner)

    BackHandler { if (!busy) onSafeAction() }

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
                .padding(start = 37.dp, top = 37.dp, end = 37.dp, bottom = 31.5.dp)
                .onPreviewKeyEvent { event ->
                    if (event.key != Key.Back && event.key != Key.Escape) return@onPreviewKeyEvent false
                    if (event.type == KeyEventType.KeyUp && !busy) onSafeAction()
                    true
                }
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
            Spacer(Modifier.height(14.5.dp))
            Text(
                text = message,
                style = typography.popupBody,
                color = colors.textSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = MessageMaxWidth),
            )
            Spacer(Modifier.height(31.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                TvButton(
                    text = safeActionText,
                    onClick = onSafeAction,
                    modifier = Modifier.focusRequester(safeFocus),
                    busy = busy,
                    interactionSource = safeActionInteraction,
                )
                TvButton(
                    text = riskyActionText,
                    onClick = onRiskyAction,
                    busy = busy,
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
