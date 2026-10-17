/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.rukamori.archivetune.localmedia

import moe.rukamori.archivetune.compat.putIfAbsent

import androidx.compose.runtime.Immutable
import moe.rukamori.archivetune.compat.Locale
import moe.rukamori.archivetune.db.MusicDatabase
import moe.rukamori.archivetune.compat.lowercase

data class LocalSongScanConfig(
    val minimumDurationSeconds: Int = 0,
    val includedFolders: Set<String> = emptySet(),
    val excludedFolders: Set<String> = emptySet(),
) {
    val sanitizedMinimumDurationSeconds: Int
        get() = minimumDurationSeconds.coerceAtLeast(0)

    val sanitizedIncludedFolders: Set<String>
        get() = deduplicateFolderEntries(includedFolders)

    val sanitizedExcludedFolders: Set<String>
        get() = deduplicateFolderEntries(excludedFolders)

    companion object {
        private val DuplicateSlashRegex = Regex("/+")

        fun normalizeFolderEntry(raw: String): String =
            raw
                .trim()
                .replace('\\', '/')
                .replace(DuplicateSlashRegex, "/")
                .trim('/')

        fun deduplicateFolderEntries(entries: Iterable<String>): Set<String> {
            val deduplicated = linkedMapOf<String, String>()
            entries.forEach { entry ->
                val normalized = normalizeFolderEntry(entry)
                if (normalized.isNotEmpty()) {
                    deduplicated.putIfAbsent(normalized.lowercase(Locale.ROOT), normalized)
                }
            }
            return deduplicated.values.toSet()
        }
    }
}

@Immutable
data class LocalSongScanSummary(
    val scannedSongs: Int,
    val removedSongs: Int,
    val metadataLookupFailed: Boolean = false,
)

/** Scansione dei brani salvati sul dispositivo (MediaStore su Android; su iPhone non disponibile). */
interface LocalSongScanner {
    suspend fun scanDevice(scanConfig: LocalSongScanConfig = LocalSongScanConfig()): LocalSongScanSummary
}

expect fun createLocalSongScanner(database: MusicDatabase): LocalSongScanner
