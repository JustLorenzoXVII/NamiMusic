package moe.rukamori.archivetune.morideobfuscator.youtubei

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Runnable
import platform.Foundation.*
import kotlin.coroutines.CoroutineContext

/**
 * Un thread dedicato con stack grande (i thread secondari di iOS hanno solo 512 KB,
 * troppo pochi per QuickJS mentre analizza il player di YouTube).
 */
internal actual fun createYoutubeiDispatcher(
    name: String,
    stackSizeBytes: Long,
): CoroutineDispatcher = DedicatedThreadDispatcher(name, stackSizeBytes)

private class DedicatedThreadDispatcher(
    name: String,
    stackSizeBytes: Long,
) : CoroutineDispatcher() {
    private val condition = NSCondition()
    private val queue = ArrayDeque<Runnable>()

    init {
        val thread = NSThread { loop() }
        thread.name = name
        thread.stackSize = stackSizeBytes.toULong()
        thread.start()
    }

    override fun dispatch(
        context: CoroutineContext,
        block: Runnable,
    ) {
        condition.lock()
        queue.addLast(block)
        condition.signal()
        condition.unlock()
    }

    private fun loop() {
        while (true) {
            condition.lock()
            while (queue.isEmpty()) {
                condition.wait()
            }
            val task = queue.removeFirst()
            condition.unlock()
            task.run()
        }
    }
}

internal actual fun youtubeiCacheDirectory(): String {
    val paths = NSSearchPathForDirectoriesInDomains(NSCachesDirectory, NSUserDomainMask, true)
    return paths.firstOrNull() as? String ?: "/tmp"
}

actual fun currentTimeZoneId(): String = NSTimeZone.localTimeZone.name

internal actual suspend fun com.dokar.quickjs.QuickJs.evaluateString(
    code: String,
    filename: String,
): String = evaluate<String>(code, filename, false)

internal actual suspend fun com.dokar.quickjs.QuickJs.evaluateUnit(
    code: String,
    filename: String,
) {
    evaluate<Unit>(code, filename, false)
}
