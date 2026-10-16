package com.justlorenzo.archivetunex.player

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import androidx.core.net.toUri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.HttpDataSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture

/** Contesto dell'app, impostato da MainActivity con [initArchiveTuneX]. */
@SuppressLint("StaticFieldLeak")
internal object AndroidAppContext {
    lateinit var context: Context
}

/** Da chiamare in MainActivity prima di mostrare l'interfaccia. */
fun initArchiveTuneX(context: Context) {
    AndroidAppContext.context = context.applicationContext
}

actual fun createAudioEngine(): AudioEngine = AndroidAudioEngine(AndroidAppContext.context)

actual fun canPlayMimeType(mimeType: String): Boolean = true

actual fun preferredAudioItag(): Int? = null

/**
 * Motore audio Android: ExoPlayer (Media3).
 * La notifica e i controlli di sistema li gestisce [PlaybackService] con una MediaSession.
 */
internal class AndroidAudioEngine(
    private val context: Context,
) : AudioEngine {
    override var listener: AudioEngine.Listener? = null

    private val exoPlayer: ExoPlayer =
        ExoPlayer
            .Builder(context)
            .setAudioAttributes(
                AudioAttributes
                    .Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                true,
            ).setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build()

    /**
     * Il player dato alla MediaSession: è ExoPlayer, ma i tasti "successivo" e
     * "precedente" della notifica passano per la coda comune.
     */
    val sessionPlayer: Player =
        object : ForwardingPlayer(exoPlayer) {
            override fun getAvailableCommands(): Player.Commands =
                super
                    .getAvailableCommands()
                    .buildUpon()
                    .add(Player.COMMAND_SEEK_TO_NEXT)
                    .add(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
                    .add(Player.COMMAND_SEEK_TO_PREVIOUS)
                    .add(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
                    .build()

            override fun isCommandAvailable(command: Int): Boolean =
                when (command) {
                    Player.COMMAND_SEEK_TO_NEXT,
                    Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM,
                    Player.COMMAND_SEEK_TO_PREVIOUS,
                    Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM,
                    -> true
                    else -> super.isCommandAvailable(command)
                }

            override fun seekToNext() {
                listener?.onRemoteNext()
            }

            override fun seekToNextMediaItem() {
                listener?.onRemoteNext()
            }

            override fun seekToPrevious() {
                listener?.onRemotePrevious()
            }

            override fun seekToPreviousMediaItem() {
                listener?.onRemotePrevious()
            }

            override fun play() {
                listener?.onRemotePlay() ?: super.play()
            }

            override fun pause() {
                listener?.onRemotePause() ?: super.pause()
            }

            override fun setPlayWhenReady(playWhenReady: Boolean) {
                if (playWhenReady) play() else pause()
            }

            override fun seekTo(positionMs: Long) {
                listener?.onRemoteSeek(positionMs) ?: super.seekTo(positionMs)
            }
        }

    private var controllerFuture: ListenableFuture<MediaController>? = null

    init {
        PlaybackService.sessionPlayer = sessionPlayer
        exoPlayer.addListener(
            object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    listener?.onIsPlayingChanged(isPlaying)
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    when (playbackState) {
                        Player.STATE_BUFFERING -> listener?.onBufferingChanged(true)
                        Player.STATE_READY -> listener?.onBufferingChanged(false)
                        Player.STATE_ENDED -> listener?.onEnded()
                        else -> Unit
                    }
                }

                override fun onPlayerError(error: PlaybackException) {
                    listener?.onError(error.describe())
                }
            },
        )
    }

    override val positionMs: Long
        get() = exoPlayer.currentPosition

    override val durationMs: Long?
        get() = exoPlayer.duration.takeIf { it != C.TIME_UNSET && it > 0 }

    override fun load(
        url: String,
        headers: Map<String, String>,
        info: NowPlayingInfo,
        playWhenReady: Boolean,
    ) {
        val metadata =
            MediaMetadata
                .Builder()
                .setTitle(info.title)
                .setArtist(info.artist)
                .apply { info.artworkUrl?.let { setArtworkUri(it.toUri()) } }
                .build()
        val mediaItem =
            MediaItem
                .Builder()
                .setUri(url)
                .setMediaMetadata(metadata)
                .build()
        val dataSourceFactory =
            DefaultHttpDataSource
                .Factory()
                .setAllowCrossProtocolRedirects(true)
                .setConnectTimeoutMs(15_000)
                .setReadTimeoutMs(30_000)
                .apply {
                    headers.entries.firstOrNull { it.key.equals("User-Agent", ignoreCase = true) }?.let { setUserAgent(it.value) }
                    setDefaultRequestProperties(headers.filterKeys { !it.equals("User-Agent", ignoreCase = true) })
                }
        exoPlayer.setMediaSource(ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(mediaItem))
        exoPlayer.prepare()
        exoPlayer.playWhenReady = playWhenReady
        connectToService()
    }

    override val bufferedPositionMs: Long
        get() = exoPlayer.bufferedPosition

    override var volume: Float
        get() = exoPlayer.volume
        set(value) {
            exoPlayer.volume = value
        }

    override var speed: Float
        get() = exoPlayer.playbackParameters.speed
        set(value) {
            exoPlayer.setPlaybackSpeed(value)
        }

    override fun play() {
        if (exoPlayer.playbackState == Player.STATE_ENDED) exoPlayer.seekTo(0)
        exoPlayer.play()
    }

    override fun pause() = exoPlayer.pause()

    override fun seekTo(positionMs: Long) = exoPlayer.seekTo(positionMs)

    override fun stop() = exoPlayer.stop()

    /** Collegarsi al servizio lo avvia: da lì Media3 mostra la notifica e tiene viva la musica in background. */
    private fun connectToService() {
        if (controllerFuture != null) return
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        controllerFuture = MediaController.Builder(context, token).buildAsync()
    }
}

/** Messaggio leggibile, con il codice HTTP se YouTube ha rifiutato lo stream (es. 403). */
private fun PlaybackException.describe(): String {
    var cause: Throwable? = this
    while (cause != null) {
        if (cause is HttpDataSource.InvalidResponseCodeException) {
            return "Errore stream: HTTP ${cause.responseCode} ${cause.responseMessage.orEmpty()}".trim()
        }
        cause = cause.cause
    }
    return listOfNotNull(errorCodeName, cause?.message ?: this.cause?.message).joinToString(": ")
}
