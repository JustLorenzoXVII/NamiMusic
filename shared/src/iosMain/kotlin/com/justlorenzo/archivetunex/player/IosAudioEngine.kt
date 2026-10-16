package com.justlorenzo.archivetunex.player

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.readValue
import kotlinx.cinterop.useContents
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import platform.AVFAudio.*
import platform.AVFoundation.*
import platform.CoreMedia.*
import platform.Foundation.*
import platform.MediaPlayer.*
import platform.UIKit.*

actual fun createAudioEngine(): AudioEngine = IosAudioEngine()

/** AVPlayer legge AAC in contenitore MP4 (m4a), non Opus/WebM. */
actual fun canPlayMimeType(mimeType: String): Boolean = mimeType.startsWith("audio/mp4")

actual fun preferredAudioItag(): Int? = 140

/**
 * Motore audio iPhone: AVPlayer, con audio in background, schermata di blocco
 * (Now Playing) e comandi da cuffie / Centro di Controllo.
 */
@OptIn(ExperimentalForeignApi::class)
internal class IosAudioEngine : AudioEngine {
    override var listener: AudioEngine.Listener? = null

    private val player = AVPlayer()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var stateJob: Job? = null
    private var artworkJob: Job? = null

    private var info: NowPlayingInfo? = null
    private var artwork: MPMediaItemArtwork? = null
    private var lastIsPlaying = false
    private var lastIsBuffering = false
    private var errorReported = false

    init {
        val session = AVAudioSession.sharedInstance()
        session.setCategory(AVAudioSessionCategoryPlayback, null)
        session.setActive(true, null)

        NSNotificationCenter.defaultCenter.addObserverForName(
            name = AVPlayerItemDidPlayToEndTimeNotification,
            `object` = null,
            queue = NSOperationQueue.mainQueue,
        ) { _ ->
            updateNowPlaying()
            listener?.onEnded()
        }

        setUpRemoteCommands()
    }

    override val positionMs: Long
        get() {
            val seconds = CMTimeGetSeconds(player.currentTime())
            return if (seconds.isNaN() || seconds < 0) 0 else (seconds * 1000).toLong()
        }

    override val durationMs: Long?
        get() {
            val item = player.currentItem ?: return null
            val seconds = CMTimeGetSeconds(item.duration)
            return if (seconds.isNaN() || seconds <= 0) null else (seconds * 1000).toLong()
        }

    override fun load(
        url: String,
        headers: Map<String, String>,
        info: NowPlayingInfo,
        playWhenReady: Boolean,
    ) {
        val nsUrl = NSURL.URLWithString(url)
        if (nsUrl == null) {
            listener?.onError("URL non valido")
            return
        }
        this.info = info
        artwork = null
        errorReported = false
        // "AVURLAssetHTTPHeaderFieldsKey": chiave non documentata ma usata da tutti per gli header HTTP
        val asset =
            AVURLAsset(
                uRL = nsUrl,
                // Il Range lo gestisce AVPlayer da solo.
                options = mapOf<Any?, Any?>("AVURLAssetHTTPHeaderFieldsKey" to headers.filterKeys { !it.equals("Range", ignoreCase = true) }),
            )
        player.replaceCurrentItemWithPlayerItem(AVPlayerItem(asset = asset))
        if (playWhenReady) player.play()
        startStateUpdates()
        updateNowPlaying()
        loadArtwork(info.artworkUrl)
    }

    override val bufferedPositionMs: Long
        get() {
            val item = player.currentItem ?: return 0
            var end = 0.0
            item.loadedTimeRanges.forEach { value ->
                val range = (value as NSValue).CMTimeRangeValue
                range.useContents {
                    val rangeEnd = CMTimeGetSeconds(start.readValue()) + CMTimeGetSeconds(duration.readValue())
                    if (!rangeEnd.isNaN() && rangeEnd > end) end = rangeEnd
                }
            }
            return (end * 1000).toLong()
        }

    override var volume: Float
        get() = player.volume
        set(value) {
            player.volume = value
        }

    override var speed: Float = 1f
        set(value) {
            field = value
            if (player.rate > 0f) player.rate = value
        }

    override fun play() {
        player.playImmediatelyAtRate(speed)
        updateNowPlaying()
    }

    override fun pause() {
        player.pause()
        updateNowPlaying()
    }

