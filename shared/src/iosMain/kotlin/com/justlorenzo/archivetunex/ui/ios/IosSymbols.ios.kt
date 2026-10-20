@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.justlorenzo.archivetunex.ui.ios

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.CoreGraphics.CGPointMake
import platform.Foundation.NSData
import platform.UIKit.UIColor
import platform.UIKit.UIGraphicsImageRenderer
import platform.UIKit.UIImage
import platform.UIKit.UIImageRenderingMode
import platform.UIKit.UIImageSymbolConfiguration
import platform.UIKit.UIImageSymbolWeightBold
import platform.UIKit.UIImageSymbolWeightMedium
import platform.posix.memcpy

/** Le SF Symbols già disegnate (nome, grandezza, peso): si disegnano una volta sola. */
private val cache = HashMap<String, ImageBitmap?>()

@Composable
actual fun rememberSystemSymbol(
    name: String,
    pointSize: Float,
    bold: Boolean,
): ImageBitmap? {
    val key = "$name|$pointSize|$bold"
    return remember(key) { cache.getOrPut(key) { drawSymbol(name, pointSize, bold) } }
}

private fun drawSymbol(
    name: String,
    pointSize: Float,
    bold: Boolean,
): ImageBitmap? =
    runCatching {
        val configuration =
            UIImageSymbolConfiguration.configurationWithPointSize(
                pointSize.toDouble(),
                weight = if (bold) UIImageSymbolWeightBold else UIImageSymbolWeightMedium,
            )
        val symbol = UIImage.systemImageNamed(name, withConfiguration = configuration) ?: return null
        // Bianca: in Compose la colora la tinta dell'icona
        val white = symbol.imageWithTintColor(UIColor.whiteColor, renderingMode = UIImageRenderingMode.UIImageRenderingModeAlwaysOriginal)
        val renderer = UIGraphicsImageRenderer(size = white.size)
        val png = renderer.PNGDataWithActions { _ -> white.drawAtPoint(CGPointMake(0.0, 0.0)) }
        org.jetbrains.skia.Image
            .makeFromEncoded(png.toByteArray())
            .toComposeImageBitmap()
    }.getOrNull()

private fun NSData.toByteArray(): ByteArray {
    val size = length.toInt()
    val bytes = ByteArray(size)
    if (size > 0) bytes.usePinned { memcpy(it.addressOf(0), this.bytes, length) }
    return bytes
}
