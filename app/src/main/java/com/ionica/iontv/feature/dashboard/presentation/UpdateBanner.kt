package com.ionica.iontv.feature.dashboard.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ionica.iontv.R
import com.ionica.iontv.core.designsystem.component.QrCode
import com.ionica.iontv.core.designsystem.theme.IonTvTheme
import com.ionica.iontv.feature.update.domain.model.AppUpdate

private val BannerWidth = 734.dp

private val BannerCorner = 9.dp

private val BannerQrSize = 58.5.dp

@Composable
internal fun UpdateBanner(update: AppUpdate, modifier: Modifier = Modifier) {
    val colors = IonTvTheme.colors
    val typography = IonTvTheme.typography
    val shape = RoundedCornerShape(BannerCorner)

    Row(
        modifier = modifier
            .width(BannerWidth)
            .background(colors.bannerSurface, shape)
            .border(1.dp, colors.bannerOutline, shape)
            .padding(horizontal = 19.5.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.dashboard_update_title),
                style = typography.bannerTitle,
                color = colors.textPrimary,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.dashboard_update_text),
                style = typography.bannerBody,
                color = colors.textEmphasis,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = update.downloadUrl.substringAfter("://").trimEnd('/'),
                style = typography.bannerMeta,
                color = colors.success,
                textAlign = TextAlign.End,
            )
            Text(
                text = stringResource(R.string.dashboard_version, update.versionName),
                style = typography.bannerMeta,
                color = colors.textTertiary,
            )
        }
        Spacer(Modifier.width(12.dp))
        QrCode(content = update.installationUrl, size = BannerQrSize, cornerRadius = 0.dp)
    }
}
