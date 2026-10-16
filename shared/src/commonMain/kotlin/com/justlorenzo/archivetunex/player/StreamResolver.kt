package com.justlorenzo.archivetunex.player

import moe.rukamori.archivetune.innertube.YouTube
import moe.rukamori.archivetune.morideobfuscator.youtubei.YoutubeiAudioQuality
import moe.rukamori.archivetune.morideobfuscator.youtubei.YoutubeiResolutionPriority
import moe.rukamori.archivetune.morideobfuscator.youtubei.YoutubeiResolvedStream
import moe.rukamori.archivetune.morideobfuscator.youtubei.YoutubeiResolver
import moe.rukamori.archivetune.morideobfuscator.youtubei.YoutubeiStreamRequest
import moe.rukamori.archivetune.morideobfuscator.youtubei.currentTimeZoneId

/**
 * Trova lo stream audio di un brano come fa ArchiveTune: youtubei.js dentro QuickJS
 * (modulo morideobfuscator), con il client VISIONOS e la decifratura completa.
 */
object StreamResolver {
    private val resolver = YoutubeiResolver(diagnostics = { message -> println("Youtubei: $message") })

    suspend fun resolve(videoId: String): Result<YoutubeiResolvedStream> =
        runCatching {
            val authState = YouTube.currentPlaybackAuthState()
            val locale = YouTube.locale
            resolver.resolve(
                request =
                    YoutubeiStreamRequest(
                        mediaId = videoId,
                        quality = YoutubeiAudioQuality.HIGHEST,
                        networkMetered = false,
                        authFingerprint = authState.streamCacheFingerprint,
                        pinnedItag = preferredAudioItag(),
                        visitorData = authState.visitorData,
                        language = locale.hl,
                        location = locale.gl,
                        timezone = currentTimeZoneId(),
                    ),
                priority = YoutubeiResolutionPriority.FOREGROUND,
            )
        }
}
