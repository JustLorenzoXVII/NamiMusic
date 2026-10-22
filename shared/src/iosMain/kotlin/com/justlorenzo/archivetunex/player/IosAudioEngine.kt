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
import platform.CoreGraphics.*
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

    /**
     * AVQueuePlayer: oltre al brano attuale tiene in coda il successivo ([preload]), che inizia a
     * scaricarsi prima della fine e parte senza pause (come le app di streaming).
     */
    private val player = AVQueuePlayer()

    /** Il brano attualmente caricato (per riconoscere la sua fine anche dopo il passaggio automatico). */
    private var currentItem: AVPlayerItem? = null

    /** Il brano successivo, già in coda nel player. */
    private var preloadedItem: AVPlayerItem? = null
    private var preloadedUrl: String? = null
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

    /** Copertine già scaricate per la schermata di blocco (cambio brano senza attese). */
    private val artworkCache = LinkedHashMap<String, MPMediaItemArtwork>()
    private var artworkUrl: String? = null
    private var nowPlayingTicks = 0

    init {
        // Parte appena ha abbastanza dati, senza aspettare di accumulare buffer (più reattivo come le app di streaming)
        player.automaticallyWaitsToMinimizeStalling = false
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
            if (item != null && item === currentItem) reportEnded()
        }
        // Il brano non è riuscito ad arrivare alla fine (rete persa, stream chiuso)
        center.addObserverForName(
            name = AVPlayerItemFailedToPlayToEndTimeNotification,
            `object` = null,
            queue = NSOperationQueue.mainQueue,
        ) { note ->
            val item = note?.`object` as? AVPlayerItem
            if (item != null && item === currentItem && !errorReported) {
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
        // Copertina già in memoria: compare subito sulla schermata di blocco
        artwork = info.artworkUrl?.let { artworkCache[it] }
        val wasPreloaded = preloadedItem?.takeIf { preloadedUrl == url }
        preloadedItem = null
        preloadedUrl = null
        errorReported = false
        endReported = false
        stuckAtEndChecks = 0
        // Nuovo brano: lo stato ripartirà da "fermo", così ogni cambio viene comunicato
        lastIsPlaying = false
        lastIsBuffering = false
        // "AVURLAssetHTTPHeaderFieldsKey": chiave non documentata ma usata da tutti per gli header HTTP
        if (wasPreloaded != null) {
            // Il brano era già in coda: se AVQueuePlayer non ci è ancora passato da solo, ci si va ora
            if (player.currentItem !== wasPreloaded) {
                if ((player.items() as List<*>).any { it === wasPreloaded }) {
                    while (player.currentItem != null && player.currentItem !== wasPreloaded) player.advanceToNextItem()
                } else {
                    player.removeAllItems()
                    player.insertItem(wasPreloaded, afterItem = null)
                }
            }
            currentItem = wasPreloaded
            player.actionAtItemEnd = AVPlayerActionAtItemEndPause
        } else {
            val item = makeItem(nsUrl, headers)
            player.removeAllItems()
            player.insertItem(item, afterItem = null)
            currentItem = item
            player.actionAtItemEnd = AVPlayerActionAtItemEndPause
        }
        if (playWhenReady || (wasPreloaded != null && wantsPlay)) play()
        startStateUpdates()
        updateNowPlaying()
        loadArtwork(info.artworkUrl)
    }

    private fun makeItem(
        nsUrl: NSURL,
        headers: Map<String, String>,
    ): AVPlayerItem {
        // "AVURLAssetHTTPHeaderFieldsKey": chiave non documentata ma usata da tutti per gli header HTTP
        val asset =
            AVURLAsset(
                uRL = nsUrl,
                // Il Range lo gestisce AVPlayer da solo.
                options = mapOf<Any?, Any?>("AVURLAssetHTTPHeaderFieldsKey" to headers.filterKeys { !it.equals("Range", ignoreCase = true) }),
            )
        return AVPlayerItem(asset = asset)
    }

    override fun preload(
        url: String,
        headers: Map<String, String>,
        info: NowPlayingInfo,
    ) {
        // Copertina del prossimo brano già pronta: in auto compare insieme al titolo
        prefetchArtwork(info.artworkUrl)
        if (preloadedUrl == url && preloadedItem != null) return
        cancelPreload()
        val current = currentItem ?: return
        if (player.currentItem !== current) return
        val nsUrl = (if (url.startsWith("file://")) NSURL.fileURLWithPath(url.removePrefix("file://")) else NSURL.URLWithString(url)) ?: return
        val item = makeItem(nsUrl, headers)
        if (!player.canInsertItem(item, afterItem = current)) return
        player.insertItem(item, afterItem = current)
        preloadedItem = item
        preloadedUrl = url
        // Alla fine del brano passa da solo al successivo, senza silenzio
        player.actionAtItemEnd = AVPlayerActionAtItemEndAdvance
    }

    override fun cancelPreload() {
        preloadedItem?.let { item -> if (item !== player.currentItem) player.removeItem(item) }
        preloadedItem = null
        preloadedUrl = null
        // Senza un successivo pronto, a fine brano resta fermo lì (ripeti brano, fine coda)
        player.actionAtItemEnd = AVPlayerActionAtItemEndPause
    }

    override fun hasPreloaded(url: String): Boolean = preloadedItem != null && preloadedUrl == url

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
        // Controlli ravvicinati subito dopo il play: l'app sa prima che sta suonando
        scope.launch {
            repeat(5) {
                delay(80)
                checkState()
            }
        }
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
        player.removeAllItems()
        currentItem = null
        preloadedItem = null
        preloadedUrl = null
        player.actionAtItemEnd = AVPlayerActionAtItemEndPause
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
                    // Ogni ~2 secondi si riallinea anche la schermata di blocco (tempo e play/pausa)
                    if (++nowPlayingTicks % 8 == 0) updateNowPlaying()
                    delay(250)
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
        current.album?.let { nowPlaying[MPMediaItemPropertyAlbumTitle] = it }
        (current.durationMs ?: durationMs)?.let { nowPlaying[MPMediaItemPropertyPlaybackDuration] = it / 1000.0 }
        nowPlaying[MPNowPlayingInfoPropertyElapsedPlaybackTime] = positionMs / 1000.0
        // Play/pausa sulla schermata di blocco: conta se sta davvero suonando
        val playing = player.timeControlStatus == AVPlayerTimeControlStatusPlaying
        nowPlaying[MPNowPlayingInfoPropertyPlaybackRate] = if (playing) speed.toDouble() else 0.0
        nowPlaying[MPNowPlayingInfoPropertyDefaultPlaybackRate] = 1.0
        artwork?.let { nowPlaying[MPMediaItemPropertyArtwork] = it }
        MPNowPlayingInfoCenter.defaultCenter().nowPlayingInfo = nowPlaying
    }

    private fun loadArtwork(url: String?) {
        artworkJob?.cancel()
        artworkUrl = url
        if (url.isNullOrBlank()) return
        if (artworkCache.containsKey(url)) return
        artworkJob =
            scope.launch {
                // Se la stava già scaricando il prefetch, si aspetta che finisca
                var item = fetchArtwork(url)
                var waited = 0
                while (item == null && url in fetching && waited < 60) {
                    delay(250)
                    waited++
                    item = artworkCache[url]
                }
                if (item == null) item = artworkCache[url] ?: return@launch
                // Solo se è ancora il brano di questa copertina
                if (artworkUrl == url) {
                    artwork = item
                    updateNowPlaying()
                }
            }
    }

    /** Scarica in anticipo la copertina del brano successivo (senza mostrarla). */
    private fun prefetchArtwork(url: String?) {
        if (url.isNullOrBlank() || artworkCache.containsKey(url)) return
        scope.launch { fetchArtwork(url) }
    }

    private val fetching = mutableSetOf<String>()

    /**
     * Scarica la copertina (fino a 3 tentativi) e la prepara per schermata di blocco e auto:
     * quadrata (le miniature dei video sono 16:9) e di 600 px, una misura che tutte le auto accettano.
     */
    private suspend fun fetchArtwork(url: String): MPMediaItemArtwork? {
        artworkCache[url]?.let { return it }
        if (!fetching.add(url)) return null
        try {
            var image: UIImage? = null
            for (attempt in 0 until 3) {
                image =
                    withContext(Dispatchers.Default) {
                        val nsUrl = (if (url.startsWith("file://")) NSURL.fileURLWithPath(url.removePrefix("file://")) else NSURL.URLWithString(url))
                            ?: return@withContext null
                        val data = NSData.dataWithContentsOfURL(nsUrl) ?: return@withContext null
                        UIImage.imageWithData(data)
                    }
                if (image != null) break
                delay(1_000L * (attempt + 1))
            }
            val loaded = image?.let { squareImage(it, 600.0) } ?: return null
            val item = MPMediaItemArtwork(boundsSize = loaded.size) { _ -> loaded }
            artworkCache[url] = item
            if (artworkCache.size > 8) artworkCache.remove(artworkCache.keys.first())
            return item
        } finally {
            fetching.remove(url)
        }
    }

    /** Ritaglio quadrato al centro, ridimensionato a [side] punti (scala 1). */
    private fun squareImage(
        image: UIImage,
        side: Double,
    ): UIImage {
        val (width, height) = image.size.useContents { width to height }
        if (width <= 0.0 || height <= 0.0) return image
        val scale = maxOf(side / width, side / height)
        val drawWidth = width * scale
        val drawHeight = height * scale
        UIGraphicsBeginImageContextWithOptions(CGSizeMake(side, side), true, 1.0)
        image.drawInRect(CGRectMake((side - drawWidth) / 2, (side - drawHeight) / 2, drawWidth, drawHeight))
        val result = UIGraphicsGetImageFromCurrentImageContext()
        UIGraphicsEndImageContext()
        return result ?: image
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
