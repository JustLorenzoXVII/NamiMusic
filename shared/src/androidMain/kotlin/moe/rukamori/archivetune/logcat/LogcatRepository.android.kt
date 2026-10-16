package moe.rukamori.archivetune.logcat

import android.util.Log
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import moe.rukamori.archivetune.utils.LogEntry
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

internal actual suspend fun platformReadLogcatEntries(
    limit: Int,
    filterSpecs: List<String>,
    isRelevant: (Int, String, String) -> Boolean,
): List<LogEntry> {
    val processId = android.os.Process.myPid()
    val timestampFormat = SimpleDateFormat("MM-dd HH:mm:ss.SSS", Locale.US)
    val currentYear = Calendar.getInstance().get(Calendar.YEAR)
    val linePattern =
        Regex(
            """^(\d{2}-\d{2} \d{2}:\d{2}:\d{2}\.\d{3})\s+([VDIWEF])/""" +
                """(.+?)\(\s*\d+\):\s*(.*)$""",
        )
    val logcatProcess =
        ProcessBuilder(
            buildList {
                add("logcat")
                add("--pid=$processId")
                add("-v")
                add("time")
                add("-t")
                add(limit.toString())
                addAll(filterSpecs)
            },
        ).redirectErrorStream(true)
            .start()

    return try {
        val entries = ArrayList<LogEntry>(limit)
        BufferedReader(InputStreamReader(logcatProcess.inputStream)).use { reader ->
            reader.forEachLine { line ->
                val match = linePattern.matchEntire(line) ?: return@forEachLine
                val (timestamp, levelCode, tag, message) = match.destructured
                val level =
                    when (levelCode) {
                        "V" -> Log.VERBOSE
                        "D" -> Log.DEBUG
                        "I" -> Log.INFO
                        "W" -> Log.WARN
                        "E", "F" -> Log.ERROR
                        else -> return@forEachLine
                    }
                val normalizedTag = tag.trim()
                if (!isRelevant(level, normalizedTag, message)) return@forEachLine
                val parsedTimestamp = timestampFormat.parse(timestamp) ?: return@forEachLine
                val calendar =
                    Calendar.getInstance().apply {
                        time = parsedTimestamp
                        set(Calendar.YEAR, currentYear)
                    }
                entries += LogEntry(time = calendar.timeInMillis, level = level, tag = normalizedTag, message = message)
            }
        }
        val exitCode = logcatProcess.waitFor()
        currentCoroutineContext().ensureActive()
        if (exitCode != 0) throw IOException("logcat exited with code $exitCode")
        entries
    } finally {
        logcatProcess.destroy()
    }
}
