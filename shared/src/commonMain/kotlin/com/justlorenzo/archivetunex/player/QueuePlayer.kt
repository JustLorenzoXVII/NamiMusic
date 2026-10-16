package com.justlorenzo.archivetunex.player

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import moe.rukamori.archivetune.compat.media3.C
import moe.rukamori.archivetune.compat.media3.MediaItem
import moe.rukamori.archivetune.compat.media3.PlaybackException
import moe.rukamori.archivetune.compat.media3.PlaybackParameters
import moe.rukamori.archivetune.compat.media3.Player
import moe.rukamori.archivetune.compat.media3.QueueTimeline
import moe.rukamori.archivetune.compat.media3.ShuffleOrder
import moe.rukamori.archivetune.compat.media3.Timeline

/** Cosa riprodurre per un elemento della coda: URL dello stream (o file locale) e header HTTP. */
data class ResolvedMedia(
    val url: String,
    val headers: Map<String, String> = emptyMap(),
    val durationMs: Long? = null,
)

/**
 * Il lettore di ArchiveTuneX: implementa la stessa interfaccia [Player] di Media3 usata da
 * ArchiveTune (coda, casuale, ripetizione, eventi) in codice comune, e fa suonare
 * il brano corrente al motore della piattaforma ([AudioEngine]: ExoPlayer o AVPlayer).
 */
