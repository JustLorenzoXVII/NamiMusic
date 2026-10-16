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
