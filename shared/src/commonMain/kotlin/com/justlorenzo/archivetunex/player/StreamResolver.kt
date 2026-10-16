package com.justlorenzo.archivetunex.player

import kotlinx.coroutines.CancellationException
import moe.rukamori.archivetune.innertube.StreamDecipher
import moe.rukamori.archivetune.innertube.StreamProbe
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
    /** googlevideo accetta lo stream solo se la richiesta ha l'intestazione Range. */
    val requiresRangeHeader: Boolean,
    /** Esito delle prove, per capire i problemi. */
    val diagnostics: String,
)

/**
 * Trova l'URL audio di un brano chiedendo la risposta "player" a YouTube.
 *
 * Prova diversi client che non richiedono PoToken né login. Per ognuno verifica
 * che googlevideo accetti davvero l'URL (prova da 2 byte): se risponde 403 passa al successivo.
 * In caso di fallimento l'errore elenca cosa ha risposto ogni client.
 */
object StreamResolver {
    private val clients: List<YouTubeClient>
        get() =
            listOf(
                YouTubeClient.ANDROID_VR_1_65_10,
                YouTubeClient.ANDROID_VR_1_61_48,
                YouTubeClient.ANDROID_VR_1_43_32,
                YouTubeClient.ANDROID_VR_NO_AUTH,
                YouTubeClient.IOS,
                YouTubeClient.IPADOS,
                YouTubeClient.VISIONOS,
                YouTubeClient.TVHTML5_SIMPLY,
                YouTubeClient.ANDROID_TESTSUITE,
                YouTubeClient.IOS_MUSIC,
            )

    suspend fun resolve(videoId: String): Result<ResolvedStream> {
        ensureVisitorData()

        val report = mutableListOf<String>()
        for (client in clients) {
            val label = "${client.clientName} ${client.clientVersion}"
            try {
                val playerResult = YouTube.player(videoId = videoId, client = client, setLogin = false)
                val response = playerResult.getOrNull()
                if (response == null) {
                    report += "$label: ${playerResult.exceptionOrNull()?.message?.take(80)}"
                    continue
                }
                if (response.playabilityStatus.status != "OK") {
                    report += "$label: ${response.playabilityStatus.status} ${response.playabilityStatus.reason.orEmpty().take(60)}"
                    continue
                }

                val format = pickAudioFormat(response)
                if (format == null) {
                    report += "$label: nessun formato audio compatibile"
                    continue
                }

                // URL da provare: quello elaborato (firma / parametro "n") e quello originale.
                val candidates = linkedSetOf<String>()
                StreamDecipher.getStreamUrl(format, videoId, client).getOrNull()?.let(candidates::add)
                format.url?.let(candidates::add)
                if (candidates.isEmpty()) {
                    report += "$label: URL cifrato, non decifrabile"
                    continue
                }

                for (url in candidates) {
                    val status = StreamProbe.status(url, client.userAgent)
                    val statusNoRange = StreamProbe.statusWithoutRange(url, client.userAgent)
                    report += "$label: stream HTTP $status (senza Range: $statusNoRange)"
                    if (status in 200..299 || statusNoRange in 200..299) {
                        return Result.success(
                            ResolvedStream(
                                url = url,
                                mimeType = format.mimeType,
                                itag = format.itag,
                                bitrate = format.bitrate,
                                durationMs = format.approxDurationMs?.toLongOrNull(),
                                clientName = client.clientName,
                                userAgent = client.userAgent,
                                requiresRangeHeader = statusNoRange !in 200..299,
                                diagnostics = report.joinToString("\n"),
                            ),
                        )
                    }
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                report += "$label: ${error.message?.take(80)}"
            }
        }
        return Result.failure(IllegalStateException("Impossibile riprodurre il brano.\n" + report.joinToString("\n")))
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
