package com.ionica.iontv.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ionica.iontv.core.designsystem.theme.IonTvTheme

private val FieldCorner = 10.dp

private val FieldMaxWidth = 492.dp

/**
 * Поле ввода из макета: подпись капсом, тёмная плашка, при фокусе — светлая рамка с ореолом.
 * Ошибка по ТЗ показывается на месте: красная рамка и строка под полем.
 * Клавиатура — системная Android TV, своей не рисуем.
 */
@Composable
fun TvTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    error: String? = null,
    enabled: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    interactionSource: MutableInteractionSource? = null,
) {
    val colors = IonTvTheme.colors
    val typography = IonTvTheme.typography
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    val shape = RoundedCornerShape(FieldCorner)

    val borderColor = when {
        error != null -> colors.danger
        focused -> colors.focusRing
        else -> colors.outline
    }

    Column(modifier = modifier.widthIn(max = FieldMaxWidth)) {
        Text(text = label, style = typography.fieldLabel, color = colors.textTertiary)
        Spacer(Modifier.height(9.dp))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            singleLine = true,
            textStyle = typography.fieldText.copy(color = colors.textEmphasis),
            cursorBrush = SolidColor(colors.focusRing),
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            interactionSource = source,
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusGlow(focused = focused && error == null, cornerRadius = FieldCorner)
                        .background(colors.fieldSurface, shape)
                        .border(1.dp, borderColor, shape)
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                ) {
                    if (value.isEmpty()) {
                        Text(text = placeholder, style = typography.fieldText, color = colors.textTertiary)
                    }
                    innerTextField()
                }
            },
        )
        if (error != null) {
            Spacer(Modifier.height(8.dp))
            Text(text = error, style = typography.statusReason, color = colors.danger)
        }
    }
}

@Preview(backgroundColor = 0xFF000000, showBackground = true, widthDp = 560)
@Composable
private fun TvTextFieldPreview() {
    IonTvTheme {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(17.dp),
        ) {
            TvTextField(
                value = "",
                onValueChange = {},
                label = "EMAIL",
                placeholder = "you@example.com",
                interactionSource = rememberFocusedInteraction(),
            )
            TvTextField(value = "syrex@", onValueChange = {}, label = "EMAIL", error = "Invalid email format")
        }
    }
}
