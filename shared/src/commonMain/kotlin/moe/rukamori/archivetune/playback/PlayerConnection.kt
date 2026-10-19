/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.rukamori.archivetune.playback

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import moe.rukamori.archivetune.compat.media3.MediaItem
import moe.rukamori.archivetune.compat.media3.PlaybackException
import moe.rukamori.archivetune.compat.media3.PlaybackParameters
import moe.rukamori.archivetune.compat.media3.Player
import moe.rukamori.archivetune.compat.media3.Player.Companion.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM
import moe.rukamori.archivetune.compat.media3.Player.Companion.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM
import moe.rukamori.archivetune.compat.media3.Player.Companion.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM
import moe.rukamori.archivetune.compat.media3.Player.Companion.REPEAT_MODE_OFF
import moe.rukamori.archivetune.compat.media3.Timeline
import moe.rukamori.archivetune.db.MusicDatabase
import moe.rukamori.archivetune.extensions.currentMetadata
import moe.rukamori.archivetune.extensions.getCurrentQueueIndex
import moe.rukamori.archivetune.extensions.getQueueWindows
import moe.rukamori.archivetune.playback.queues.Queue
import moe.rukamori.archivetune.utils.reportException

internal enum class CanvasArtworkRefetchResult {
    Success,
    Failure,
    AlreadyRunning,
}

