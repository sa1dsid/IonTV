package com.ionica.iontv.feature.setup.presentation

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ionica.iontv.R
import com.ionica.iontv.core.designsystem.component.TvActionLink
import com.ionica.iontv.core.designsystem.component.TvButton
import com.ionica.iontv.core.designsystem.component.TvButtonSize
import com.ionica.iontv.core.designsystem.component.rememberFocusedInteraction
import com.ionica.iontv.core.designsystem.theme.IonTvTheme

private val ChecklistWidth = 736.dp

private val BoxSize = 30.dp

private val BoxCorner = 8.dp

private val DoneLetterSpacing = 1.3.sp

@Composable
fun SetupRoute(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SetupViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.onResume() }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                SetupEvent.Finished -> onFinished()
            }
        }
    }

    SetupScreen(
        uiState = uiState,
        onOpenSettingsClick = viewModel::onOpenSettingsClick,
        onContinueClick = viewModel::onContinueClick,
        modifier = modifier,
    )
}

@Composable
fun SetupScreen(
    uiState: SetupUiState,
    onOpenSettingsClick: (SetupStep) -> Unit,
    onContinueClick: () -> Unit,
    modifier: Modifier = Modifier,
    actionInteraction: MutableInteractionSource? = null,
) {
    val colors = IonTvTheme.colors
    val backgroundFocus = remember { FocusRequester() }
    val autostartFocus = remember { FocusRequester() }
    val continueFocus = remember { FocusRequester() }

    LaunchedEffect(uiState.backgroundActivityDone, uiState.autostartDone) {
        when {
            !uiState.backgroundActivityDone -> backgroundFocus.requestFocus()
            !uiState.autostartDone -> autostartFocus.requestFocus()
            else -> continueFocus.requestFocus()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(horizontal = 85.dp, vertical = 54.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(R.string.setup_title),
            style = IonTvTheme.typography.title,
            color = colors.textPrimary,
        )
        Spacer(Modifier.height(21.dp))

        Column(modifier = Modifier.widthIn(max = ChecklistWidth)) {
            ChecklistRow(
                label = stringResource(R.string.setup_background),
                done = uiState.backgroundActivityDone,
                onOpenSettings = { onOpenSettingsClick(SetupStep.BACKGROUND_ACTIVITY) },
                focusRequester = backgroundFocus,
                interactionSource = actionInteraction.takeUnless { uiState.backgroundActivityDone },
            )
            ChecklistRow(
                label = stringResource(R.string.setup_autostart),
                done = uiState.autostartDone,
                onOpenSettings = { onOpenSettingsClick(SetupStep.AUTOSTART) },
                focusRequester = autostartFocus,
                interactionSource = actionInteraction.takeIf { uiState.backgroundActivityDone },
            )
        }

        Spacer(Modifier.height(34.dp))
        TvButton(
            text = stringResource(R.string.setup_continue),
            onClick = onContinueClick,
            enabled = uiState.canContinue,
            size = TvButtonSize.LARGE,
            modifier = Modifier.focusRequester(continueFocus),
            interactionSource = actionInteraction.takeIf { uiState.canContinue },
        )
    }
}

@Composable
private fun ChecklistRow(
    label: String,
    done: Boolean,
    onOpenSettings: () -> Unit,
    focusRequester: FocusRequester,
    interactionSource: MutableInteractionSource?,
) {
    val colors = IonTvTheme.colors
    val typography = IonTvTheme.typography

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                val line = 1.dp.toPx()
                drawRect(
                    color = colors.divider,
                    topLeft = Offset(0f, size.height - line),
                    size = Size(size.width, line),
                )
            }
            .padding(vertical = 18.5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CheckMark(done = done)
        Spacer(Modifier.width(17.dp))
        Text(
            text = label,
            style = typography.label,
            color = colors.textPrimary,
            modifier = Modifier.weight(1f),
        )
        if (done) {
            Text(
                text = stringResource(R.string.setup_done),
                style = typography.actionLabel.copy(letterSpacing = DoneLetterSpacing),
                color = colors.success,
            )
        } else {
            TvActionLink(
                text = stringResource(R.string.setup_open_settings),
                onClick = onOpenSettings,
                modifier = Modifier.focusRequester(focusRequester),
                interactionSource = interactionSource,
            )
        }
    }
}

@Composable
private fun CheckMark(done: Boolean) {
    val colors = IonTvTheme.colors
    val shape = RoundedCornerShape(BoxCorner)

    Box(
        modifier = Modifier
            .size(BoxSize)
            .border(2.5.dp, if (done) colors.success else colors.outline, shape),
        contentAlignment = Alignment.Center,
    ) {
        if (done) {
            Canvas(modifier = Modifier.size(16.dp)) {
                val unit = size.width / 24f
                val tick = Path().apply {
                    moveTo(4f * unit, 12.5f * unit)
                    lineTo(10f * unit, 18.5f * unit)
                    lineTo(20f * unit, 6.5f * unit)
                }
                drawPath(
                    path = tick,
                    color = colors.success,
                    style = Stroke(width = 3.5f * unit, cap = StrokeCap.Round, join = StrokeJoin.Round),
                )
            }
        }
    }
}

@Preview(widthDp = 960, heightDp = 540)
@Composable
private fun SetupNothingDonePreview() {
    IonTvTheme {
        SetupScreen(
            uiState = SetupUiState(),
            onOpenSettingsClick = {},
            onContinueClick = {},
            actionInteraction = rememberFocusedInteraction(),
        )
    }
}

@Preview(widthDp = 960, heightDp = 540)
@Composable
private fun SetupFirstDonePreview() {
    IonTvTheme {
        SetupScreen(
            uiState = SetupUiState(backgroundActivityDone = true),
            onOpenSettingsClick = {},
            onContinueClick = {},
            actionInteraction = rememberFocusedInteraction(),
        )
    }
}

@Preview(widthDp = 960, heightDp = 540)
@Composable
private fun SetupAllDonePreview() {
    IonTvTheme {
        SetupScreen(
            uiState = SetupUiState(backgroundActivityDone = true, autostartDone = true),
            onOpenSettingsClick = {},
            onContinueClick = {},
            actionInteraction = rememberFocusedInteraction(),
        )
    }
}
