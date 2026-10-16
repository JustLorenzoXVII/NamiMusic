package moe.rukamori.archivetune.compat.media3

/**
 * Interfaccia del lettore, con gli stessi nomi di androidx.media3.common.Player,
 * così il codice di ArchiveTune (schermate, coda, PlayerConnection) resta uguale.
 */
interface Player {
    // Stato
    val playbackState: Int
    var playWhenReady: Boolean
    val isPlaying: Boolean
    val isLoading: Boolean
    val playerError: PlaybackException?
    var repeatMode: Int
    var shuffleModeEnabled: Boolean
    var playbackParameters: PlaybackParameters
    var volume: Float
    var pauseAtEndOfMediaItems: Boolean
    var skipSilenceEnabled: Boolean
    val audioSessionId: Int

    // Coda
    val currentTimeline: Timeline
    val mediaItemCount: Int
    val currentMediaItemIndex: Int
    val currentMediaItem: MediaItem?
    val nextMediaItemIndex: Int
    val previousMediaItemIndex: Int
    val availableCommands: Commands

    fun hasNextMediaItem(): Boolean = nextMediaItemIndex != C.INDEX_UNSET

    fun hasPreviousMediaItem(): Boolean = previousMediaItemIndex != C.INDEX_UNSET

    fun getMediaItemAt(index: Int): MediaItem

    fun isCommandAvailable(command: Int): Boolean = availableCommands.contains(command)

    // Tempo
    val currentPosition: Long
    val duration: Long
    val contentPosition: Long
        get() = currentPosition
    val contentDuration: Long
        get() = duration
    val bufferedPosition: Long
    val bufferedPercentage: Int
        get() {
            val d = duration
            if (d == C.TIME_UNSET || d <= 0) return 0
            return ((bufferedPosition * 100) / d).toInt().coerceIn(0, 100)
        }
    val totalBufferedDuration: Long
        get() = (bufferedPosition - currentPosition).coerceAtLeast(0)
    val isCurrentMediaItemSeekable: Boolean
        get() = currentMediaItem != null

    // Comandi
    fun addListener(listener: Listener)

    fun removeListener(listener: Listener)

    fun prepare()

    fun play() {
        playWhenReady = true
    }

    fun pause() {
        playWhenReady = false
    }

    fun stop()

    fun release()

    fun seekTo(positionMs: Long)

    fun seekTo(mediaItemIndex: Int, positionMs: Long)

    fun seekToDefaultPosition() = seekTo(currentMediaItemIndex, C.TIME_UNSET)

    fun seekToDefaultPosition(mediaItemIndex: Int) = seekTo(mediaItemIndex, C.TIME_UNSET)

    fun seekToNext()

    fun seekToPrevious()

    fun seekToNextMediaItem() {
        val next = nextMediaItemIndex
        if (next != C.INDEX_UNSET) seekTo(next, C.TIME_UNSET)
    }

    fun seekToPreviousMediaItem() {
        val previous = previousMediaItemIndex
        if (previous != C.INDEX_UNSET) seekTo(previous, C.TIME_UNSET)
    }

    fun setPlaybackSpeed(speed: Float) {
        playbackParameters = playbackParameters.copy(speed = speed)
    }

    fun setMediaItem(mediaItem: MediaItem) = setMediaItems(listOf(mediaItem), 0, C.TIME_UNSET)

    fun setMediaItem(mediaItem: MediaItem, startPositionMs: Long) = setMediaItems(listOf(mediaItem), 0, startPositionMs)

    fun setMediaItems(mediaItems: List<MediaItem>) = setMediaItems(mediaItems, 0, C.TIME_UNSET)

    fun setMediaItems(mediaItems: List<MediaItem>, resetPosition: Boolean) = setMediaItems(mediaItems, 0, C.TIME_UNSET)

    fun setMediaItems(mediaItems: List<MediaItem>, startIndex: Int, startPositionMs: Long)

    fun addMediaItem(mediaItem: MediaItem) = addMediaItems(mediaItemCount, listOf(mediaItem))