/**
 * Il collegamento tra le schermate e il lettore, come nell'originale.
 * Su Android l'originale si collegava al MusicService con un Binder; qui il servizio è comune.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PlayerConnection(
    val service: MusicService,
    val database: MusicDatabase,
    scope: CoroutineScope,
) : Player.Listener {
    val player = service.player
    val localPlayer = service.localPlayer

    val playbackState = MutableStateFlow(player.playbackState)
    private val _isPlaying = MutableStateFlow(player.isPlaying)
    val isPlaying = _isPlaying.asStateFlow()
    val playbackParameters = MutableStateFlow(player.playbackParameters)
    val mediaMetadata = service.currentMediaMetadata
    val currentSong =
        mediaMetadata.flatMapLatest {
            database.song(it?.id)
        }
    val currentLyrics =
        mediaMetadata.flatMapLatest { mediaMetadata ->
            database.lyrics(mediaMetadata?.id)
        }
    val currentFormat =
        mediaMetadata.flatMapLatest { mediaMetadata ->
            database.format(mediaMetadata?.id)
        }

    val queueTitle = MutableStateFlow<String?>(null)
    val queueWindows = MutableStateFlow<List<Timeline.Window>>(emptyList())
    val currentMediaItemIndex = MutableStateFlow(-1)
    val currentWindowIndex = MutableStateFlow(-1)

    val shuffleModeEnabled = MutableStateFlow(false)
    val repeatMode = MutableStateFlow(REPEAT_MODE_OFF)

    val canSkipPrevious = MutableStateFlow(true)
    val canSkipNext = MutableStateFlow(true)

    val aodModeEnabled = MutableStateFlow(false)

    val error = MutableStateFlow<PlaybackException?>(null)
    private var dismissedPlaybackError: PlaybackException? = null
    val waitingForNetworkConnection = service.waitingForNetworkConnection
    val queueRestoreCompleted = service.queueRestoreCompleted

    /** Canvas (copertine animate di Spotify): non ancora portato, la rete resta disattivata. */
    internal val canvasNetworkAllowed: StateFlow<Boolean> = MutableStateFlow(false)
    private val _isCanvasArtworkRefetching = MutableStateFlow(false)
    internal val isCanvasArtworkRefetching = _isCanvasArtworkRefetching.asStateFlow()

    /** Ricarica la copertina animata: non ancora portato, risponde sempre Failure. */
    internal suspend fun refetchCanvasArtwork(
        metadata: moe.rukamori.archivetune.models.MediaMetadata,
        requireVertical: Boolean,
    ): CanvasArtworkRefetchResult = CanvasArtworkRefetchResult.Failure

    init {
        player.addListener(this)

        playbackState.value = player.playbackState
        _isPlaying.value = player.isPlaying
        playbackParameters.value = player.playbackParameters
        queueTitle.value = service.queueTitle
        queueWindows.value = player.getQueueWindows()
        currentWindowIndex.value = player.getCurrentQueueIndex()
        currentMediaItemIndex.value = player.currentMediaItemIndex
        shuffleModeEnabled.value = player.shuffleModeEnabled
        repeatMode.value = player.repeatMode
        if (player.mediaItemCount > 0 && service.currentMediaMetadata.value == null) {
            service.currentMediaMetadata.value = player.currentMetadata
        }
    }

    fun playQueue(
        queue: Queue,
        shuffle: Boolean = false,
    ) {
        service.playQueue(queue, shuffle = shuffle)
    }

    fun startRadioSeamlessly() {
        service.startRadioSeamlessly()
    }

    fun playNext(item: MediaItem) = playNext(listOf(item))

    fun playNext(items: List<MediaItem>) {
        service.playNext(items)
    }

    fun moveQueueItemToNext(mediaItemIndex: Int) {
        service.moveQueueItemToNext(mediaItemIndex)
    }

    fun addToQueue(item: MediaItem) = addToQueue(listOf(item))

    fun addToQueue(items: List<MediaItem>) {
        service.addToQueue(items)
    }

    fun playFromVoiceSearch(query: String) {
        service.playFromVoiceSearch(query)
    }

    fun toggleLike() {
        service.toggleLike()
    }

    fun dismissPlaybackError() {
        dismissedPlaybackError = error.value ?: player.playerError
        error.value = null
    }

    fun seekToNext() {
        player.seekToNext()
        player.prepare()
        player.playWhenReady = true
    }

    fun seekToPrevious() {
        player.seekToPrevious()
        player.prepare()
        player.playWhenReady = true
    }

    override fun onPlaybackStateChanged(state: Int) {
        playbackState.value = state
        updatePlaybackError(player.playerError)
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        _isPlaying.value = isPlaying
    }

    override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {
        this.playbackParameters.value = playbackParameters
    }

    override fun onMediaItemTransition(
        mediaItem: MediaItem?,
        reason: Int,
    ) {
        currentMediaItemIndex.value = player.currentMediaItemIndex
        currentWindowIndex.value = player.getCurrentQueueIndex()
        updateCanSkipPreviousAndNext()
    }

    override fun onTimelineChanged(
        timeline: Timeline,
        reason: Int,
    ) {
        queueWindows.value = player.getQueueWindows()
        queueTitle.value = service.queueTitle
        currentMediaItemIndex.value = player.currentMediaItemIndex
        currentWindowIndex.value = player.getCurrentQueueIndex()
        updateCanSkipPreviousAndNext()
    }

    override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
        this.shuffleModeEnabled.value = shuffleModeEnabled
        queueWindows.value = player.getQueueWindows()
        currentWindowIndex.value = player.getCurrentQueueIndex()
        updateCanSkipPreviousAndNext()
    }

    override fun onRepeatModeChanged(repeatMode: Int) {
        this.repeatMode.value = repeatMode
        updateCanSkipPreviousAndNext()
    }

    override fun onPlayerErrorChanged(error: PlaybackException?) {
        if (error != null) {
            reportException(error)
        }
        updatePlaybackError(error)
    }

    private fun updatePlaybackError(playbackError: PlaybackException?) {
        when {
            playbackError == null -> {
                dismissedPlaybackError = null
                error.value = null
            }

            playbackError !== dismissedPlaybackError -> {
                dismissedPlaybackError = null
                error.value = playbackError
            }
        }
    }

    private fun updateCanSkipPreviousAndNext() {
        if (!player.currentTimeline.isEmpty) {
            val window =
                player.currentTimeline.getWindow(player.currentMediaItemIndex, Timeline.Window())
            canSkipPrevious.value = player.isCommandAvailable(COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM) ||
                !window.isLive ||
                player.isCommandAvailable(COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
            canSkipNext.value = window.isLive &&
                window.isDynamic ||
                player.isCommandAvailable(COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
        } else {
            canSkipPrevious.value = false
            canSkipNext.value = false
        }
    }

    fun dispose() {
        player.removeListener(this)
    }
}
