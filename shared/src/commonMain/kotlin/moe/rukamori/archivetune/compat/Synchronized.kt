package moe.rukamori.archivetune.compat

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * synchronized(lock) { } di Kotlin/JVM, che su iPhone non esiste.
 * Usa un unico lock rientrante per tutta l'app: le sezioni protette in ArchiveTune sono brevi
 * (cache in memoria), quindi serializzarle tutte non pesa e non cambia il comportamento.
 */
expect class PlatformRecursiveLock() {
    fun lock()

    fun unlock()
}

@PublishedApi
internal val globalSynchronizedLock = PlatformRecursiveLock()

@OptIn(ExperimentalContracts::class)
inline fun <R> synchronized(
    @Suppress("UNUSED_PARAMETER") lock: Any,
    block: () -> R,
): R {
    contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
    globalSynchronizedLock.lock()
    try {
        return block()
    } finally {
        globalSynchronizedLock.unlock()
    }
}
