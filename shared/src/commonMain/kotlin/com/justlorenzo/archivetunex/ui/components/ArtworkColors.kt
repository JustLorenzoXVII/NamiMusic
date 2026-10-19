package com.justlorenzo.archivetunex.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import coil3.SingletonImageLoader
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import moe.rukamori.archivetune.compat.Palette
import moe.rukamori.archivetune.compat.allowHardware
import moe.rukamori.archivetune.compat.toBitmap
import moe.rukamori.archivetune.ui.theme.PlayerColorExtractor
import moe.rukamori.archivetune.ui.utils.resize

/** Colori presi da una copertina: il principale e la sfumatura per gli sfondi. */
data class ArtworkColors(
    val primary: Color,
    val gradient: List<Color>,
)

private val colorCache = mutableMapOf<String, ArtworkColors>()

/**
 * Estrae i colori della copertina (con la stessa Palette del player di ArchiveTune).
 * Restituisce null finché non sono pronti; il risultato resta in cache.
 */
@Composable
fun rememberArtworkColors(url: String?): ArtworkColors? {
    val context = LocalPlatformContext.current
    var colors by remember(url) { mutableStateOf(url?.let { colorCache[it] }) }
    LaunchedEffect(url) {
        if (url.isNullOrBlank() || colors != null) return@LaunchedEffect
        colors =
            try {
                val request =
                    ImageRequest
                        .Builder(context)
                        .data(url.resize(PlayerColorExtractor.Config.IMAGE_SIZE, PlayerColorExtractor.Config.IMAGE_SIZE))
                        .size(PlayerColorExtractor.Config.IMAGE_SIZE, PlayerColorExtractor.Config.IMAGE_SIZE)
                        .allowHardware(false)
                        .build()
                val image = withContext(Dispatchers.IO) { SingletonImageLoader.get(context).execute(request) }.image
                if (image == null) {
                    null
                } else {
                    val palette =
                        withContext(Dispatchers.Default) {
                            Palette
                                .from(image.toBitmap())
                                .maximumColorCount(PlayerColorExtractor.Config.MAX_COLOR_COUNT)
                                .resizeBitmapArea(PlayerColorExtractor.Config.BITMAP_AREA)
                                .generate()
                        }
                    val fallback = Color(0xFFFF8FB8).toArgb()
                    val primary = Color(palette.getVibrantColor(palette.getDominantColor(fallback)))
                    val gradient = PlayerColorExtractor.extractGradientColors(palette, fallback)
                    ArtworkColors(primary, gradient.ifEmpty { listOf(primary) }).also { colorCache[url] = it }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                null
            }
    }
    return colors
}

/** Colore della copertina animato nei cambi di brano, con un valore di riserva. */
@Composable
fun animatedArtworkColor(
    colors: ArtworkColors?,
    fallback: Color = MaterialTheme.colorScheme.primary,
): Color {
    val target = colors?.primary ?: fallback
    val animated by animateColorAsState(target, tween(600), label = "artworkColor")
    return animated
}

/** Testo leggibile sopra un colore (bianco o quasi nero). */
fun Color.readableOn(): Color = if (luminance() > 0.5f) Color(0xFF111114) else Color.White

/** Il colore mescolato con lo sfondo, per tinte morbide di card e sfondi. */
fun Color.tint(
    background: Color,
    amount: Float,
): Color = copy(alpha = amount).compositeOver(background)
