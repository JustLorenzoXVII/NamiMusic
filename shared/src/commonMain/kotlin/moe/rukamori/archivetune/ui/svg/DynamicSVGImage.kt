/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.rukamori.archivetune.ui.svg

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.svg.SvgDecoder
import moe.rukamori.archivetune.ui.theme.palette.TonalPalettes

/** Illustrazione SVG con i colori del tema (l'originale usava AndroidSVG, qui il decoder SVG di Coil). */
@Composable
fun DynamicSVGImage(
    svgImageString: String,
    tonalPalettes: TonalPalettes,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier,
) {
    val context = LocalPlatformContext.current
    val request =
        remember(svgImageString, isDarkTheme, tonalPalettes) {
            ImageRequest
                .Builder(context)
                .data(svgImageString.parseDynamicColor(tonalPalettes, isDarkTheme).encodeToByteArray())
                .decoderFactory(SvgDecoder.Factory())
                .memoryCacheKey("svg:${svgImageString.hashCode()}:$isDarkTheme:${tonalPalettes.hashCode()}")
                .build()
        }
    AsyncImage(
        model = request,
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = modifier,
    )
}