    fun addMediaItem(index: Int, mediaItem: MediaItem) = addMediaItems(index, listOf(mediaItem))

    fun addMediaItems(mediaItems: List<MediaItem>) = addMediaItems(mediaItemCount, mediaItems)

    fun addMediaItems(index: Int, mediaItems: List<MediaItem>)

    fun removeMediaItem(index: Int) = removeMediaItems(index, index + 1)

    fun removeMediaItems(fromIndex: Int, toIndex: Int)

    fun moveMediaItem(currentIndex: Int, newIndex: Int) = moveMediaItems(currentIndex, currentIndex + 1, newIndex)

    fun moveMediaItems(fromIndex: Int, toIndex: Int, newIndex: Int)

    fun replaceMediaItem(index: Int, mediaItem: MediaItem) {
        replaceMediaItems(index, index + 1, listOf(mediaItem))
    }

    fun replaceMediaItems(fromIndex: Int, toIndex: Int, mediaItems: List<MediaItem>)

    fun clearMediaItems() = removeMediaItems(0, mediaItemCount)

    fun setShuffleOrder(shuffleOrder: ShuffleOrder)

    /** Insieme di comandi disponibili (Player.Commands). */
    class Commands(private val commands: Set<Int>) {
        fun contains(command: Int): Boolean = command in commands

        fun size(): Int = commands.size

        companion object {
            val EMPTY = Commands(emptySet())
        }
    }

    /** Eventi raggruppati, passati a [Listener.onEvents]. */
    class Events(private val events: Set<Int>) {
        fun contains(event: Int): Boolean = event in events

        fun containsAny(vararg events: Int): Boolean = events.any { it in this.events }

        fun size(): Int = events.size

        fun get(index: Int): Int = events.elementAt(index)
    }

    data class PositionInfo(
        val windowUid: Any?,
        val mediaItemIndex: Int,
        val mediaItem: MediaItem?,
        val periodUid: Any?,
        val periodIndex: Int,
        val positionMs: Long,
        val contentPositionMs: Long,
        val adGroupIndex: Int = C.INDEX_UNSET,
        val adIndexInAdGroup: Int = C.INDEX_UNSET,
    )

    interface Listener {
        fun onEvents(player: Player, events: Events) {}

        fun onTimelineChanged(timeline: Timeline, reason: Int) {}

        fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {}

        fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {}

        fun onIsLoadingChanged(isLoading: Boolean) {}

        fun onAvailableCommandsChanged(availableCommands: Commands) {}

        fun onPlaybackStateChanged(playbackState: Int) {}

        fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {}

        fun onIsPlayingChanged(isPlaying: Boolean) {}

        fun onRepeatModeChanged(repeatMode: Int) {}

        fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {}

        fun onPlayerError(error: PlaybackException) {}

        fun onPlayerErrorChanged(error: PlaybackException?) {}

        fun onPositionDiscontinuity(oldPosition: PositionInfo, newPosition: PositionInfo, reason: Int) {}

        fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {}

        fun onVolumeChanged(volume: Float) {}

        fun onAudioSessionIdChanged(audioSessionId: Int) {}

        fun onSkipSilenceEnabledChanged(skipSilenceEnabled: Boolean) {}
    }

    // Annotazioni di tipo come in Media3 (solo documentazione).
    @Retention(AnnotationRetention.SOURCE)
    annotation class State

    @Retention(AnnotationRetention.SOURCE)
    annotation class RepeatMode

    @Retention(AnnotationRetention.SOURCE)
    annotation class Event

    @Retention(AnnotationRetention.SOURCE)
    annotation class Command

    @Retention(AnnotationRetention.SOURCE)
    annotation class DiscontinuityReason

    @Retention(AnnotationRetention.SOURCE)
    annotation class MediaItemTransitionReason

    @Retention(AnnotationRetention.SOURCE)
    annotation class PlayWhenReadyChangeReason

    @Retention(AnnotationRetention.SOURCE)
    annotation class TimelineChangeReason

