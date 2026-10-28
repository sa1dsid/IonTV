package com.ionica.iontv.core.designsystem.component

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.ionica.iontv.core.designsystem.theme.IonTvTheme
import kotlin.math.floor

private const val QUIET_ZONE_MODULES = 2

@Composable
fun QrCode(
    content: String,
    modifier: Modifier = Modifier,
    size: Dp = 196.dp,
    cornerRadius: Dp = 10.dp,
) {
    val colors = IonTvTheme.colors
    val matrix = remember(content) { encodeQr(content) }

    Spacer(
        modifier = modifier
            .size(size)
            .clearAndSetSemantics { }
            .drawWithCache {
                val cells = matrix.width + QUIET_ZONE_MODULES * 2
                val module = floor(this.size.minDimension / cells)
                val origin = (this.size.minDimension - module * matrix.width) / 2f
                val modules = Path().apply {
                    for (y in 0 until matrix.height) {
                        for (x in 0 until matrix.width) {
                            if (!matrix[x, y]) continue
                            val left = origin + x * module
                            val top = origin + y * module
                            addRect(Rect(left, top, left + module, top + module))
                        }
                    }
                }
                val radius = CornerRadius(cornerRadius.toPx())

                onDrawBehind {
                    drawRoundRect(color = colors.qrLight, cornerRadius = radius)
                    drawPath(modules, color = colors.qrDark)
                }
            },
    )
}

private fun encodeQr(content: String): BitMatrix = QRCodeWriter().encode(
    content,
    BarcodeFormat.QR_CODE,
    0,
    0,
    mapOf(
        EncodeHintType.MARGIN to 0,
        EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
    ),
)

@Preview(backgroundColor = 0xFF000000, showBackground = true)
@Composable
private fun QrCodePreview() {
    IonTvTheme {
        QrCode(content = "7K2M4Q")
    }
}
