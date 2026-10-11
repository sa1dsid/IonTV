package com.ionica.iontv.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ionica.iontv.core.designsystem.theme.IonTvTheme

private const val DISABLED_ALPHA = 0.4f

private val ButtonCorner = 10.dp

private val ActionCorner = 7.dp

enum class TvButtonTone {
    /** Основное действие: светлая подпись. */
    NORMAL,

    /** Второстепенное или разрушительное действие: приглушённая подпись. */
    MUTED,
}

/**
 * Кнопка под D-pad. Выключенная кнопка не берёт фокус: пульт её просто перескакивает,
 * как заблокированный Continue в чеклисте.
 */
@Composable
fun TvButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tone: TvButtonTone = TvButtonTone.NORMAL,
    interactionSource: MutableInteractionSource? = null,
) {
    val colors = IonTvTheme.colors
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    val pressed by source.collectIsPressedAsState()
    val shape = RoundedCornerShape(ButtonCorner)

    Box(
        modifier = modifier
            .focusRing(focused = focused && enabled, cornerRadius = ButtonCorner)
            .pressScale(pressed)
            .alpha(if (enabled) 1f else DISABLED_ALPHA)
            .clip(shape)
            .background(colors.buttonSurface)
            .border(1.dp, colors.outline, shape)
            .clickable(
                interactionSource = source,
                indication = null,
                enabled = enabled,
                onClick = onClick,
            )
            .padding(horizontal = 32.dp, vertical = 15.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = IonTvTheme.typography.button,
            color = when (tone) {
                TvButtonTone.NORMAL -> colors.buttonText
                TvButtonTone.MUTED -> colors.textSecondary
            },
            textAlign = TextAlign.Center,
        )
    }
}

/** Моноширинная ссылка-действие внутри строки, вроде «OPEN SETTINGS →» в чеклисте. */
@Composable
fun TvActionLink(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    interactionSource: MutableInteractionSource? = null,
) {
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()

    Text(
        text = text,
        style = IonTvTheme.typography.actionLabel,
        color = IonTvTheme.colors.link,
        modifier = modifier
            .focusRing(focused = focused, cornerRadius = ActionCorner, offset = 2.dp)
            .clip(RoundedCornerShape(ActionCorner))
            .clickable(interactionSource = source, indication = null, onClick = onClick)
            .padding(horizontal = 11.dp, vertical = 5.dp),
    )
}

/**
 * Для превью и скриншотов: ставит элементу фокус, не трогая настоящий фокус окна.
 * Событие уходит кадром позже: раньше элемент ещё не подписан на источник и его теряет.
 */
@Composable
internal fun rememberFocusedInteraction(): MutableInteractionSource {
    val source = remember { MutableInteractionSource() }
    LaunchedEffect(source) {
        withFrameNanos { }
        source.emit(FocusInteraction.Focus())
    }
    return source
}

@Preview(backgroundColor = 0xFF000000, showBackground = true)
@Composable
private fun TvButtonsPreview() {
    IonTvTheme {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(17.dp)) {
                TvButton(
                    text = "Agree & continue",
                    onClick = {},
                    interactionSource = rememberFocusedInteraction(),
                )
                TvButton(text = "Decline", onClick = {}, tone = TvButtonTone.MUTED)
                TvButton(text = "Continue", onClick = {}, enabled = false)
            }
            TvActionLink(
                text = "OPEN SETTINGS →",
                onClick = {},
                interactionSource = rememberFocusedInteraction(),
            )
        }
    }
}