    companion object {
        const val STATE_IDLE = 1
        const val STATE_BUFFERING = 2
        const val STATE_READY = 3
        const val STATE_ENDED = 4

        const val REPEAT_MODE_OFF = 0
        const val REPEAT_MODE_ONE = 1
        const val REPEAT_MODE_ALL = 2

        const val PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST = 1
        const val PLAY_WHEN_READY_CHANGE_REASON_AUDIO_FOCUS_LOSS = 2
        const val PLAY_WHEN_READY_CHANGE_REASON_AUDIO_BECOMING_NOISY = 3
        const val PLAY_WHEN_READY_CHANGE_REASON_REMOTE = 4
        const val PLAY_WHEN_READY_CHANGE_REASON_END_OF_MEDIA_ITEM = 5

        const val MEDIA_ITEM_TRANSITION_REASON_REPEAT = 0
        const val MEDIA_ITEM_TRANSITION_REASON_AUTO = 1
        const val MEDIA_ITEM_TRANSITION_REASON_SEEK = 2
        const val MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED = 3

        const val TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED = 0
        const val TIMELINE_CHANGE_REASON_SOURCE_UPDATE = 1

        const val DISCONTINUITY_REASON_AUTO_TRANSITION = 0
        const val DISCONTINUITY_REASON_SEEK = 1
        const val DISCONTINUITY_REASON_SEEK_ADJUSTMENT = 2
        const val DISCONTINUITY_REASON_SKIP = 3
        const val DISCONTINUITY_REASON_REMOVE = 4
        const val DISCONTINUITY_REASON_INTERNAL = 5

        const val EVENT_TIMELINE_CHANGED = 0
        const val EVENT_MEDIA_ITEM_TRANSITION = 1
        const val EVENT_TRACKS_CHANGED = 2
        const val EVENT_IS_LOADING_CHANGED = 3
        const val EVENT_PLAYBACK_STATE_CHANGED = 4
        const val EVENT_PLAY_WHEN_READY_CHANGED = 5
        const val EVENT_PLAYBACK_SUPPRESSION_REASON_CHANGED = 6
        const val EVENT_IS_PLAYING_CHANGED = 7
        const val EVENT_REPEAT_MODE_CHANGED = 8
        const val EVENT_SHUFFLE_MODE_ENABLED_CHANGED = 9
        const val EVENT_PLAYER_ERROR = 10
        const val EVENT_POSITION_DISCONTINUITY = 11
        const val EVENT_PLAYBACK_PARAMETERS_CHANGED = 12
        const val EVENT_AVAILABLE_COMMANDS_CHANGED = 13
        const val EVENT_MEDIA_METADATA_CHANGED = 14
        const val EVENT_AUDIO_SESSION_ID = 21
        const val EVENT_VOLUME_CHANGED = 22
        const val EVENT_SKIP_SILENCE_ENABLED_CHANGED = 23
        const val EVENT_DEVICE_VOLUME_CHANGED = 30

        const val COMMAND_PLAY_PAUSE = 1
        const val COMMAND_PREPARE = 2
        const val COMMAND_STOP = 3
        const val COMMAND_SEEK_TO_DEFAULT_POSITION = 4
        const val COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM = 5
        const val COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM = 6
        const val COMMAND_SEEK_TO_PREVIOUS = 7
        const val COMMAND_SEEK_TO_NEXT_MEDIA_ITEM = 8
        const val COMMAND_SEEK_TO_NEXT = 9
        const val COMMAND_SEEK_TO_MEDIA_ITEM = 10
        const val COMMAND_SET_SPEED_AND_PITCH = 13
        const val COMMAND_SET_SHUFFLE_MODE = 14
        const val COMMAND_SET_REPEAT_MODE = 15
        const val COMMAND_GET_CURRENT_MEDIA_ITEM = 16
        const val COMMAND_GET_TIMELINE = 17
        const val COMMAND_CHANGE_MEDIA_ITEMS = 20
    }
}
