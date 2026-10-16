package com.justlorenzo.archivetunex.player

import kotlinx.coroutines.CancellationException
import moe.rukamori.archivetune.innertube.StreamDecipher
import moe.rukamori.archivetune.innertube.YouTube
import moe.rukamori.archivetune.innertube.models.YouTubeClient
import moe.rukamori.archivetune.innertube.models.response.PlayerResponse

data class ResolvedStream(
    val url: String,
    val mimeType: String,
    val itag: Int,
    val bitrate: Int,
    val durationMs: Long?,
    val clientName: String,
    /** User-Agent del client YouTube che ha dato l'URL: va usato anche per scaricare l'audio. */
    val userAgent: String,
)

/**
 * Trova l'URL audio di un brano chiedendo la risposta "player" a YouTube.
 *
 * Prova prima i client che danno URL diretti (ANDROID_VR e IOS), che funzionano
 * anche su iPhone senza decifrare la firma.
 */
object StreamResolver {
    private val clients: List<YouTubeClient>
        get() = listOf(YouTubeClient.ANDROID_VR_1_65_10, YouTubeClient.IOS)

    suspend fun resolve(videoId: String): Result<ResolvedStream> {
        ensureVisitorData()

        val failures = mutableListOf<String>()
        for (client in clients) {
            try {
                val playerResult = YouTube.player(videoId = videoId, client = client, setLogin = false)
                val response = playerResult.getOrNull()
                if (response == null) {
                    failures += "${client.clientName}: ${playerResult.exceptionOrNull()?.message}"
                    continue
                }

                if (response.playabilityStatus.status != "OK") {
                    failures += "${client.clientName}: ${response.playabilityStatus.status} ${response.playabilityStatus.reason.orEmpty()}"
                    continue
                }

                val format = pickAudioFormat(response)
                if (format == null) {
                    failures += "${client.clientName}: nessun formato audio compatibile"
                    continue
                }

                val urlResult = StreamDecipher.getStreamUrl(format, videoId, client)
                val url = urlResult.getOrNull()
                if (url == null) {
                    failures += "${client.clientName}: ${urlResult.exceptionOrNull()?.message}"
                    continue
                }

                return Result.success(
                    ResolvedStream(
                        url = url,
                        mimeType = format.mimeType,
                        itag = format.itag,
                        bitrate = format.bitrate,
                        durationMs = format.approxDurationMs?.toLongOrNull(),
                        clientName = client.clientName,
                        userAgent = client.userAgent,
                    ),
                )
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                failures += "${client.clientName}: ${error.message}"
            }
        }
        return Result.failure(IllegalStateException("Impossibile riprodurre il brano.\n" + failures.joinToString("\n")))
    }

    /** Il formato solo-audio con il bitrate più alto tra quelli che la piattaforma sa leggere. */
    private fun pickAudioFormat(response: PlayerResponse): PlayerResponse.StreamingData.Format? =
        response.streamingData
            ?.adaptiveFormats
            .orEmpty()
            .filter { it.isAudio && it.mimeType.startsWith("audio/") && canPlayMimeType(it.mimeType) }
            .maxByOrNull { it.bitrate }

    /** Senza visitorData YouTube risponde spesso "LOGIN_REQUIRED" anche per i brani pubblici. */
    private suspend fun ensureVisitorData() {
        if (!YouTube.visitorData.isNullOrBlank()) return
        YouTube.visitorData().onSuccess { YouTube.visitorData = it }
    }
}
