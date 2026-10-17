package moe.rukamori.archivetune.compat

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toPixelMap
import moe.rukamori.archivetune.ui.theme.extractGradientColors
import moe.rukamori.archivetune.ui.theme.extractThemeColor

/**
 * android.graphics.Bitmap per l'interfaccia: incapsula una ImageBitmap di Compose,
 * che esiste sia su Android sia su iPhone.
 */
class Bitmap(
    image: ImageBitmap,
) {
    /** L'immagine attuale: setPixels la sostituisce con una nuova (le ImageBitmap non si modificano). */
    var image: ImageBitmap = image
        private set

    val width: Int
        get() = image.width
    val height: Int
        get() = image.height
    val isRecycled: Boolean
        get() = false
    val config: Config
        get() = Config.ARGB_8888

    fun asImageBitmap(): ImageBitmap = image

    fun copy(
        config: Config,
        isMutable: Boolean,
    ): Bitmap = Bitmap(image)

    /** Come Bitmap.getPixels: legge i pixel ARGB in [pixels]. */
    fun getPixels(
        pixels: IntArray,
        offset: Int,
        stride: Int,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
    ) {
        image.readPixels(pixels, x, y, width, height, offset, stride)
    }

    /** Come Bitmap.setPixels per l'intera immagine (l'unico uso in ArchiveTune). */
    fun setPixels(
        pixels: IntArray,
        offset: Int,
        stride: Int,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
    ) {
        val full = IntArray(this.width * this.height)
        image.readPixels(full)
        for (row in 0 until height) {
            for (col in 0 until width) {
                full[(y + row) * this.width + (x + col)] = pixels[offset + row * stride + col]
            }
        }
        image = imageBitmapFromArgb(full, this.width, this.height)
    }

    fun recycle() = Unit

    fun getPixel(
        x: Int,
        y: Int,
    ): Int = image.toPixelMap(x, y, 1, 1)[0, 0].let { c -> AndroidColor.argb((c.alpha * 255).toInt(), (c.red * 255).toInt(), (c.green * 255).toInt(), (c.blue * 255).toInt()) }

    fun extractThemeColor() = image.extractThemeColor()

    fun extractGradientColors() = image.extractGradientColors()

    enum class Config { ALPHA_8, RGB_565, ARGB_8888, RGBA_F16, HARDWARE }

    companion object {
        fun createBitmap(
            width: Int,
            height: Int,
            config: Config = Config.ARGB_8888,
        ): Bitmap = Bitmap(ImageBitmap(width, height))

        fun createScaledBitmap(
            source: Bitmap,
            width: Int,
            height: Int,
            filter: Boolean,
        ): Bitmap = Bitmap(scaleImageBitmap(source.image, width, height))
    }
}

fun ImageBitmap.asBitmap(): Bitmap = Bitmap(this)

/** coil3: immagine caricata → Bitmap. */
fun coil3.Image.toBitmap(): Bitmap = Bitmap(platformCoilImageToImageBitmap(this))

internal expect fun platformCoilImageToImageBitmap(image: coil3.Image): ImageBitmap

/** Ridimensiona un'immagine disegnandola su una nuova ImageBitmap. */
fun scaleImageBitmap(
    source: ImageBitmap,
    width: Int,
    height: Int,
): ImageBitmap {
    val target = ImageBitmap(width.coerceAtLeast(1), height.coerceAtLeast(1))
    val canvas = androidx.compose.ui.graphics.Canvas(target)
    canvas.drawImageRect(
        image = source,
        srcOffset = androidx.compose.ui.unit.IntOffset.Zero,
        srcSize = androidx.compose.ui.unit.IntSize(source.width, source.height),
        dstOffset = androidx.compose.ui.unit.IntOffset.Zero,
        dstSize = androidx.compose.ui.unit.IntSize(target.width, target.height),
        paint = androidx.compose.ui.graphics.Paint().apply { filterQuality = androidx.compose.ui.graphics.FilterQuality.Low },
    )
    return target
}

/** Codifica un'immagine in PNG. */
expect fun encodePng(image: ImageBitmap): ByteArray

/** Crea una ImageBitmap da pixel ARGB (Android: android.graphics.Bitmap, iPhone: Skia). */
expect fun imageBitmapFromArgb(
    pixels: IntArray,
    width: Int,
    height: Int,
): ImageBitmap
