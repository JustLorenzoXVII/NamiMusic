/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.rukamori.archivetune

import moe.rukamori.archivetune.compat.Context

/** LeakCanary esiste solo nelle build di debug Android dell'originale: qui non fa nulla. */
object LeakCanaryController {
    fun setEnabled(
        context: Context,
        enabled: Boolean,
    ) = Unit
}

internal object LeakCanaryToggle {
    const val PREFERENCE_KEY = "dev_enable_leak_canary"
}
