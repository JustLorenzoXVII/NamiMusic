package com.justlorenzo.archivetunex.player

/** Dati del brano mostrati nella schermata di blocco / notifica. */
data class NowPlayingInfo(
    val title: String,
    val artist: String,
    val artworkUrl: String?,
    val durationMs: Long?,
    /** Album (schermata di blocco, display dell'auto). */
    val album: String? = null,
)

/**
 * Il "motore" audio della piattaforma: ExoPlayer (Media3) su Android, AVPlayer su iPhone.
 * Riproduce un solo brano alla volta; la coda la gestisce [PlayerController] nel codice comune.
 */
interface AudioEngine {
    interface Listener {
        fun onIsPlayingChanged(isPlaying: Boolean)

        fun onBufferingChanged(isBuffering: Boolean)

        fun onEnded()

        fun onError(message: String)

        /** Comandi dalla notifica, dalla schermata di blocco o dalle cuffie. */
        fun onRemoteNext()

        fun onRemotePrevious()

        /** Play/pausa/posizione dalla schermata di blocco, dalla notifica o dalle cuffie. */
        fun onRemotePlay() {}

        fun onRemotePause() {}

        fun onRemoteSeek(positionMs: Long) {}
    }

    var listener: Listener?

    /** Posizione corrente in millisecondi. */
    val positionMs: Long

    /** Durata del brano in millisecondi, se nota. */
    val durationMs: Long?

    /**
     * @param headers intestazioni HTTP per lo stream: YouTube rifiuta (403) le richieste
     * se lo User-Agent non corrisponde al client che ha chiesto l'URL.
     */
    fun load(
        url: String,
        headers: Map<String, String>,
        info: NowPlayingInfo,
        playWhenReady: Boolean,
    )

    /** Fin dove è già stato scaricato il brano, in millisecondi. */
    val bufferedPositionMs: Long

    /** Volume 0..1. */
    var volume: Float

    /** Velocità di riproduzione (1 = normale). */
    var speed: Float

    fun play()

    fun pause()

    fun seekTo(positionMs: Long)

    fun stop()

    /**
     * Prepara in anticipo il brano successivo (iPhone: lo mette in coda ad AVQueuePlayer, che inizia
     * a scaricarlo e lo fa partire senza pause alla fine di quello attuale).
     */
    fun preload(
        url: String,
        headers: Map<String, String>,
        info: NowPlayingInfo,
    ) {}

    /** Il brano preparato non è più quello giusto (coda cambiata, ripeti brano…). */
    fun cancelPreload() {}

    /** true se [url] è il brano preparato con [preload] (allora [load] lo usa al volo). */
    fun hasPreloaded(url: String): Boolean = false
}

/** Crea il motore audio della piattaforma. */
expect fun createAudioEngine(): AudioEngine

/**
 * Formati audio che la piattaforma sa riprodurre.
 * Android legge sia Opus (WebM) sia AAC (MP4); AVPlayer su iPhone solo AAC (MP4).
 */
expect fun canPlayMimeType(mimeType: String): Boolean

/**
 * Formato audio da chiedere a youtubei.js (null = il migliore).
 * Su iPhone 140 = AAC 128 kbps in MP4, perché AVPlayer non legge Opus/WebM.
 */
expect fun preferredAudioItag(): Int?
