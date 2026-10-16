package moe.rukamori.archivetune.compat

import kotlin.time.Clock
import kotlin.time.TimeSource

/** Sostituto multipiattaforma di java.lang.System per il codice ArchiveTune. */
object System {
    private val start = TimeSource.Monotonic.markNow()

    fun currentTimeMillis(): Long = Clock.System.now().toEpochMilliseconds()

    fun nanoTime(): Long = start.elapsedNow().inWholeNanoseconds
}
