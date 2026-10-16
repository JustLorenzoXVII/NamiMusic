/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.rukamori.archivetune.backup

import moe.rukamori.archivetune.compat.Context

/**
 * Nell'originale pianificava il backup automatico con WorkManager (Android).
 * Su iPhone le app non possono eseguire lavori pianificati in background in modo affidabile:
 * il backup si fa a mano dalle impostazioni.
 */
class ScheduledBackupScheduler constructor(
    private val context: Context,
) {
    fun replace(settings: ScheduledBackupSettings) = Unit

    fun appendNext(settings: ScheduledBackupSettings) = Unit
}
