package com.ionica.iontv.core.designsystem.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ionica.iontv.core.designsystem.theme.IonTvTheme

private const val TURN_MILLIS = 900

private const val ARC_START_DEGREES = -135f

private const val ARC_SWEEP_DEGREES = 90f

@Composable
fun TvSpinner(
    modifier: Modifier = Modifier,
    size: Dp = 32.dp,
    strokeWidth: Dp = 4.dp,
) {
    val colors = IonTvTheme.colors
    val rotation by rememberInfiniteTransition(label = "spinner").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(TURN_MILLIS, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "spinner-rotation",
    )

    Canvas(modifier = modifier.size(size)) {
        val stroke = strokeWidth.toPx()
        val ringSize = Size(this.size.width - stroke, this.size.height - stroke)
        val ringTopLeft = Offset(stroke / 2f, stroke / 2f)

        drawArc(
            color = colors.spinnerTrack,
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = ringTopLeft,
            size = ringSize,
            style = Stroke(width = stroke),
        )
        rotate(rotation) {
            drawArc(
                color = colors.link,
                startAngle = ARC_START_DEGREES,
                sweepAngle = ARC_SWEEP_DEGREES,
                useCenter = false,
                topLeft = ringTopLeft,
                size = ringSize,
                style = Stroke(width = stroke),
            )
        }
    }
}

@Preview(backgroundColor = 0xFF000000, showBackground = true)
@Composable
private fun TvSpinnerPreview() {
    IonTvTheme {
        TvSpinner()
    }
}
