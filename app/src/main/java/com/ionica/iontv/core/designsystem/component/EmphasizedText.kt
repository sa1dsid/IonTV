package com.ionica.iontv.core.designsystem.component

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.fromHtml

@Composable
fun emphasizedString(@StringRes id: Int, emphasis: SpanStyle): AnnotatedString {
    val html = stringResource(id)
    return remember(html, emphasis) {
        val parsed = AnnotatedString.fromHtml(html)
        buildAnnotatedString {
            append(parsed.text)
            parsed.spanStyles.forEach { addStyle(emphasis, it.start, it.end) }
        }
    }
}