    override fun seekTo(positionMs: Long) {
        player.seekToTime(CMTimeMakeWithSeconds(positionMs / 1000.0, 1000))
        updateNowPlaying()
    }

    override fun stop() {
        player.pause()
        player.replaceCurrentItemWithPlayerItem(null)
    }

    /** AVPlayer non ha un listener semplice come ExoPlayer: si controlla lo stato ogni 300 ms. */
    private fun startStateUpdates() {
        if (stateJob?.isActive == true) return
        stateJob =
            scope.launch {
                while (isActive) {
                    checkState()
                    delay(300)
                }
            }
    }

    private fun checkState() {
        val item = player.currentItem
        if (item != null && item.status == AVPlayerItemStatusFailed && !errorReported) {
            errorReported = true
            listener?.onError(item.error?.localizedDescription ?: "Errore di riproduzione")
        }

        val status = player.timeControlStatus
        val isPlaying = status == AVPlayerTimeControlStatusPlaying
        val isBuffering = status == AVPlayerTimeControlStatusWaitingToPlayAtSpecifiedRate
        if (isPlaying != lastIsPlaying) {
            lastIsPlaying = isPlaying
            listener?.onIsPlayingChanged(isPlaying)
            updateNowPlaying()
        }
        if (isBuffering != lastIsBuffering) {
            lastIsBuffering = isBuffering
            listener?.onBufferingChanged(isBuffering)
        }
    }

    private fun updateNowPlaying() {
        val current = info ?: return
        val nowPlaying = mutableMapOf<Any?, Any?>()
        nowPlaying[MPMediaItemPropertyTitle] = current.title
        nowPlaying[MPMediaItemPropertyArtist] = current.artist
        (durationMs ?: current.durationMs)?.let { nowPlaying[MPMediaItemPropertyPlaybackDuration] = it / 1000.0 }
        nowPlaying[MPNowPlayingInfoPropertyElapsedPlaybackTime] = positionMs / 1000.0
        nowPlaying[MPNowPlayingInfoPropertyPlaybackRate] = player.rate.toDouble()
        artwork?.let { nowPlaying[MPMediaItemPropertyArtwork] = it }
        MPNowPlayingInfoCenter.defaultCenter().nowPlayingInfo = nowPlaying
    }

    private fun loadArtwork(url: String?) {
        artworkJob?.cancel()
        if (url.isNullOrBlank()) return
        artworkJob =
            scope.launch {
                val image =
                    withContext(Dispatchers.Default) {
                        val nsUrl = NSURL.URLWithString(url) ?: return@withContext null
                        val data = NSData.dataWithContentsOfURL(nsUrl) ?: return@withContext null
                        UIImage.imageWithData(data)
                    } ?: return@launch
                val size = image.size
                artwork = MPMediaItemArtwork(boundsSize = size) { _ -> image }
                updateNowPlaying()
            }
    }

    private fun setUpRemoteCommands() {
        val center = MPRemoteCommandCenter.sharedCommandCenter()
        center.playCommand.addTargetWithHandler { _ ->
            listener?.onRemotePlay() ?: play()
            MPRemoteCommandHandlerStatusSuccess
        }
        center.pauseCommand.addTargetWithHandler { _ ->
            listener?.onRemotePause() ?: pause()
            MPRemoteCommandHandlerStatusSuccess
        }
        center.togglePlayPauseCommand.addTargetWithHandler { _ ->
            if (player.rate > 0f) {
                listener?.onRemotePause() ?: pause()
            } else {
                listener?.onRemotePlay() ?: play()
            }
            MPRemoteCommandHandlerStatusSuccess
        }
        center.nextTrackCommand.addTargetWithHandler { _ ->
            listener?.onRemoteNext()
            MPRemoteCommandHandlerStatusSuccess
        }
        center.previousTrackCommand.addTargetWithHandler { _ ->
            listener?.onRemotePrevious()
            MPRemoteCommandHandlerStatusSuccess
        }
        center.changePlaybackPositionCommand.addTargetWithHandler { event ->
            val positionEvent = event as? MPChangePlaybackPositionCommandEvent
            if (positionEvent == null) {
                MPRemoteCommandHandlerStatusCommandFailed
            } else {
                val target = (positionEvent.positionTime * 1000).toLong()
                listener?.onRemoteSeek(target) ?: seekTo(target)
                MPRemoteCommandHandlerStatusSuccess
            }
        }
    }
}
