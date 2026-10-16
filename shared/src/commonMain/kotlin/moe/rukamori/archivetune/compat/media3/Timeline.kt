package moe.rukamori.archivetune.compat.media3

/** La coda vista dal lettore (androidx.media3.common.Timeline, solo le parti usate da ArchiveTune). */
abstract class Timeline {
    abstract val windowCount: Int

    abstract fun getWindow(windowIndex: Int, window: Window): Window

    open val shuffleOrder: ShuffleOrder? = null

    val isEmpty: Boolean
        get() = windowCount == 0

    val periodCount: Int
        get() = windowCount

    fun getWindow(windowIndex: Int): Window = getWindow(windowIndex, Window())

    open fun getFirstWindowIndex(shuffleModeEnabled: Boolean): Int {
        if (isEmpty) return C.INDEX_UNSET
        return if (shuffleModeEnabled) shuffleOrder?.getFirstIndex() ?: 0 else 0
    }

    open fun getLastWindowIndex(shuffleModeEnabled: Boolean): Int {
        if (isEmpty) return C.INDEX_UNSET
        return if (shuffleModeEnabled) shuffleOrder?.getLastIndex() ?: (windowCount - 1) else windowCount - 1
    }

    open fun getNextWindowIndex(windowIndex: Int, repeatMode: Int, shuffleModeEnabled: Boolean): Int {
        if (repeatMode == Player.REPEAT_MODE_ONE) return windowIndex
        if (windowIndex == getLastWindowIndex(shuffleModeEnabled)) {
            return if (repeatMode == Player.REPEAT_MODE_ALL) getFirstWindowIndex(shuffleModeEnabled) else C.INDEX_UNSET
        }
        return if (shuffleModeEnabled) shuffleOrder?.getNextIndex(windowIndex) ?: (windowIndex + 1) else windowIndex + 1
    }

    open fun getPreviousWindowIndex(windowIndex: Int, repeatMode: Int, shuffleModeEnabled: Boolean): Int {
        if (repeatMode == Player.REPEAT_MODE_ONE) return windowIndex
        if (windowIndex == getFirstWindowIndex(shuffleModeEnabled)) {
            return if (repeatMode == Player.REPEAT_MODE_ALL) getLastWindowIndex(shuffleModeEnabled) else C.INDEX_UNSET
        }
        return if (shuffleModeEnabled) shuffleOrder?.getPreviousIndex(windowIndex) ?: (windowIndex - 1) else windowIndex - 1
    }

    class Window {
        var uid: Any = SINGLE_WINDOW_UID
        var mediaItem: MediaItem = MediaItem.EMPTY
        var firstPeriodIndex: Int = 0
        var lastPeriodIndex: Int = 0
        var durationMs: Long = C.TIME_UNSET
        var defaultPositionMs: Long = 0
        var isSeekable: Boolean = true
        var isPlaceholder: Boolean = false
        var isLive: Boolean = false
        var isDynamic: Boolean = false

        companion object {
            val SINGLE_WINDOW_UID = Any()
        }
    }

    companion object {
        val EMPTY: Timeline =
            object : Timeline() {
                override val windowCount: Int = 0

                override fun getWindow(windowIndex: Int, window: Window): Window = throw IndexOutOfBoundsException()
            }
    }
}

/**
 * Timeline di una coda: ogni elemento ha un uid stabile (come in ExoPlayer),
 * così la UI riconosce lo stesso elemento anche se la coda viene riordinata.
 */
class QueueTimeline(
    private val items: List<MediaItem>,
    private val uids: List<Any>,
    override val shuffleOrder: ShuffleOrder?,
    private val durations: (Int) -> Long = { C.TIME_UNSET },
) : Timeline() {
    override val windowCount: Int
        get() = items.size

    override fun getWindow(windowIndex: Int, window: Window): Window =
        window.apply {
            uid = uids[windowIndex]
            mediaItem = items[windowIndex]
            firstPeriodIndex = windowIndex
            lastPeriodIndex = windowIndex
            durationMs = durations(windowIndex)
            defaultPositionMs = 0
        }
}
