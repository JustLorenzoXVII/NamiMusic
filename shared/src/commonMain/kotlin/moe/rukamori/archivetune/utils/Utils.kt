/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.rukamori.archivetune.utils


fun reportException(throwable: Throwable) {
    throwable.printStackTrace()
}


/**
 * Cambia la lingua dell'app. ArchiveTuneX usa le risorse Compose, che seguono la lingua del sistema;
 * la scelta viene salvata nelle impostazioni dall'interfaccia, qui non serve altro.
 */
fun setAppLocale(
    context: moe.rukamori.archivetune.compat.Context,
    locale: moe.rukamori.archivetune.compat.Locale,
) = Unit
