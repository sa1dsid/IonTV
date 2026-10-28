package com.ionica.iontv.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.ionica.iontv.R

private val SpaceGrotesk = FontFamily(
    Font(R.font.space_grotesk_regular, FontWeight.Normal),
    Font(R.font.space_grotesk_medium, FontWeight.Medium),
    Font(R.font.space_grotesk_semibold, FontWeight.SemiBold),
    Font(R.font.space_grotesk_bold, FontWeight.Bold),
)

private val JetBrainsMono = FontFamily(
    Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
    Font(R.font.jetbrains_mono_medium, FontWeight.Medium),
    Font(R.font.jetbrains_mono_bold, FontWeight.Bold),
)

private val EvenLineHeight = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.None,
)

@Immutable
data class IonTvTypography(
    val logo: TextStyle,
    val title: TextStyle,
    val body: TextStyle,
    val consentTitle: TextStyle,
    val consentBody: TextStyle,
    val button: TextStyle,
    val buttonLarge: TextStyle,
    val buttonCompact: TextStyle,
    val label: TextStyle,
    val fieldLabel: TextStyle,
    val fieldText: TextStyle,
    val actionLabel: TextStyle,
    val separator: TextStyle,
    val pairingCode: TextStyle,
    val notice: TextStyle,
    val statusHeadline: TextStyle,
    val statusReason: TextStyle,
    val statusMeta: TextStyle,
    val footer: TextStyle,
    val ambient: TextStyle,
    val popupTitle: TextStyle,
    val popupBody: TextStyle,
    val legal: TextStyle,
    val legalLabel: TextStyle,
    val bannerTitle: TextStyle,
    val bannerBody: TextStyle,
    val bannerMeta: TextStyle,
)

val IonTvDefaultTypography = IonTvTypography(
    logo = TextStyle(
        fontFamily = SpaceGrotesk,
        fontWeight = FontWeight.Bold,
        fontSize = 47.sp,
        letterSpacing = 0.06.em,
    ),
    title = TextStyle(
        fontFamily = SpaceGrotesk,
        fontWeight = FontWeight.SemiBold,
        fontSize = 30.sp,
        lineHeight = 38.sp,
        lineHeightStyle = EvenLineHeight,
    ),
    body = TextStyle(
        fontFamily = SpaceGrotesk,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 29.sp,
        lineHeightStyle = EvenLineHeight,
    ),
    consentTitle = TextStyle(
        fontFamily = SpaceGrotesk,
        fontWeight = FontWeight.Bold,
        fontSize = 25.5.sp,
        lineHeight = 39.5.sp,
        lineHeightStyle = EvenLineHeight,
    ),
    consentBody = TextStyle(
        fontFamily = SpaceGrotesk,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 23.sp,
        lineHeightStyle = EvenLineHeight,
    ),
    button = TextStyle(
        fontFamily = SpaceGrotesk,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 23.sp,
        lineHeightStyle = EvenLineHeight,
    ),
    buttonLarge = TextStyle(
        fontFamily = SpaceGrotesk,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.5.sp,
        lineHeight = 28.5.sp,
        lineHeightStyle = EvenLineHeight,
    ),
    buttonCompact = TextStyle(
        fontFamily = SpaceGrotesk,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 21.sp,
        lineHeightStyle = EvenLineHeight,
    ),
    label = TextStyle(
        fontFamily = SpaceGrotesk,
        fontWeight = FontWeight.Normal,
        fontSize = 22.5.sp,
        lineHeight = 28.5.sp,
        lineHeightStyle = EvenLineHeight,
    ),
    fieldLabel = TextStyle(
        fontFamily = JetBrainsMono,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        letterSpacing = 2.5.sp,
    ),
    fieldText = TextStyle(
        fontFamily = SpaceGrotesk,
        fontWeight = FontWeight.Normal,
        fontSize = 17.sp,
        lineHeight = 22.sp,
        lineHeightStyle = EvenLineHeight,
    ),
    actionLabel = TextStyle(
        fontFamily = JetBrainsMono,
        fontWeight = FontWeight.Normal,
        fontSize = 21.sp,
    ),
    separator = TextStyle(
        fontFamily = JetBrainsMono,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        letterSpacing = 0.2.em,
    ),
    pairingCode = TextStyle(
        fontFamily = JetBrainsMono,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        letterSpacing = 0.2.em,
    ),
    notice = TextStyle(
        fontFamily = JetBrainsMono,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
    ),
    statusHeadline = TextStyle(
        fontFamily = SpaceGrotesk,
        fontWeight = FontWeight.Bold,
        fontSize = 39.sp,
        lineHeight = 60.sp,
        letterSpacing = 2.3.sp,
        lineHeightStyle = EvenLineHeight,
    ),
    statusReason = TextStyle(
        fontFamily = SpaceGrotesk,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        lineHeightStyle = EvenLineHeight,
    ),
    statusMeta = TextStyle(
        fontFamily = JetBrainsMono,
        fontWeight = FontWeight.Normal,
        fontSize = 9.8.sp,
        lineHeight = 15.sp,
        lineHeightStyle = EvenLineHeight,
    ),
    footer = TextStyle(
        fontFamily = JetBrainsMono,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 25.5.sp,
        lineHeightStyle = EvenLineHeight,
    ),
    ambient = TextStyle(
        fontFamily = JetBrainsMono,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        letterSpacing = 0.3.em,
    ),
    popupTitle = TextStyle(
        fontFamily = SpaceGrotesk,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 31.sp,
        lineHeightStyle = EvenLineHeight,
    ),
    popupBody = TextStyle(
        fontFamily = SpaceGrotesk,
        fontWeight = FontWeight.Normal,
        fontSize = 18.sp,
        lineHeight = 30.sp,
        lineHeightStyle = EvenLineHeight,
    ),
    legal = TextStyle(
        fontFamily = SpaceGrotesk,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        lineHeightStyle = EvenLineHeight,
    ),
    legalLabel = TextStyle(
        fontFamily = SpaceGrotesk,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 21.sp,
        lineHeightStyle = EvenLineHeight,
    ),
    bannerTitle = TextStyle(
        fontFamily = SpaceGrotesk,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.5.sp,
        lineHeight = 25.5.sp,
        lineHeightStyle = EvenLineHeight,
    ),
    bannerBody = TextStyle(
        fontFamily = SpaceGrotesk,
        fontWeight = FontWeight.Normal,
        fontSize = 10.5.sp,
        lineHeight = 16.5.sp,
        lineHeightStyle = EvenLineHeight,
    ),
    bannerMeta = TextStyle(
        fontFamily = JetBrainsMono,
        fontWeight = FontWeight.Normal,
        fontSize = 10.5.sp,
        lineHeight = 18.sp,
        lineHeightStyle = EvenLineHeight,
    ),
)
