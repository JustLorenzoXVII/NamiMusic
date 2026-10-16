/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.rukamori.archivetune.utils.oem

import moe.rukamori.archivetune.compat.Context

/** Apre il selettore dell'uscita audio di sistema (Android); su iPhone si usa AirPlay dal Centro di Controllo. */
expect object SystemMediaControlResolver {
    fun openMediaOutputSwitcher(context: Context)
}
