package moe.rukamori.archivetune.compat

import platform.Foundation.NSRecursiveLock

actual class PlatformRecursiveLock actual constructor() {
    private val delegate = NSRecursiveLock()

    actual fun lock() = delegate.lock()

    actual fun unlock() = delegate.unlock()
}
