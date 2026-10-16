package moe.rukamori.archivetune.morideobfuscator.youtubei

import com.justlorenzo.archivetunex.player.AndroidAppContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import java.util.TimeZone
import java.util.concurrent.Executors

internal actual fun createYoutubeiDispatcher(
    name: String,
    stackSizeBytes: Long,
): CoroutineDispatcher =
    Executors
        .newSingleThreadExecutor { runnable ->
            Thread(null, runnable, name, stackSizeBytes).apply { isDaemon = true }
        }.asCoroutineDispatcher()

internal actual fun youtubeiCacheDirectory(): String = AndroidAppContext.context.noBackupFilesDir.absolutePath

actual fun currentTimeZoneId(): String = TimeZone.getDefault().id
