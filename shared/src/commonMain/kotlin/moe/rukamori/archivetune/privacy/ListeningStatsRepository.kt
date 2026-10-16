/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.rukamori.archivetune.privacy

import kotlinx.coroutines.IO

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import moe.rukamori.archivetune.db.MusicDatabase

class ListeningStatsRepository constructor(
    private val database: MusicDatabase,
) {
    suspend fun reset() = withContext(Dispatchers.IO) {
        database.withTransaction {
            clearListenHistory()
            resetTotalPlayTime()
            clearPlayCounts()
        }
    }
}
