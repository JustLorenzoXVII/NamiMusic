/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.rukamori.archivetune.logcat

import moe.rukamori.archivetune.compat.Charsets

import kotlinx.coroutines.IO

import moe.rukamori.archivetune.compat.System

import moe.rukamori.archivetune.compat.Context
import moe.rukamori.archivetune.compat.Uri
import moe.rukamori.archivetune.compat.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import moe.rukamori.archivetune.utils.GlobalLog
import moe.rukamori.archivetune.utils.LogEntry
import moe.rukamori.archivetune.compat.BufferedReader
import moe.rukamori.archivetune.compat.File
import kotlinx.io.IOException
import moe.rukamori.archivetune.compat.SimpleDateFormat
import moe.rukamori.archivetune.compat.Locale
import moe.rukamori.archivetune.compat.uppercase
import moe.rukamori.archivetune.compat.lowercase
import kotlin.math.abs
import moe.rukamori.archivetune.compat.bufferedWriter

private const val LOGCAT_POLL_INTERVAL_MILLIS = 2_000L
private const val LOGCAT_ENTRY_LIMIT = 1_000
private const val LOGCAT_CHUNK_LENGTH = 4_000
private const val DUPLICATE_WINDOW_MILLIS = 1_000L

private val LOGCAT_FILTER_SPECS =
    listOf(
        "AndroidRuntime:E",
        "LeakCanaryController:W",
        "MusicDatabase:V",
        "MusicWidgetActions:E",
        "RLog:I",
        "System.err:V",
        "System.out:V",
        "libc:F",
        "*:S",
    )

class LogcatRepository constructor(
        private val context: Context,
    ) {
        private val clearTimestamp = MutableStateFlow(0L)

        fun observeEntries(): Flow<List<LogEntry>> =
            combine(
                GlobalLog.logs,
                observeLogcatSnapshots(),
                clearTimestamp,
            ) { capturedEntries, logcatEntries, clearedAt ->
                mergeCapturedAndLogcatEntries(
                    capturedEntries = capturedEntries,
                    logcatEntries = logcatEntries,
                ).asSequence()
                    .filter { entry -> entry.time >= clearedAt }
                    .sortedBy(LogEntry::time)
                    .toList()
            }.flowOn(Dispatchers.Default)

        fun clear() {
            clearTimestamp.value = System.currentTimeMillis()
            GlobalLog.clear()
        }

        suspend fun exportText(content: String): Uri =
            withContext(Dispatchers.IO) {
                require(content.isNotBlank())

                val exportDirectory = File(context.cacheDir, "shared_logs")
                if (!exportDirectory.exists() && !exportDirectory.mkdirs()) {
                    throw IOException("Unable to create log export directory")
                }

                val exportFile =
                    File(
                        exportDirectory,
                        "archivetune-log-${System.currentTimeMillis()}.txt",
                    )
                exportFile.bufferedWriter(Charsets.UTF_8).use { writer ->
                    writer.write(content)
                    writer.newLine()
                }

                exportDirectory
                    .listFiles { file -> file.isFile && file.extension.equals("txt", ignoreCase = true) }
                    ?.sortedByDescending(File::lastModified)
                    ?.drop(5)
                    ?.forEach { file -> file.delete() }

                Uri.parse("file://" + exportFile.absolutePath)
            }

        private fun observeLogcatSnapshots(): Flow<List<LogEntry>> =
            flow {
                while (currentCoroutineContext().isActive) {
                    emit(readLogcatEntries())
                    delay(LOGCAT_POLL_INTERVAL_MILLIS)
                }
            }.flowOn(Dispatchers.IO)

        // Logcat di sistema: solo su Android (su iPhone restano i log raccolti dall'app).
        private suspend fun readLogcatEntries(): List<LogEntry> =
            platformReadLogcatEntries(LOGCAT_ENTRY_LIMIT, LOGCAT_FILTER_SPECS) { level, tag, message ->
                isRelevantLogcatEntry(level, tag, message)
            }
    }

internal expect suspend fun platformReadLogcatEntries(
    limit: Int,
    filterSpecs: List<String>,
    isRelevant: (Int, String, String) -> Boolean,
): List<LogEntry>

private fun isRelevantLogcatEntry(
    level: Int,
    tag: String,
    message: String,
): Boolean =
    when (tag) {
        "AndroidRuntime", "libc" -> level >= Log.ERROR
        "LeakCanaryController", "MusicDatabase", "MusicWidgetActions", "RLog" -> true
        "System.err" -> message.startsWith("PaxsenixLyrics:")
        "System.out" ->
            message.startsWith("AppleMusicCanvas:") ||
                message.startsWith("Error converting chart item:") ||
                message.startsWith("Error converting two row item:")

        else -> false
    }

private fun mergeCapturedAndLogcatEntries(
    capturedEntries: List<LogEntry>,
    logcatEntries: List<LogEntry>,
): List<LogEntry> {
    val consumedLogcatEntries = BooleanArray(logcatEntries.size)

    capturedEntries.forEach { capturedEntry ->
        capturedEntry.message.toLogcatChunks().forEach { chunk ->
            val matchingIndex =
                logcatEntries.indices.firstOrNull { index ->
                    if (consumedLogcatEntries[index]) {
                        return@firstOrNull false
                    }

                    val logcatEntry = logcatEntries[index]
                    logcatEntry.level == capturedEntry.level &&
                        (capturedEntry.tag == null || logcatEntry.tag == capturedEntry.tag) &&
                        logcatEntry.message == chunk &&
                        abs(logcatEntry.time - capturedEntry.time) <= DUPLICATE_WINDOW_MILLIS
                }
            if (matchingIndex != null) {
                consumedLogcatEntries[matchingIndex] = true
            }
        }
    }

    val uniqueLogcatEntries =
        logcatEntries.filterIndexed { index, _ ->
            !consumedLogcatEntries[index]
        }

    return capturedEntries + uniqueLogcatEntries.mergeContinuationChunks()
}

private fun String.toLogcatChunks(): List<String> {
    if (length < LOGCAT_CHUNK_LENGTH) {
        return listOf(this)
    }

    val chunks = mutableListOf<String>()
    var start = 0
    while (start < length) {
        val newlineIndex = indexOf('\n', start).let { index -> if (index == -1) length else index }
        do {
            val end = minOf(newlineIndex, start + LOGCAT_CHUNK_LENGTH)
            chunks += substring(start, end)
            start = end
        } while (start < newlineIndex)
        start++
    }
    return chunks
}

private fun List<LogEntry>.mergeContinuationChunks(): List<LogEntry> {
    val mergedEntries = ArrayList<LogEntry>(size)
    sortedBy(LogEntry::time).forEach { entry ->
        val previous = mergedEntries.lastOrNull()
        if (
            previous != null &&
            previous.message.length >= LOGCAT_CHUNK_LENGTH &&
            previous.level == entry.level &&
            previous.tag == entry.tag &&
            entry.time - previous.time in 0..DUPLICATE_WINDOW_MILLIS
        ) {
            mergedEntries[mergedEntries.lastIndex] =
                previous.copy(message = previous.message + entry.message)
        } else {
            mergedEntries += entry
        }
    }
    return mergedEntries
}
