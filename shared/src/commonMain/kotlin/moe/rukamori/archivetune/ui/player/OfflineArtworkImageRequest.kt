/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.rukamori.archivetune.ui.player

import moe.rukamori.archivetune.compat.platformContext

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import moe.rukamori.archivetune.compat.LocalContext
import coil3.request.CachePolicy
import coil3.request.ImageRequest

@Composable
internal fun rememberOfflineArtworkImageRequest(imageUrl: String?): ImageRequest? {
    val context = LocalContext.current
    return remember(context, imageUrl) {
        imageUrl
            ?.trim()
            ?.takeIf(String::isNotBlank)
            ?.let { url ->
                ImageRequest
                    .Builder(context.platformContext)
                    .data(url)
                    .memoryCacheKey(url)
                    .diskCacheKey(url)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .networkCachePolicy(CachePolicy.ENABLED)
                    .build()
            }
    }
}
