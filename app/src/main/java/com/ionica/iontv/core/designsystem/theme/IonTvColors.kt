package com.ionica.iontv.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
data class IonTvColors(
    val background: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textBody: Color,
    val textTertiary: Color,
    val textFooter: Color,
    val textEmphasis: Color,
    val buttonSurface: Color,
    val buttonText: Color,
    val outline: Color,
    val focusRing: Color,
    val fieldSurface: Color,
    val divider: Color,
    val link: Color,
    val spinnerTrack: Color,
    val success: Color,
    val danger: Color,
    val scrim: Color,
    val ambientText: Color,
    val qrLight: Color,
    val qrDark: Color,
)

val IonTvDarkColors = IonTvColors(
    background = Color(0xFF000000),
    textPrimary = Color(0xFFF2F6F8),
    textSecondary = Color(0xFF8A8F93),
    textBody = Color(0xFFD6DADD),
    textTertiary = Color(0xFF5E6367),
    textFooter = Color(0xFF6B7075),
    textEmphasis = Color(0xFFC9CDD0),
    buttonSurface = Color(0xFF15181A),
    buttonText = Color(0xFFE8EDF0),
    outline = Color(0xFF2A2E31),
    focusRing = Color(0xFFAEBFC7),
    fieldSurface = Color(0xFF0D1013),
    divider = Color(0xFF16181A),
    link = Color(0xFFA8D8F0),
    spinnerTrack = Color(0xFF24282B),
    success = Color(0xFF9BE8C8),
    danger = Color(0xFFE04957),
    scrim = Color(0x99000000),
    ambientText = Color(0xFF2E3235),
    qrLight = Color(0xFFF2F6F8),
    qrDark = Color(0xFF0A0A0A),
)
