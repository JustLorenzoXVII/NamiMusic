/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.rukamori.archivetune.ui.player

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import moe.rukamori.archivetune.canvas.CanvasSource
import moe.rukamori.archivetune.compat.AspectRatioFrameLayout
import moe.rukamori.archivetune.compat.Bitmap

/**
 * Copertina animata (Canvas). Nell'originale era un secondo ExoPlayer con TextureView;
 * in ArchiveTuneX non è ancora disponibile, quindi resta la copertina statica.
 */
@Composable
internal fun CanvasArtworkPlayer(
    source: CanvasSource?,
    primaryUrl: String?,
    fallbackUrl: String?,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    resizeMode: Int = AspectRatioFrameLayout.RESIZE_MODE_FIT,
    onFrameCaptured: ((Bitmap?) -> Unit)? = null,
) = Unit
