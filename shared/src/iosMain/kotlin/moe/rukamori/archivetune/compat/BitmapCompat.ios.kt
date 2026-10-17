package moe.rukamori.archivetune.compat

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asComposeImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import coil3.toBitmap

internal actual fun platformCoilImageToImageBitmap(image: coil3.Image): ImageBitmap = image.toBitmap().asComposeImageBitmap()

actual fun encodePng(image: ImageBitmap): ByteArray =
    org.jetbrains.skia.Image
        .makeFromBitmap(image.asSkiaBitmap())
        .encodeToData(org.jetbrains.skia.EncodedImageFormat.PNG)
        ?.bytes ?: ByteArray(0)

actual fun imageBitmapFromArgb(
    pixels: IntArray,
    width: Int,
    height: Int,
): androidx.compose.ui.graphics.ImageBitmap {
    // Skia su iPhone: BGRA little-endian, cioè i byte B, G, R, A per ogni pixel ARGB
    val bytes = ByteArray(pixels.size * 4)
    for (i in pixels.indices) {
        val c = pixels[i]
        bytes[i * 4] = (c and 0xFF).toByte()
        bytes[i * 4 + 1] = ((c shr 8) and 0xFF).toByte()
        bytes[i * 4 + 2] = ((c shr 16) and 0xFF).toByte()
        bytes[i * 4 + 3] = ((c ushr 24) and 0xFF).toByte()
    }
    val info =
        org.jetbrains.skia.ImageInfo(
            width,
            height,
            org.jetbrains.skia.ColorType.BGRA_8888,
            org.jetbrains.skia.ColorAlphaType.UNPREMUL,
        )
    val bitmap = org.jetbrains.skia.Bitmap()
    bitmap.allocPixels(info)
    bitmap.installPixels(info, bytes, width * 4)
    return bitmap.asComposeImageBitmap()
}
