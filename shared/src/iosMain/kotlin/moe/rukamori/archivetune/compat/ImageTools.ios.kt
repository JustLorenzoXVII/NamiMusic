package moe.rukamori.archivetune.compat

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.useContents
import kotlinx.cinterop.usePinned
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.Foundation.NSData
import platform.Foundation.create
import platform.UIKit.UIGraphicsImageRenderer
import platform.UIKit.UIGraphicsImageRendererFormat
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.posix.memcpy

@OptIn(ExperimentalForeignApi::class)
private fun ByteArray.toNSData(): NSData =
    usePinned { pinned -> NSData.create(bytes = pinned.addressOf(0), length = size.toULong()) }

@OptIn(ExperimentalForeignApi::class)
private fun NSData.toByteArray(): ByteArray {
    val result = ByteArray(length.toInt())
    if (result.isNotEmpty()) result.usePinned { memcpy(it.addressOf(0), bytes, length) }
    return result
}

@OptIn(ExperimentalForeignApi::class)
internal actual fun platformImageSize(bytes: ByteArray): Pair<Int, Int>? {
    if (bytes.isEmpty()) return null
    val image = UIImage.imageWithData(bytes.toNSData()) ?: return null
    return image.size.useContents { (width * image.scale).toInt() to (height * image.scale).toInt() }
}

@OptIn(ExperimentalForeignApi::class)
internal actual fun platformPrepareSquareJpeg(
    bytes: ByteArray,
    maxDimension: Int,
    maxBytes: Int,
    initialQuality: Int,
    minQuality: Int,
    qualityStep: Int,
): ByteArray {
    val image = UIImage.imageWithData(bytes.toNSData()) ?: throw IllegalArgumentException("Selected image could not be decoded")
    // UIImage applica già l'orientamento EXIF quando disegna.
    val (width, height) = image.size.useContents { width to height }
    val square = minOf(width, height)
    val target = minOf(square, maxDimension.toDouble())
    val format = UIGraphicsImageRendererFormat.defaultFormat().apply { scale = 1.0 }
    val renderer = UIGraphicsImageRenderer(size = CGSizeMake(target, target), format = format)
    val scale = target / square
    val output =
        renderer.imageWithActions { _ ->
            image.drawInRect(CGRectMake(-(width - square) / 2 * scale, -(height - square) / 2 * scale, width * scale, height * scale))
        }
    var quality = initialQuality
    var data: ByteArray
    do {
        data = UIImageJPEGRepresentation(output, quality / 100.0)?.toByteArray() ?: throw IllegalStateException("JPEG non riuscito")
        quality -= qualityStep
    } while (data.size > maxBytes && quality >= minQuality)
    require(data.size <= maxBytes) { "Selected image is too large" }
    return data
}
