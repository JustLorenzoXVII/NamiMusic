package moe.rukamori.archivetune.compat

import platform.Foundation.NSProcessInfo

/** Su iPhone la rete è gestita dal sistema: si assume connessi e senza limiti di traffico. */
actual object NetworkState {
    actual fun isConnected(): Boolean = true

    actual fun isMetered(): Boolean = false

    actual fun isCellular(): Boolean = false

    actual fun observe(onChange: () -> Unit): () -> Unit = {}
}

actual fun platformIsLowRamDevice(): Boolean = NSProcessInfo.processInfo.physicalMemory < 3_000_000_000uL
