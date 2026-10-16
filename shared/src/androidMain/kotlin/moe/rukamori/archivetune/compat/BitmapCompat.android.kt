package moe.rukamori.archivetune.compat

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import coil3.toBitmap

internal actual fun platformCoilImageToImageBitmap(image: coil3.Image): ImageBitmap = image.toBitmap().asImageBitmap()

/** android.graphics.Bitmap → Bitmap comune. */
fun android.graphics.Bitmap.toCompatBitmap(): Bitmap = Bitmap(asImageBitmap())

actual fun encodePng(image: ImageBitmap): ByteArray {
    val out = java.io.ByteArrayOutputStream()
    image.asAndroidBitmap().compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out)
    return out.toByteArray()
}
