package moe.rukamori.archivetune.innertube

import moe.rukamori.archivetune.innertube.models.YouTubeClient
import moe.rukamori.archivetune.innertube.models.response.PlayerResponse

/**
 * Trasforma un formato audio della risposta "player" in un URL riproducibile:
 * decifra la firma (signatureCipher) e il parametro "n" anti-rallentamento.
 *
 * Android: usa NewPipe Extractor (come ArchiveTune originale, ex NewPipeUtils).
 * iOS: per ora gestisce solo gli URL diretti (client IOS / ANDROID_VR), la decifratura
 * con JavaScriptCore arriverà in un passo successivo.
 */
expect object StreamDecipher {
    suspend fun getSignatureTimestamp(videoId: String): Result<Int>

    suspend fun getStreamUrl(
        format: PlayerResponse.StreamingData.Format,
        videoId: String,
        client: YouTubeClient? = null,
        authState: PlaybackAuthState = YouTube.currentPlaybackAuthState(),
    ): Result<String>
}
