package com.ionica.iontv.core.designsystem.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.ionica.iontv.R
import com.ionica.iontv.core.designsystem.theme.IonTvTheme

private const val PLANET_EM = 0.95f

private const val GAP_EM = 0.1f

private const val PLANET_SHIFT_EM = 0.04f

@Composable
fun IonTvLogo(
    modifier: Modifier = Modifier,
    fontSize: TextUnit = IonTvTheme.typography.logo.fontSize,
) {
    val style = IonTvTheme.typography.logo.copy(fontSize = fontSize)
    val color = IonTvTheme.colors.textPrimary
    val brand = stringResource(R.string.app_name)

    val density = LocalDensity.current
    val planetSize = with(density) { (fontSize * PLANET_EM).toDp() }
    val gap = with(density) { (fontSize * GAP_EM).toDp() }
    val planetShift = with(density) { (fontSize * PLANET_SHIFT_EM).toDp() }

    Row(
        modifier = modifier.clearAndSetSemantics { contentDescription = brand },
        horizontalArrangement = Arrangement.spacedBy(gap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = stringResource(R.string.brand_i), style = style, color = color)
        Image(
            painter = painterResource(R.drawable.img_planet),
            contentDescription = null,
            modifier = Modifier
                .size(planetSize)
                .offset(y = planetShift),
        )
        Text(text = stringResource(R.string.brand_ntv), style = style, color = color)
    }
}

@Preview(backgroundColor = 0xFF000000, showBackground = true)
@Composable
private fun IonTvLogoPreview() {
    IonTvTheme {
        IonTvLogo()
    }
}

@Preview(backgroundColor = 0xFF000000, showBackground = true)
@Composable
private fun IonTvLogoSmallPreview() {
    IonTvTheme {
        IonTvLogo(fontSize = 22.sp)
    }
}