class QueuePlayer(
    private val engine: AudioEngine,
    private val resolver: suspend (MediaItem) -> ResolvedMedia,
) : Player, AudioEngine.Listener {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val listeners = mutableListOf<Player.Listener>()

    private val items = mutableListOf<MediaItem>()
    private val uids = mutableListOf<Any>()
    private var order: ShuffleOrder = ShuffleOrder.DefaultShuffleOrder(0)
    private var index = C.INDEX_UNSET

    private var prepared = false
    private var loadJob: Job? = null
    private var loadedIndexUid: Any? = null
    private var pendingSeekMs: Long = C.TIME_UNSET
    private var knownDurationMs: Long = C.TIME_UNSET
    private var engineIsPlaying = false
    private var engineBuffering = false

    init {
        engine.listener = this
    }

    // region Stato

    override var playbackState: Int = Player.STATE_IDLE
        private set

    override var playWhenReady: Boolean = false
        set(value) {
            if (field == value) return
            field = value
            if (value) {
                if (playbackState == Player.STATE_ENDED) seekTo(firstIndex(), 0)
                if (prepared && loadJob?.isActive != true) engine.play()
            } else {
                engine.pause()
            }
            dispatch(setOf(Player.EVENT_PLAY_WHEN_READY_CHANGED)) { it.onPlayWhenReadyChanged(value, Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST) }
            updateIsPlaying()
        }

    override var isPlaying: Boolean = false
        private set

    override val isLoading: Boolean
        get() = loadJob?.isActive == true || engineBuffering

    override var playerError: PlaybackException? = null
        private set

    override var repeatMode: Int = Player.REPEAT_MODE_OFF
        set(value) {
            if (field == value) return
            field = value
            dispatch(setOf(Player.EVENT_REPEAT_MODE_CHANGED)) { it.onRepeatModeChanged(value) }
        }

    override var shuffleModeEnabled: Boolean = false
        set(value) {
            if (field == value) return
            field = value
            dispatch(setOf(Player.EVENT_SHUFFLE_MODE_ENABLED_CHANGED)) { it.onShuffleModeEnabledChanged(value) }
        }

    override var playbackParameters: PlaybackParameters = PlaybackParameters.DEFAULT
        set(value) {
            field = value
            engine.speed = value.speed
            dispatch(setOf(Player.EVENT_PLAYBACK_PARAMETERS_CHANGED)) { it.onPlaybackParametersChanged(value) }
        }

    override var volume: Float = 1f
        set(value) {
            field = value.coerceIn(0f, 1f)
            engine.volume = field
            dispatch(setOf(Player.EVENT_VOLUME_CHANGED)) { it.onVolumeChanged(field) }
        }

    override var pauseAtEndOfMediaItems: Boolean = false

    override var skipSilenceEnabled: Boolean = false

    override val audioSessionId: Int = C.AUDIO_SESSION_ID_UNSET

    // endregion

    // region Coda

    override val currentTimeline: Timeline
        get() {
            val snapshotItems = items.toList()
            val snapshotUids = uids.toList()
            val currentUid = uids.getOrNull(index)
            return QueueTimeline(snapshotItems, snapshotUids, order) { i ->
                if (snapshotUids[i] == currentUid) duration else C.TIME_UNSET
            }
        }

    override val mediaItemCount: Int
        get() = items.size

    override val currentMediaItemIndex: Int
        get() = if (items.isEmpty()) 0 else index.coerceIn(items.indices)

    override val currentMediaItem: MediaItem?
        get() = items.getOrNull(index)

    override val nextMediaItemIndex: Int
        get() = if (items.isEmpty()) C.INDEX_UNSET else currentTimeline.getNextWindowIndex(index, repeatMode.takeIf { it != Player.REPEAT_MODE_ONE } ?: Player.REPEAT_MODE_OFF, shuffleModeEnabled)

    override val previousMediaItemIndex: Int
        get() = if (items.isEmpty()) C.INDEX_UNSET else currentTimeline.getPreviousWindowIndex(index, repeatMode.takeIf { it != Player.REPEAT_MODE_ONE } ?: Player.REPEAT_MODE_OFF, shuffleModeEnabled)

    override val availableCommands: Player.Commands
        get() =
            Player.Commands(
                buildSet {
                    addAll(
                        listOf(
                            Player.COMMAND_PLAY_PAUSE, Player.COMMAND_PREPARE, Player.COMMAND_STOP,
                            Player.COMMAND_SET_SPEED_AND_PITCH, Player.COMMAND_SET_SHUFFLE_MODE, Player.COMMAND_SET_REPEAT_MODE,
                            Player.COMMAND_GET_CURRENT_MEDIA_ITEM, Player.COMMAND_GET_TIMELINE, Player.COMMAND_CHANGE_MEDIA_ITEMS,
                            Player.COMMAND_SEEK_TO_MEDIA_ITEM,
                        ),
                    )
                    if (currentMediaItem != null) {
                        add(Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM)
                        add(Player.COMMAND_SEEK_TO_DEFAULT_POSITION)
                        add(Player.COMMAND_SEEK_TO_PREVIOUS)
                        add(Player.COMMAND_SEEK_TO_NEXT)
                    }
                    if (hasNextMediaItem()) add(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
                    if (hasPreviousMediaItem()) add(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
                },
            )

    override fun getMediaItemAt(index: Int): MediaItem = items[index]

    // endregion

    // region Tempo

    override val currentPosition: Long
        get() =
            when {
                currentMediaItem == null -> 0
                pendingSeekMs != C.TIME_UNSET -> pendingSeekMs
                loadJob?.isActive == true -> 0
                playbackState == Player.STATE_ENDED -> duration.coerceAtLeast(0)
                else -> engine.positionMs
            }

    override val duration: Long
        get() = engine.durationMs?.takeIf { loadJob?.isActive != true && it > 0 } ?: knownDurationMs

    override val bufferedPosition: Long
        get() = if (loadJob?.isActive == true) 0 else maxOf(engine.bufferedPositionMs, currentPosition)

    // endregion

    // region Comandi

    override fun addListener(listener: Player.Listener) {
        if (listener !in listeners) listeners += listener
    }

    override fun removeListener(listener: Player.Listener) {
        listeners -= listener
    }

    override fun prepare() {
        prepared = true
        if (playerError != null) setError(null)
        if (currentMediaItem != null && (loadedIndexUid != uids.getOrNull(index) || playbackState == Player.STATE_IDLE)) {
            loadCurrent(keepPosition = true)
        }
    }

    override fun stop() {
        loadJob?.cancel()
        engine.stop()
        prepared = false
        loadedIndexUid = null
        setPlaybackState(Player.STATE_IDLE)
        updateIsPlaying()
    }

    override fun release() {
        stop()
        listeners.clear()
    }

    override fun seekTo(positionMs: Long) {
        seekTo(currentMediaItemIndex, positionMs)
    }

    override fun seekTo(mediaItemIndex: Int, positionMs: Long) {
        if (items.isEmpty() || mediaItemIndex !in items.indices) return
        val target = if (positionMs == C.TIME_UNSET) 0 else positionMs.coerceAtLeast(0)
        if (mediaItemIndex == index && loadedIndexUid == uids[index] && playbackState != Player.STATE_IDLE) {
            val old = positionInfo()
            if (loadJob?.isActive == true) {
                pendingSeekMs = target
            } else {
                engine.seekTo(target)
                if (playbackState == Player.STATE_ENDED) {
                    setPlaybackState(Player.STATE_READY)
                    if (playWhenReady) engine.play()
                }
            }
            dispatch(setOf(Player.EVENT_POSITION_DISCONTINUITY)) { it.onPositionDiscontinuity(old, positionInfo(target), Player.DISCONTINUITY_REASON_SEEK) }
            return
        }
        val old = positionInfo()
        index = mediaItemIndex
        pendingSeekMs = target
        dispatch(setOf(Player.EVENT_MEDIA_ITEM_TRANSITION, Player.EVENT_POSITION_DISCONTINUITY)) {
            it.onPositionDiscontinuity(old, positionInfo(target), Player.DISCONTINUITY_REASON_SEEK)
            it.onMediaItemTransition(currentMediaItem, Player.MEDIA_ITEM_TRANSITION_REASON_SEEK)
        }
        if (prepared) loadCurrent(keepPosition = true)
    }

    override fun seekToNext() {
        val next = nextMediaItemIndex
        if (next != C.INDEX_UNSET) seekTo(next, C.TIME_UNSET)
    }

    override fun seekToPrevious() {
        val previous = previousMediaItemIndex
        // Come Media3: dopo 3 secondi "indietro" riparte dall'inizio del brano.
        if (previous == C.INDEX_UNSET || currentPosition > MAX_SEEK_TO_PREVIOUS_POSITION_MS) {
            seekTo(currentMediaItemIndex, 0)
        } else {
            seekTo(previous, C.TIME_UNSET)
        }
    }

    override fun setMediaItems(mediaItems: List<MediaItem>, startIndex: Int, startPositionMs: Long) {
        loadJob?.cancel()
        engine.stop()
        loadedIndexUid = null
        items.clear()
        uids.clear()
        items += mediaItems
        repeat(mediaItems.size) { uids += Any() }
        order = ShuffleOrder.DefaultShuffleOrder(mediaItems.size)
        index = if (mediaItems.isEmpty()) C.INDEX_UNSET else startIndex.coerceIn(mediaItems.indices)
        pendingSeekMs = if (startPositionMs == C.TIME_UNSET) 0 else startPositionMs
        knownDurationMs = C.TIME_UNSET
        if (mediaItems.isEmpty()) {
            setPlaybackState(if (prepared) Player.STATE_ENDED else Player.STATE_IDLE)
        }
        timelineChanged()
        dispatch(setOf(Player.EVENT_MEDIA_ITEM_TRANSITION)) { it.onMediaItemTransition(currentMediaItem, Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED) }
        if (prepared && mediaItems.isNotEmpty()) loadCurrent(keepPosition = true)
    }

    override fun addMediaItems(index: Int, mediaItems: List<MediaItem>) {
        if (mediaItems.isEmpty()) return
        val at = index.coerceIn(0, items.size)
        val wasEmpty = items.isEmpty()
        items.addAll(at, mediaItems)
        uids.addAll(at, List(mediaItems.size) { Any() })
        order = order.cloneAndInsert(at, mediaItems.size)
        if (wasEmpty) {
            this.index = 0
        } else if (at <= this.index) {
            this.index += mediaItems.size
        }
        timelineChanged()
        if (wasEmpty) {
            dispatch(setOf(Player.EVENT_MEDIA_ITEM_TRANSITION)) { it.onMediaItemTransition(currentMediaItem, Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED) }
            if (prepared) loadCurrent(keepPosition = false)
        }
    }

    override fun removeMediaItems(fromIndex: Int, toIndex: Int) {
        val from = fromIndex.coerceIn(0, items.size)
        val to = toIndex.coerceIn(from, items.size)
        if (from == to) return
        val currentUid = uids.getOrNull(index)
        val removingCurrent = index in from until to
        repeat(to - from) {
            items.removeAt(from)
            uids.removeAt(from)
        }
        order = order.cloneAndRemove(from, to)
        if (items.isEmpty()) {
            index = C.INDEX_UNSET
            loadJob?.cancel()
            engine.stop()
            loadedIndexUid = null
            setPlaybackState(if (prepared) Player.STATE_ENDED else Player.STATE_IDLE)
            timelineChanged()
            dispatch(setOf(Player.EVENT_MEDIA_ITEM_TRANSITION)) { it.onMediaItemTransition(null, Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED) }
            updateIsPlaying()
            return
        }
        if (removingCurrent) {
            index = from.coerceAtMost(items.lastIndex)
            pendingSeekMs = 0
            timelineChanged()
            dispatch(setOf(Player.EVENT_MEDIA_ITEM_TRANSITION)) { it.onMediaItemTransition(currentMediaItem, Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED) }
            if (prepared) loadCurrent(keepPosition = true)
        } else {
            index = uids.indexOf(currentUid)
            timelineChanged()
        }
    }

    override fun moveMediaItems(fromIndex: Int, toIndex: Int, newIndex: Int) {
        val from = fromIndex.coerceIn(0, items.size)
        val to = toIndex.coerceIn(from, items.size)
        if (from == to || from == newIndex) return
        val currentUid = uids.getOrNull(index)
        val movedItems = items.subList(from, to).toList()
        val movedUids = uids.subList(from, to).toList()
        repeat(to - from) {
            items.removeAt(from)
            uids.removeAt(from)
        }
        val insertAt = newIndex.coerceIn(0, items.size)
        items.addAll(insertAt, movedItems)
        uids.addAll(insertAt, movedUids)
        // Media3 mantiene l'ordine casuale degli elementi: lo ricostruiamo spostando gli indici.
        order = remapShuffleOrder(from, to, insertAt)
        index = uids.indexOf(currentUid)
        timelineChanged()
    }

    override fun replaceMediaItems(fromIndex: Int, toIndex: Int, mediaItems: List<MediaItem>) {
        val from = fromIndex.coerceIn(0, items.size)
        val to = toIndex.coerceIn(from, items.size)
        if (to - from == mediaItems.size) {
            for (i in mediaItems.indices) items[from + i] = mediaItems[i]
            timelineChanged()
            return
        }
        val currentUid = uids.getOrNull(index)
        val keepsCurrent = index !in from until to
        addMediaItems(to, mediaItems)
        if (keepsCurrent) {
            removeMediaItems(from, to)
            index = uids.indexOf(currentUid)
        } else {
            removeMediaItems(from, to)
        }
    }

    override fun setShuffleOrder(shuffleOrder: ShuffleOrder) {
        order = if (shuffleOrder.length == items.size) shuffleOrder else ShuffleOrder.DefaultShuffleOrder(items.size)
        timelineChanged()
    }

    // endregion

    // region Motore

    private fun loadCurrent(keepPosition: Boolean) {
        val item = currentMediaItem ?: return
        val uid = uids[index]
        loadJob?.cancel()
        engine.stop()
        loadedIndexUid = uid
        knownDurationMs = C.TIME_UNSET
        if (!keepPosition) pendingSeekMs = 0
        if (playerError != null) setError(null)
        setPlaybackState(Player.STATE_BUFFERING)
        updateIsPlaying()
        loadJob =
            scope.launch {
                try {
                    val media = resolver(item)
                    if (uids.getOrNull(index) !== uid) return@launch
                    knownDurationMs = media.durationMs ?: C.TIME_UNSET
                    val metadata = item.mediaMetadata
                    engine.load(
                        url = media.url,
                        headers = media.headers,
                        info =
                            NowPlayingInfo(
                                title = metadata.title?.toString().orEmpty(),
                                artist = metadata.artist?.toString().orEmpty(),
                                artworkUrl = metadata.artworkUri?.toString(),
                                durationMs = media.durationMs,
                            ),
                        playWhenReady = false,
                    )
                    engine.speed = playbackParameters.speed
                    engine.volume = volume
                    val seek = pendingSeekMs
                    pendingSeekMs = C.TIME_UNSET
                    if (seek > 0) engine.seekTo(seek)
                    if (playWhenReady) engine.play()
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (failure: Throwable) {
                    if (uids.getOrNull(index) !== uid) return@launch
                    pendingSeekMs = C.TIME_UNSET
                    setError(failure.toPlaybackException())
                }
            }
    }

    private fun setError(error: PlaybackException?) {
        if (playerError === error) return
        playerError = error
        if (error != null) {
            loadedIndexUid = null
            setPlaybackState(Player.STATE_IDLE)
            dispatch(setOf(Player.EVENT_PLAYER_ERROR)) {
                it.onPlayerErrorChanged(error)
                it.onPlayerError(error)
            }
        } else {
            dispatch(setOf(Player.EVENT_PLAYER_ERROR)) { it.onPlayerErrorChanged(null) }
        }
        updateIsPlaying()
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        engineIsPlaying = isPlaying
        if (isPlaying) {
            engineBuffering = false
            if (playbackState == Player.STATE_BUFFERING) setPlaybackState(Player.STATE_READY)
        }
        updateIsPlaying()
    }

    override fun onBufferingChanged(isBuffering: Boolean) {
        engineBuffering = isBuffering
        if (loadJob?.isActive == true || playbackState == Player.STATE_IDLE || playbackState == Player.STATE_ENDED) return
        setPlaybackState(if (isBuffering) Player.STATE_BUFFERING else Player.STATE_READY)
        dispatch(setOf(Player.EVENT_IS_LOADING_CHANGED)) { it.onIsLoadingChanged(isBuffering) }
    }

    override fun onEnded() {
        engineIsPlaying = false
        if (repeatMode == Player.REPEAT_MODE_ONE) {
            engine.seekTo(0)
            engine.play()
            dispatch(setOf(Player.EVENT_MEDIA_ITEM_TRANSITION)) { it.onMediaItemTransition(currentMediaItem, Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT) }
            return
        }
        val next = currentTimeline.getNextWindowIndex(index, repeatMode, shuffleModeEnabled)
        if (next == C.INDEX_UNSET) {
            setPlaybackState(Player.STATE_ENDED)
            updateIsPlaying()
            return
        }
        val old = positionInfo()
        index = next
        pendingSeekMs = 0
        if (pauseAtEndOfMediaItems && playWhenReady) playWhenReady = false
        dispatch(setOf(Player.EVENT_MEDIA_ITEM_TRANSITION, Player.EVENT_POSITION_DISCONTINUITY)) {
            it.onPositionDiscontinuity(old, positionInfo(0), Player.DISCONTINUITY_REASON_AUTO_TRANSITION)
            it.onMediaItemTransition(currentMediaItem, Player.MEDIA_ITEM_TRANSITION_REASON_AUTO)
        }
        loadCurrent(keepPosition = true)
    }

    override fun onError(message: String) {
        setError(PlaybackException(message, null, PlaybackException.ERROR_CODE_IO_UNSPECIFIED))
    }

    override fun onRemoteNext() = seekToNext()

    override fun onRemotePrevious() = seekToPrevious()

    override fun onRemotePlay() {
        if (playbackState == Player.STATE_IDLE) prepare()
        playWhenReady = true
    }

    override fun onRemotePause() {
        playWhenReady = false
    }

    override fun onRemoteSeek(positionMs: Long) = seekTo(positionMs)

    // endregion

    // region Utilità

    private fun firstIndex(): Int = currentTimeline.getFirstWindowIndex(shuffleModeEnabled).takeIf { it != C.INDEX_UNSET } ?: 0

    private fun setPlaybackState(state: Int) {
        if (playbackState == state) return
        playbackState = state
        dispatch(setOf(Player.EVENT_PLAYBACK_STATE_CHANGED)) { it.onPlaybackStateChanged(state) }
    }

    private fun updateIsPlaying() {
        val now = playWhenReady && engineIsPlaying && playbackState == Player.STATE_READY
        if (now == isPlaying) return
        isPlaying = now
        dispatch(setOf(Player.EVENT_IS_PLAYING_CHANGED)) { it.onIsPlayingChanged(now) }
    }

    private fun timelineChanged() {
        val timeline = currentTimeline
        dispatch(setOf(Player.EVENT_TIMELINE_CHANGED)) { it.onTimelineChanged(timeline, Player.TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED) }
    }

    private fun positionInfo(positionMs: Long = currentPosition): Player.PositionInfo =
        Player.PositionInfo(
            windowUid = uids.getOrNull(index),
            mediaItemIndex = currentMediaItemIndex,
            mediaItem = currentMediaItem,
            periodUid = uids.getOrNull(index),
            periodIndex = currentMediaItemIndex,
            positionMs = positionMs,
            contentPositionMs = positionMs,
        )

    private fun remapShuffleOrder(from: Int, to: Int, insertAt: Int): ShuffleOrder {
        val size = items.size
        if (order.length != size) return ShuffleOrder.DefaultShuffleOrder(size)
        // Posizione vecchia -> nuova di ogni elemento.
        val oldPositions = (0 until size).toMutableList()
        val moved = oldPositions.subList(from, to).toList()
        repeat(to - from) { oldPositions.removeAt(from) }
        oldPositions.addAll(insertAt, moved)
        val newIndexOf = IntArray(size)
        oldPositions.forEachIndexed { newIdx, oldIdx -> newIndexOf[oldIdx] = newIdx }
        val shuffled = mutableListOf<Int>()
        var i = order.getFirstIndex()
        while (i != C.INDEX_UNSET && shuffled.size < size) {
            shuffled += newIndexOf[i]
            i = order.getNextIndex(i)
        }
        return ShuffleOrder.DefaultShuffleOrder(shuffled.toIntArray(), 0L)
    }

    private inline fun dispatch(
        events: Set<Int>,
        crossinline block: (Player.Listener) -> Unit,
    ) {
        val snapshot = listeners.toList()
        snapshot.forEach { block(it) }
        val grouped = Player.Events(events)
        snapshot.forEach { it.onEvents(this, grouped) }
    }

    private fun Throwable.toPlaybackException(): PlaybackException =
        this as? PlaybackException
            ?: PlaybackException(message ?: toString(), this, PlaybackException.ERROR_CODE_REMOTE_ERROR)

    // endregion

    private companion object {
        const val MAX_SEEK_TO_PREVIOUS_POSITION_MS = 3_000L
    }
}
