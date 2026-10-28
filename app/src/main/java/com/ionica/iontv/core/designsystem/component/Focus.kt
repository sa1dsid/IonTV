package com.ionica.iontv.core.designsystem.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ionica.iontv.core.designsystem.theme.IonTvTheme

private const val FOCUS_FADE_MILLIS = 120

private const val GLOW_STEPS = 12

private const val EDGE_SHARE = 0.5f

private const val PRESSED_SCALE = 0.97f

private val RingWidth = 2.5.dp

internal val FocusRingOffset = 4.dp

private val RingGlow = 17.dp

private const val RING_GLOW_ALPHA = 0.3f

private val FieldGlow = 15.dp

private const val FIELD_GLOW_ALPHA = 0.25f

@Composable
fun Modifier.focusRing(
    focused: Boolean,
    cornerRadius: Dp,
    offset: Dp = FocusRingOffset,
): Modifier {
    val color = IonTvTheme.colors.focusRing
    val visibility by animateFloatAsState(
        targetValue = if (focused) 1f else 0f,
        animationSpec = tween(FOCUS_FADE_MILLIS),
        label = "focus-ring",
    )

    return drawWithCache {
        val ringOffset = offset.toPx()
        val ring = Rect(Offset.Zero, size).inflate(ringOffset)
        val radius = cornerRadius.toPx() + ringOffset
        val stroke = RingWidth.toPx()

        onDrawWithContent {
            drawContent()
            if (visibility <= 0f) return@onDrawWithContent

            drawGlow(ring, radius, RingGlow.toPx(), color.copy(alpha = RING_GLOW_ALPHA * visibility))
            drawRoundRect(
                color = color.copy(alpha = visibility),
                topLeft = ring.topLeft,
                size = ring.size,
                cornerRadius = CornerRadius(radius),
                style = Stroke(width = stroke),
            )
        }
    }
}

@Composable
fun Modifier.focusGlow(focused: Boolean, cornerRadius: Dp): Modifier {
    val color = IonTvTheme.colors.focusRing
    val visibility by animateFloatAsState(
        targetValue = if (focused) 1f else 0f,
        animationSpec = tween(FOCUS_FADE_MILLIS),
        label = "focus-glow",
    )

    return drawWithCache {
        val edge = Rect(Offset.Zero, size)
        val radius = cornerRadius.toPx()

        onDrawBehind {
            if (visibility <= 0f) return@onDrawBehind
            drawGlow(edge, radius, FieldGlow.toPx(), color.copy(alpha = FIELD_GLOW_ALPHA * visibility))
        }
    }
}

@Composable
fun Modifier.pressScale(pressed: Boolean): Modifier {
    val scale by animateFloatAsState(
        targetValue = if (pressed) PRESSED_SCALE else 1f,
        animationSpec = tween(FOCUS_FADE_MILLIS),
        label = "press-scale",
    )
    return scale(scale)
}

private fun DrawScope.drawGlow(edge: Rect, cornerRadius: Float, reach: Float, color: Color) {
    val step = reach / GLOW_STEPS
    repeat(GLOW_STEPS) { index ->
        val distance = step * (index + 0.5f)
        val falloff = 1f - distance / reach
        val bounds = edge.inflate(distance)
        drawRoundRect(
            color = color.copy(alpha = color.alpha * EDGE_SHARE * falloff * falloff),
            topLeft = bounds.topLeft,
            size = Size(bounds.width, bounds.height),
            cornerRadius = CornerRadius(cornerRadius + distance),
            style = Stroke(width = step),
        )
    }
}
