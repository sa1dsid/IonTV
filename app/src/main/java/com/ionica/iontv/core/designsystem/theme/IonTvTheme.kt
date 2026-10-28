package com.ionica.iontv.core.designsystem.theme

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density

private const val SCREEN_WIDTH_DP = 960f

private const val SCREEN_HEIGHT_DP = 540f

private val LocalIonTvColors: ProvidableCompositionLocal<IonTvColors> =
    staticCompositionLocalOf { IonTvDarkColors }

private val LocalIonTvTypography: ProvidableCompositionLocal<IonTvTypography> =
    staticCompositionLocalOf { IonTvDefaultTypography }

object IonTvTheme {
    val colors: IonTvColors
        @Composable @ReadOnlyComposable get() = LocalIonTvColors.current

    val typography: IonTvTypography
        @Composable @ReadOnlyComposable get() = LocalIonTvTypography.current
}

@Composable
fun IonTvTheme(content: @Composable () -> Unit) {
    val colors = IonTvDarkColors
    val typography = IonTvDefaultTypography

    CompositionLocalProvider(
        LocalDensity provides rememberTvDensity(),
        LocalIonTvColors provides colors,
        LocalIonTvTypography provides typography,
        LocalTextStyle provides typography.body.copy(color = colors.textPrimary),
    ) {
        MaterialTheme(
            colorScheme = darkColorScheme(
                primary = colors.focusRing,
                background = colors.background,
                surface = colors.buttonSurface,
                onBackground = colors.textPrimary,
                onSurface = colors.textPrimary,
                error = colors.danger,
            ),
            content = content,
        )
    }
}

@Composable
private fun rememberTvDensity(): Density {
    val system = LocalDensity.current
    val configuration = LocalConfiguration.current
    val widthPx = configuration.screenWidthDp * system.density
    val heightPx = configuration.screenHeightDp * system.density

    return remember(widthPx, heightPx, system.fontScale) {
        if (widthPx <= 0f || heightPx <= 0f) {
            system
        } else {
            Density(minOf(widthPx / SCREEN_WIDTH_DP, heightPx / SCREEN_HEIGHT_DP), system.fontScale)
        }
    }
}
