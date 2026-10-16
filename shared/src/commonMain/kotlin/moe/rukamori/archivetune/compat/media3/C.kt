package moe.rukamori.archivetune.compat.media3

/** Costanti di androidx.media3.common.C usate da ArchiveTune. */
object C {
    const val TIME_UNSET: Long = Long.MIN_VALUE + 1
    const val TIME_END_OF_SOURCE: Long = Long.MIN_VALUE
    const val INDEX_UNSET: Int = -1
    const val LENGTH_UNSET: Long = -1
    const val RATE_UNSET: Float = -Float.MAX_VALUE
    const val POSITION_UNSET: Int = -1
    const val WAKE_MODE_NETWORK: Int = 2
    const val USAGE_MEDIA: Int = 1
    const val AUDIO_CONTENT_TYPE_MUSIC: Int = 2
    const val AUDIO_SESSION_ID_UNSET: Int = 0
    const val TRACK_TYPE_AUDIO: Int = 1
    const val TRACK_TYPE_VIDEO: Int = 2

    fun usToMs(timeUs: Long): Long = if (timeUs == TIME_UNSET || timeUs == TIME_END_OF_SOURCE) timeUs else timeUs / 1000

    fun msToUs(timeMs: Long): Long = if (timeMs == TIME_UNSET || timeMs == TIME_END_OF_SOURCE) timeMs else timeMs * 1000
}
