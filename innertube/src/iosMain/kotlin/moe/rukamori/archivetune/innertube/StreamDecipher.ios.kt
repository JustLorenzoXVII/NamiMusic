package moe.rukamori.archivetune.innertube

import kotlinx.coroutines.CancellationException
import moe.rukamori.archivetune.innertube.models.YouTubeClient
import moe.rukamori.archivetune.innertube.models.response.PlayerResponse

/**
 * Versione iOS: per ora accetta solo gli URL diretti, quelli che danno i client
 * IOS e ANDROID_VR senza firma cifrata. I formati con signatureCipher falliscono,
 * così il lettore passa al client successivo.
 *
 * TODO: decifrare firma e parametro "n" con JavaScriptCore, come fa NewPipe su Android.
 */
actual object StreamDecipher {
    actual suspend fun getSignatureTimestamp(videoId: String): Result<Int> =
        Result.failure(UnsupportedOperationException("Signature timestamp non ancora disponibile su iOS"))

    actual suspend fun getStreamUrl(
        format: PlayerResponse.StreamingData.Format,
        videoId: String,
        client: YouTubeClient?,
        authState: PlaybackAuthState,
    ): Result<String> {
        try {
            val directUrl =
                format.url
                    ?: return Result.failure(
                        UnsupportedOperationException("Formato con firma cifrata: non ancora supportato su iOS"),
                    )
            return Result.success(
                YouTube.appendGvsPoToken(
                    url = directUrl,
                    client = client,
                    videoId = videoId,
                    authState = authState,
                ),
            )
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Exception) {
            return Result.failure(error)
        }
    }
}
