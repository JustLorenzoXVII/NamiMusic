package moe.rukamori.archivetune.logcat

import moe.rukamori.archivetune.utils.LogEntry

internal actual suspend fun platformReadLogcatEntries(
    limit: Int,
    filterSpecs: List<String>,
    isRelevant: (Int, String, String) -> Boolean,
): List<LogEntry> = emptyList()
