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
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ionica.iontv.R
import com.ionica.iontv.core.designsystem.component.TvActionLink
import com.ionica.iontv.core.designsystem.component.TvButton
import com.ionica.iontv.core.designsystem.component.rememberFocusedInteraction
import com.ionica.iontv.core.designsystem.theme.IonTvTheme

private val ChecklistWidth = 689.dp

private val BoxSize = 28.dp

private val BoxCorner = 7.dp

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

    // Выполненный шаг прячет свою кнопку — фокус уходит на следующее несделанное.
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
            .padding(horizontal = 79.dp, vertical = 54.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(R.string.setup_title),
            style = IonTvTheme.typography.title,
            color = colors.textPrimary,
        )
        Spacer(Modifier.height(20.dp))

        Column(modifier = Modifier.widthIn(max = ChecklistWidth)) {
            ChecklistRow(
                label = stringResource(R.string.setup_background),
                done = uiState.backgroundActivityDone,
                onOpenSettings = { onOpenSettingsClick(SetupStep.BACKGROUND_ACTIVITY) },
                focusRequester = backgroundFocus,
                interactionSource = actionInteraction.takeUnless { uiState.backgroundActivityDone },
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(colors.divider),
            )
            ChecklistRow(
                label = stringResource(R.string.setup_autostart),
                done = uiState.autostartDone,
                onOpenSettings = { onOpenSettingsClick(SetupStep.AUTOSTART) },
                focusRequester = autostartFocus,
                interactionSource = actionInteraction.takeIf { uiState.backgroundActivityDone },
            )
        }

        Spacer(Modifier.height(32.dp))
        TvButton(
            text = stringResource(R.string.setup_continue),
            onClick = onContinueClick,
            enabled = uiState.canContinue,
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
            .padding(vertical = 17.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CheckMark(done = done)
        Spacer(Modifier.width(16.dp))
        Text(
            text = label,
            style = typography.label,
            color = colors.textPrimary,
            modifier = Modifier.weight(1f),
        )
        if (done) {
            Text(
                text = stringResource(R.string.setup_done),
                style = typography.actionLabel,
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

/** Галка только показывает состояние: тыкать в неё нельзя, её ставит проверка системы. */
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
            Canvas(modifier = Modifier.size(14.dp)) {
                // Галка из макета: путь «M4 12.5 10 18.5 20 6.5» в поле 24×24.
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
