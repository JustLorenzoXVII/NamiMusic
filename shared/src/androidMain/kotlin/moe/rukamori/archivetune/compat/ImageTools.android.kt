package moe.rukamori.archivetune.compat

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import kotlin.math.min

internal actual fun platformImageSize(bytes: ByteArray): Pair<Int, Int>? {
    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
    return if (options.outWidth > 0 && options.outHeight > 0) options.outWidth to options.outHeight else null
}

internal actual fun platformPrepareSquareJpeg(
    bytes: ByteArray,
    maxDimension: Int,
    maxBytes: Int,
    initialQuality: Int,
    minQuality: Int,
    qualityStep: Int,
): ByteArray {
    val bounds = platformImageSize(bytes) ?: throw IllegalArgumentException("Selected image is not valid")
    var sampleSize = 1
    while (bounds.first.toLong() / sampleSize * (bounds.second.toLong() / sampleSize) > 8_000_000L) sampleSize *= 2
    val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
    var decoded =
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
            ?: throw IllegalArgumentException("Selected image could not be decoded")
    val rotation =
        runCatching {
            when (ExifInterface(bytes.inputStream()).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
        }.getOrDefault(0f)
    if (rotation != 0f) {
        decoded = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, Matrix().apply { postRotate(rotation) }, true)
    }
    val square = min(decoded.width, decoded.height)
    var bitmap = Bitmap.createBitmap(decoded, (decoded.width - square) / 2, (decoded.height - square) / 2, square, square)
    if (bitmap.width > maxDimension) bitmap = Bitmap.createScaledBitmap(bitmap, maxDimension, maxDimension, true)
    val output = java.io.ByteArrayOutputStream()
    var quality = initialQuality
    do {
        output.reset()
        check(bitmap.compress(Bitmap.CompressFormat.JPEG, quality, output))
        quality -= qualityStep
    } while (output.size() > maxBytes && quality >= minQuality)
    require(output.size() <= maxBytes) { "Selected image is too large" }
    return output.toByteArray()
}
