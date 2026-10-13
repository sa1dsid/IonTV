package com.ionica.iontv.feature.dashboard.presentation

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.ionica.iontv.R
import com.ionica.iontv.core.designsystem.theme.IonTvTheme
import com.ionica.iontv.feature.sharing.domain.model.SharingStatus
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

internal const val AMBIENT_TAG = "ambient"

private const val DOT_ALPHA = 0.4f

// Строка плывёт по двум осям с разными периодами и не возвращается в ту же точку
// подолгу — ни один пиксель OLED не горит на месте часами.
private const val DRIFT_X_MILLIS = 47_000

private const val DRIFT_Y_MILLIS = 31_000

private val FULL_TURN = (2 * PI).toFloat()

private val DriftX = 280.dp

private val DriftY = 180.dp

/** Кадр 6: почти чёрный экран и одна тусклая строка статуса. */
@Composable
fun AmbientScreen(
    status: SharingStatus,
    modifier: Modifier = Modifier,
) {
    val colors = IonTvTheme.colors
    val dotColor = if (status == SharingStatus.Active) colors.success else colors.danger
    val headline = stringResource(status.ambientHeadline())

    // Фазы по кругу: строка стартует из центра и плавно уходит по синусу.
    val drift = rememberInfiniteTransition(label = "ambient-drift")
    val phaseX by drift.animateFloat(
        initialValue = 0f,
        targetValue = FULL_TURN,
        animationSpec = infiniteRepeatable(tween(DRIFT_X_MILLIS, easing = LinearEasing), RepeatMode.Restart),
        label = "ambient-x",
    )
    val phaseY by drift.animateFloat(
        initialValue = 0f,
        targetValue = FULL_TURN,
        animationSpec = infiniteRepeatable(tween(DRIFT_Y_MILLIS, easing = LinearEasing), RepeatMode.Restart),
        label = "ambient-y",
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .testTag(AMBIENT_TAG),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(color = dotColor.copy(alpha = DOT_ALPHA))) { append("●") }
                append("  ")
                append(headline)
            },
            style = IonTvTheme.typography.ambient,
            color = colors.ambientText,
            modifier = Modifier.offset {
                IntOffset((sin(phaseX) * DriftX.toPx()).roundToInt(), (sin(phaseY) * DriftY.toPx()).roundToInt())
            },
        )
    }
}

private fun SharingStatus.ambientHeadline(): Int = when (this) {
    SharingStatus.Active -> R.string.dashboard_active
    is SharingStatus.Paused -> R.string.dashboard_paused
    SharingStatus.Offline -> R.string.dashboard_offline
}

@Preview(widthDp = 960, heightDp = 540)
@Composable
private fun AmbientScreenPreview() {
    IonTvTheme {
        AmbientScreen(status = SharingStatus.Active)
    }
}
