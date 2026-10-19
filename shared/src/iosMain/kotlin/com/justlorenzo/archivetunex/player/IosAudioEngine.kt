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
 * (Now Playing), comandi da cuffie / auto / Centro di Controllo, interruzioni (chiamate, Siri)
 * e cambi di uscita (Bluetooth che si scollega).
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

    // Ultimo stato comunicato all'app: si azzera a ogni brano, così il nuovo stato arriva sempre
    private var lastIsPlaying = false
    private var lastIsBuffering = false
    private var errorReported = false

    /** Il brano attuale ha già segnalato la fine (si segnala una volta sola). */
    private var endReported = false

    /** L'app vuole che suoni (play chiesto e non ancora messo in pausa). */
    private var wantsPlay = false

    /** Controlli di fila in cui il brano risulta fermo alla fine senza che sia arrivato l'evento. */
    private var stuckAtEndChecks = 0

    /** Prima dell'interruzione (chiamata, Siri) stava suonando: alla fine si riprende. */
    private var resumeAfterInterruption = false

    /** Tempo extra chiesto a iOS tra un brano e l'altro, per non essere sospesi in background. */
    private var backgroundTask: UIBackgroundTaskIdentifier = UIBackgroundTaskInvalid
    private var backgroundTaskJob: Job? = null

    init {
        val session = AVAudioSession.sharedInstance()
        session.setCategory(AVAudioSessionCategoryPlayback, null)
        session.setActive(true, null)

        val center = NSNotificationCenter.defaultCenter
        // Fine del brano: conta solo quella del brano in riproduzione (non di quelli vecchi)
        center.addObserverForName(
            name = AVPlayerItemDidPlayToEndTimeNotification,
            `object` = null,
            queue = NSOperationQueue.mainQueue,
        ) { note ->
            val item = note?.`object` as? AVPlayerItem
            if (item != null && item === player.currentItem) reportEnded()
        }
        // Il brano non è riuscito ad arrivare alla fine (rete persa, stream chiuso)
        center.addObserverForName(
            name = AVPlayerItemFailedToPlayToEndTimeNotification,
            `object` = null,
            queue = NSOperationQueue.mainQueue,
        ) { note ->
            val item = note?.`object` as? AVPlayerItem
            if (item != null && item === player.currentItem && !errorReported) {
                errorReported = true
                val error = note.userInfo?.get(AVPlayerItemFailedToPlayToEndTimeErrorKey) as? NSError
                listener?.onError(error?.localizedDescription ?: "Riproduzione interrotta")
            }
        }
        // Chiamate, Siri, navigatore dell'auto: pausa e, se iOS lo consente, si riprende dopo
        center.addObserverForName(
            name = AVAudioSessionInterruptionNotification,
            `object` = null,
            queue = NSOperationQueue.mainQueue,
        ) { note -> handleInterruption(note) }
        // Cuffie o Bluetooth scollegati: pausa, come fanno tutte le app musicali
        center.addObserverForName(
            name = AVAudioSessionRouteChangeNotification,
            `object` = null,
            queue = NSOperationQueue.mainQueue,
        ) { note ->
            val reason = (note?.userInfo?.get(AVAudioSessionRouteChangeReasonKey) as? NSNumber)?.unsignedLongValue
            if (reason == AVAudioSessionRouteChangeReasonOldDeviceUnavailable) {
                listener?.onRemotePause() ?: pause()
            }
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
        val nsUrl = if (url.startsWith("file://")) NSURL.fileURLWithPath(url.removePrefix("file://")) else NSURL.URLWithString(url)
        if (nsUrl == null) {
            listener?.onError("URL non valido")
            return
        }
        this.info = info
        artwork = null
        errorReported = false
        endReported = false
        stuckAtEndChecks = 0
        // Nuovo brano: lo stato ripartirà da "fermo", così ogni cambio viene comunicato
        lastIsPlaying = false
        lastIsBuffering = false
        // "AVURLAssetHTTPHeaderFieldsKey": chiave non documentata ma usata da tutti per gli header HTTP
        val asset =
            AVURLAsset(
                uRL = nsUrl,
                // Il Range lo gestisce AVPlayer da solo.
                options = mapOf<Any?, Any?>("AVURLAssetHTTPHeaderFieldsKey" to headers.filterKeys { !it.equals("Range", ignoreCase = true) }),
            )
        player.replaceCurrentItemWithPlayerItem(AVPlayerItem(asset = asset))
        if (playWhenReady) play()
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
        wantsPlay = true
        // Dopo un'interruzione la sessione audio può essere stata disattivata
        AVAudioSession.sharedInstance().setActive(true, null)
        player.playImmediatelyAtRate(speed)
        updateNowPlaying()
    }

    override fun pause() {
        wantsPlay = false
        player.pause()
        updateNowPlaying()
    }

    override fun seekTo(positionMs: Long) {
        stuckAtEndChecks = 0
        // Tornando indietro dopo la fine, il brano può finire di nuovo
        if (endReported && positionMs < (limitMs() ?: Long.MAX_VALUE) - 2_000) endReported = false
        player.seekToTime(CMTimeMakeWithSeconds(positionMs / 1000.0, 1000))
        updateNowPlaying()
    }

    override fun stop() {
        wantsPlay = false
        player.pause()
        player.replaceCurrentItemWithPlayerItem(null)
        lastIsPlaying = false
        lastIsBuffering = false
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

    /** Durata del brano: quella di YouTube (esatta) o, se manca, quella stimata da AVPlayer. */
    private fun limitMs(): Long? = info?.durationMs?.takeIf { it > 0 } ?: durationMs

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
            if (isPlaying) endBackgroundTask()
        }
        if (isBuffering != lastIsBuffering) {
            lastIsBuffering = isBuffering
            listener?.onBufferingChanged(isBuffering)
        }

        // Rete di sicurezza: se l'evento di fine non arriva, la fine si riconosce dalla posizione
        if (item != null && !endReported && item.status == AVPlayerItemStatusReadyToPlay) {
            val limit = limitMs()
            val position = positionMs
            if (limit != null && limit > 0) {
                when {
                    // Il tempo va oltre la fine del brano: è finito
                    position > limit + 2_000 -> reportEnded()
                    // Fermo all'ultimo secondo mentre dovrebbe suonare: dopo ~1 s si considera finito
                    wantsPlay && !isPlaying && position >= limit - 1_000 -> {
                        stuckAtEndChecks++
                        if (stuckAtEndChecks >= 3) reportEnded()
                    }
                    else -> stuckAtEndChecks = 0
                }
            }
        }
    }

    private fun reportEnded() {
        if (endReported) return
        endReported = true
        stuckAtEndChecks = 0
        // Tra un brano e il successivo l'audio si ferma: si chiede a iOS un po' di tempo,
        // così in background (schermo spento, in auto) l'app non viene sospesa prima di farlo partire
        beginBackgroundTask()
        lastIsPlaying = false
        updateNowPlaying()
        listener?.onEnded()
    }

    private fun beginBackgroundTask() {
        endBackgroundTask()
        val app = UIApplication.sharedApplication
        backgroundTask = app.beginBackgroundTaskWithName("Nami-next-track") { endBackgroundTask() }
        // Al massimo 30 secondi, poi lo si restituisce comunque
        backgroundTaskJob =
            scope.launch {
                delay(30_000)
                endBackgroundTask()
            }
    }

    private fun endBackgroundTask() {
        backgroundTaskJob?.cancel()
        backgroundTaskJob = null
        if (backgroundTask != UIBackgroundTaskInvalid) {
            UIApplication.sharedApplication.endBackgroundTask(backgroundTask)
            backgroundTask = UIBackgroundTaskInvalid
        }
    }

    private fun handleInterruption(note: NSNotification?) {
        val userInfo = note?.userInfo ?: return
        val type = (userInfo[AVAudioSessionInterruptionTypeKey] as? NSNumber)?.unsignedLongValue ?: return
        if (type == AVAudioSessionInterruptionTypeBegan) {
            // iOS ha già fermato l'audio: lo si dice all'app, ricordando se stava suonando
            resumeAfterInterruption = wantsPlay
            listener?.onRemotePause() ?: pause()
        } else if (type == AVAudioSessionInterruptionTypeEnded) {
            val options = (userInfo[AVAudioSessionInterruptionOptionKey] as? NSNumber)?.unsignedLongValue ?: 0uL
            val shouldResume = (options and AVAudioSessionInterruptionOptionShouldResume) != 0uL
            if (resumeAfterInterruption && shouldResume) listener?.onRemotePlay() ?: play()
            resumeAfterInterruption = false
        }
    }

    private fun updateNowPlaying() {
        val current = info ?: return
        val nowPlaying = mutableMapOf<Any?, Any?>()
        nowPlaying[MPMediaItemPropertyTitle] = current.title
        nowPlaying[MPMediaItemPropertyArtist] = current.artist
        (current.durationMs ?: durationMs)?.let { nowPlaying[MPMediaItemPropertyPlaybackDuration] = it / 1000.0 }
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
            // Si decide in base a cosa l'app vuole (non a rate, che è 0 anche mentre carica)
            if (wantsPlay) {
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
