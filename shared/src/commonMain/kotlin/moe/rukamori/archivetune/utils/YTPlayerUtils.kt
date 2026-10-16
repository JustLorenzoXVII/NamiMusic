/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.rukamori.archivetune.utils

import com.justlorenzo.archivetunex.player.StreamResolver
import moe.rukamori.archivetune.compat.media3.PlaybackException
import moe.rukamori.archivetune.innertube.YouTube
import moe.rukamori.archivetune.innertube.PlaybackAuthState

/**
 * Parte di utils/YTPlayerUtils.kt dell'originale usata dalle schermate.
 * La risoluzione degli stream in ArchiveTuneX la fa StreamResolver (youtubei.js in QuickJS).
 */
object YTPlayerUtils {
    class LoginRequiredForPlaybackException(
        val videoId: String,
        val targetUrl: String,
        reason: String?,
        cause: Throwable? = null,
    ) : IllegalStateException(reason, cause)

    class InvalidPlaybackLoginContextException(
        val videoId: String,
        val targetUrl: String,
        cause: Throwable,
    ) : IllegalStateException("Invalid YouTube Music playback login context", cause)

    class BotDetectionPlaybackException(
        message: String,
        cause: Throwable? = null,
    ) : IllegalStateException(message, cause)

    class BadStreamPlayerResponseException(
        message: String,
        cause: Throwable? = null,
    ) : IllegalStateException(message, cause)

    fun clearPlaybackAuthCaches() {
        StreamResolver.invalidate()
    }

    suspend fun ensureWebPoTokensForSubtitles(videoId: String): PlaybackAuthState = YouTube.currentPlaybackAuthState()

    internal fun isBotDetectionError(reason: String): Boolean {
        val lower = reason.lowercase()
        return "bot" in lower ||
            "unusual traffic" in lower ||
            "automated" in lower ||
            "confirm" in lower && "not a" in lower ||
            "not a robot" in lower ||
            "verify" in lower && "human" in lower
    }

    fun isBotDetectionException(error: PlaybackException): Boolean {
        if (isBotDetectionError(error.message.orEmpty())) return true
        var cause: Throwable? = error.cause
        while (cause != null) {
            if (cause is BotDetectionPlaybackException) return true
            if (isBotDetectionError(cause.message.orEmpty())) return true
            cause = cause.cause
        }
        return false
    }

    fun isBadStreamPlayerResponseException(error: PlaybackException): Boolean {
        var cause: Throwable? = error
        while (cause != null) {
            if (cause is BadStreamPlayerResponseException) return true
            if (cause.message?.contains("YouTube playback stream clients returned no playable response") == true) return true
            cause = cause.cause
        }
        return false
    }
}
