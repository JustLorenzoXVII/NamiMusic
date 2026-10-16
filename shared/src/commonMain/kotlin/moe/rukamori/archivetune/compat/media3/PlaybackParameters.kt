package moe.rukamori.archivetune.compat.media3

/** Velocità e tonalità di riproduzione (androidx.media3.common.PlaybackParameters). */
data class PlaybackParameters(
    val speed: Float,
    val pitch: Float = 1f,
) {
    fun withSpeed(speed: Float) = copy(speed = speed)

    companion object {
        val DEFAULT = PlaybackParameters(1f, 1f)
    }
}
