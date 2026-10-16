/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.rukamori.archivetune.utils

import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import moe.rukamori.archivetune.compat.AppContext
import moe.rukamori.archivetune.compat.Bitmap
import moe.rukamori.archivetune.compat.Context
import moe.rukamori.archivetune.compat.File
import moe.rukamori.archivetune.compat.Uri
import moe.rukamori.archivetune.compat.View
import moe.rukamori.archivetune.ui.component.LyricsShareImageOptions

/**
 * Immagini da condividere (testi, copertine). L'originale disegnava con android.graphics;
 * qui si lavora su ImageBitmap di Compose. La cattura di una View e l'immagine dei testi
 * generata a mano non sono ancora disponibili.
 */
object ComposeToImage {
    suspend fun captureViewBitmap(
        view: View,
        targetWidth: Int? = null,
        targetHeight: Int? = null,
        backgroundColor: Int? = null,
    ): Bitmap = throw UnsupportedOperationException("Cattura della schermata non disponibile")

    fun cropBitmap(
        source: Bitmap,
        left: Int,
        top: Int,
        width: Int,
        height: Int,
    ): Bitmap {
        val safeLeft = left.coerceIn(0, (source.width - 1).coerceAtLeast(0))
        val safeTop = top.coerceIn(0, (source.height - 1).coerceAtLeast(0))
        val safeWidth = width.coerceIn(1, source.width - safeLeft)
        val safeHeight = height.coerceIn(1, source.height - safeTop)
        val target = ImageBitmap(safeWidth, safeHeight)
        Canvas(target).drawImageRect(
            image = source.image,
            srcOffset = IntOffset(safeLeft, safeTop),
            srcSize = IntSize(safeWidth, safeHeight),
            dstOffset = IntOffset.Zero,
            dstSize = IntSize(safeWidth, safeHeight),
            paint = Paint(),
        )
        return Bitmap(target)
    }

    fun fitBitmap(
        source: Bitmap,
        targetWidth: Int,
        targetHeight: Int,
        backgroundColor: Int,
    ): Bitmap {
        val target = ImageBitmap(targetWidth.coerceAtLeast(1), targetHeight.coerceAtLeast(1))
        val canvas = Canvas(target)
        canvas.drawRect(0f, 0f, target.width.toFloat(), target.height.toFloat(), Paint().apply { color = Color(backgroundColor) })
        val scale =
            minOf(
                targetWidth.toFloat() / source.width.coerceAtLeast(1),
                targetHeight.toFloat() / source.height.coerceAtLeast(1),
            )
        val scaledW = (source.width * scale).toInt().coerceAtLeast(1)
        val scaledH = (source.height * scale).toInt().coerceAtLeast(1)
        canvas.drawImageRect(
            image = source.image,
            srcOffset = IntOffset.Zero,
            srcSize = IntSize(source.width, source.height),
            dstOffset = IntOffset((targetWidth - scaledW) / 2, (targetHeight - scaledH) / 2),
            dstSize = IntSize(scaledW, scaledH),
            paint = Paint(),
        )
        return Bitmap(target)
    }

    suspend fun createLyricsImage(
        context: Context,
        coverArtUrl: String?,
        songTitle: String,
        artistName: String,
        lyrics: String,
        width: Int,
        height: Int,
        backgroundColor: Int? = null,
        textColor: Int? = null,
        secondaryTextColor: Int? = null,
        glassStyle: moe.rukamori.archivetune.ui.component.LyricsGlassStyle? = null,
        shareOptions: LyricsShareImageOptions = LyricsShareImageOptions(),
    ): Bitmap = throw UnsupportedOperationException("Immagine dei testi non disponibile")

    /** Salva l'immagine come PNG nella cache dell'app e ne restituisce l'indirizzo file://. */
    fun saveBitmapAsFile(
        context: Context,
        bitmap: Bitmap,
        fileName: String,
    ): Uri {
        val directory = File(AppContext.cacheDir, "images")
        directory.mkdirs()
        val file = File(directory, "$fileName.png")
        file.writeBytes(moe.rukamori.archivetune.compat.encodePng(bitmap.image))
        return Uri.parse("file://" + file.absolutePath)
    }
}
