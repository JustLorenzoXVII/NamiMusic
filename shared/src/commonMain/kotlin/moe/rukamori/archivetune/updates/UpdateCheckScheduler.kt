/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.rukamori.archivetune.updates

import moe.rukamori.archivetune.compat.Context

/**
 * Nell'originale pianificava con WorkManager il controllo aggiornamenti dell'APK.
 * ArchiveTuneX si aggiorna da TestFlight, quindi qui non c'è nulla da pianificare.
 */
class UpdateCheckScheduler constructor(
    private val context: Context,
) {
    fun replace(enabled: Boolean) = Unit

    companion object {
        fun schedule(context: Context) = Unit

        fun cancel(context: Context) = Unit
    }
}
