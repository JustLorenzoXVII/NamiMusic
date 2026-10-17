package moe.rukamori.archivetune.compat

import kotlin.time.Clock
import kotlin.time.TimeSource

/** Sostituto multipiattaforma di java.lang.System per il codice ArchiveTune. */
object System {
    private val start = TimeSource.Monotonic.markNow()

    fun currentTimeMillis(): Long = Clock.System.now().toEpochMilliseconds()

    fun nanoTime(): Long = start.elapsedNow().inWholeNanoseconds

    /** System.out / System.err: scrivono nel log della console. */
    object out {
        fun println(message: Any?) = kotlin.io.println(message)
    }

    object err {
        fun println(message: Any?) = kotlin.io.println(message)
    }

    fun lineSeparator(): String = "\n"

    fun arraycopy(
        src: IntArray,
        srcPos: Int,
        dest: IntArray,
        destPos: Int,
        length: Int,
    ) {
        src.copyInto(dest, destPos, srcPos, srcPos + length)
    }

    fun arraycopy(
        src: ByteArray,
        srcPos: Int,
        dest: ByteArray,
        destPos: Int,
        length: Int,
    ) {
        src.copyInto(dest, destPos, srcPos, srcPos + length)
    }

    fun arraycopy(
        src: FloatArray,
        srcPos: Int,
        dest: FloatArray,
        destPos: Int,
        length: Int,
    ) {
        src.copyInto(dest, destPos, srcPos, srcPos + length)
    }

    fun arraycopy(
        src: DoubleArray,
        srcPos: Int,
        dest: DoubleArray,
        destPos: Int,
        length: Int,
    ) {
        src.copyInto(dest, destPos, srcPos, srcPos + length)
    }
}

/** Ferma la riproduzione prima di un ripristino del backup (nell'originale: stopService(MusicService)). */
fun stopPlaybackForRestore() {
    moe.rukamori.archivetune.playback.MusicServiceHolder.instance?.stopAndClearPlayback(clearPersistentState = false)
}

/**
 * Dopo un ripristino l'originale riapre MainActivity e chiude il processo.
 * Su Android si riavvia l'app; su iPhone l'app si chiude e va riaperta.
 */
fun restartAppAfterRestore() = platformRestartApp()

internal expect fun platformRestartApp()
