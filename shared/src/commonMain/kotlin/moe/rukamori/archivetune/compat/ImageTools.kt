package moe.rukamori.archivetune.compat

/**
 * Operazioni sulle immagini che nell'originale usavano android.graphics.Bitmap.
 * Android: Bitmap/BitmapFactory; iPhone: UIImage.
 */
object ImageTools {
    /**
     * Decodifica l'immagine (rispettando l'orientamento EXIF), la ritaglia al centro in un quadrato,
     * la riduce a [maxDimension] e la comprime in JPEG abbassando la qualità finché sta in [maxBytes].
     */
    fun prepareSquareJpeg(
        bytes: ByteArray,
        maxDimension: Int,
        maxBytes: Int,
        initialQuality: Int = 92,
        minQuality: Int = 60,
        qualityStep: Int = 8,
    ): ByteArray = platformPrepareSquareJpeg(bytes, maxDimension, maxBytes, initialQuality, minQuality, qualityStep)

    /** Larghezza e altezza dell'immagine, o null se non è un'immagine valida. */
    fun imageSize(bytes: ByteArray): Pair<Int, Int>? = platformImageSize(bytes)

    fun isDecodableImage(bytes: ByteArray): Boolean = imageSize(bytes)?.let { it.first > 0 && it.second > 0 } == true
}

internal expect fun platformPrepareSquareJpeg(
    bytes: ByteArray,
    maxDimension: Int,
    maxBytes: Int,
    initialQuality: Int,
    minQuality: Int,
    qualityStep: Int,
): ByteArray

internal expect fun platformImageSize(bytes: ByteArray): Pair<Int, Int>?
