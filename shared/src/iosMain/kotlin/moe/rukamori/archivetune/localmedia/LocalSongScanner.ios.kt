package moe.rukamori.archivetune.localmedia

import moe.rukamori.archivetune.db.MusicDatabase

/** Su iPhone le app non possono leggere la libreria musicale come file: nessun brano locale. */
actual fun createLocalSongScanner(database: MusicDatabase): LocalSongScanner =
    object : LocalSongScanner {
        override suspend fun scanDevice(scanConfig: LocalSongScanConfig): LocalSongScanSummary = LocalSongScanSummary(scannedSongs = 0, removedSongs = 0)
    }
