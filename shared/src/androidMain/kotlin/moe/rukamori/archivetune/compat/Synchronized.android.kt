package moe.rukamori.archivetune.compat

actual class PlatformRecursiveLock actual constructor() {
    private val delegate = java.util.concurrent.locks.ReentrantLock()

    actual fun lock() = delegate.lock()

    actual fun unlock() = delegate.unlock()
}
