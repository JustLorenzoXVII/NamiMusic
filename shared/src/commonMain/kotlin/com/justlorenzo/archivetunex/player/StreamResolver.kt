package com.justlorenzo.archivetunex.player

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock
import moe.rukamori.archivetune.innertube.YouTube
import moe.rukamori.archivetune.morideobfuscator.youtubei.YoutubeiAudioQuality
import moe.rukamori.archivetune.morideobfuscator.youtubei.YoutubeiException
import moe.rukamori.archivetune.morideobfuscator.youtubei.YoutubeiFailureKind
import moe.rukamori.archivetune.morideobfuscator.youtubei.YoutubeiResolutionPriority
import moe.rukamori.archivetune.morideobfuscator.youtubei.YoutubeiResolvedStream
import moe.rukamori.archivetune.morideobfuscator.youtubei.YoutubeiResolver
import moe.rukamori.archivetune.morideobfuscator.youtubei.YoutubeiStreamRequest
import moe.rukamori.archivetune.morideobfuscator.youtubei.currentTimeZoneId

/**
 * Trova lo stream audio di un brano come fa ArchiveTune: youtubei.js dentro QuickJS
 * (modulo morideobfuscator), con il client VISIONOS e la decifratura completa.
 *
 * Come l'originale (YTPlayerUtils.ensureVisitorDataReady / recoverFromBadStreamPlayerResponse):
 * - prima di riprodurre si assicura di avere un visitorData vero di YouTube;
 * - se YouTube risponde "Sign in to confirm you're not a bot", rinnova il visitorData,
 *   ricrea la sessione di youtubei.js e riprova una volta.
 */
object StreamResolver {
    /** Qualità audio scelta nelle Impostazioni (la imposta ArchiveTuneXRuntime). */
    var audioQuality: YoutubeiAudioQuality = YoutubeiAudioQuality.HIGHEST

    private val resolver = YoutubeiResolver(diagnostics = { message -> println("Youtubei: $message") })

    /** Stream già trovati (riascoltare un brano o tornare indietro è immediato), finché l'URL vale. */
    private val memory = mutableMapOf<String, YoutubeiResolvedStream>()
    private val memoryLock = kotlinx.coroutines.sync.Mutex()

    private fun nowMs(): Long = kotlin.time.Clock.System.now().toEpochMilliseconds()

    suspend fun resolve(videoId: String): Result<YoutubeiResolvedStream> {
        memoryLock.withLock { memory[videoId] }?.let { cached ->
            // Margine di 10 minuti sulla scadenza dell'URL
            if (cached.expiresAtMs <= 0 || cached.expiresAtMs - nowMs() > 10 * 60 * 1000L) return Result.success(cached)
        }
        return resolveFresh(videoId).onSuccess { stream ->
            memoryLock.withLock {
                memory[videoId] = stream
                if (memory.size > 200) memory.remove(memory.keys.first())
            }
        }
    }

    private suspend fun resolveFresh(videoId: String): Result<YoutubeiResolvedStream> =
        try {
            ensureVisitorData(forceRefresh = false)
            Result.success(resolveOnce(videoId))
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (failure: Exception) {
            if (!failure.isBotDetection()) {
                Result.failure(failure)
            } else {
                try {
                    ensureVisitorData(forceRefresh = true)
                    resolver.invalidateSessions()
                    Result.success(resolveOnce(videoId))
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (retryFailure: Exception) {
                    Result.failure(retryFailure)
                }
            }
        }

    /** Dimentica sessioni e URL in cache (cambio account o errori di autenticazione). */
    fun invalidate() {
        invalidationScope.launch {
            memoryLock.withLock { memory.clear() }
            resolver.invalidateSessions()
        }
    }

    private val invalidationScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.Default)

    private suspend fun resolveOnce(videoId: String): YoutubeiResolvedStream {
        val authState = YouTube.currentPlaybackAuthState()
        val locale = YouTube.locale
        return resolver.resolve(
            request =
                YoutubeiStreamRequest(
                    mediaId = videoId,
                    quality = audioQuality,
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

    private suspend fun ensureVisitorData(forceRefresh: Boolean) {
        if (!forceRefresh && !YouTube.visitorData.isNullOrBlank()) return
        YouTube
            .visitorData()
            .getOrNull()
            ?.takeIf { it.isNotBlank() }
            ?.let { YouTube.visitorData = it }
    }

    private fun Throwable.isBotDetection(): Boolean {
        val youtubei = this as? YoutubeiException ?: return false
        if (youtubei.kind == YoutubeiFailureKind.PO_TOKEN) return true
        val message = youtubei.message.orEmpty().lowercase()
        return youtubei.kind == YoutubeiFailureKind.LOGIN_REQUIRED &&
            ("bot" in message || "confirm" in message && "not a" in message)
    }
}
