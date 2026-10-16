/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.rukamori.archivetune.canvas

import kotlin.concurrent.Volatile

import moe.rukamori.archivetune.compat.okhttp3.Interceptor
import moe.rukamori.archivetune.compat.okhttp3.Response

object CanvasRequestPolicy {
    @Volatile
    var check: (CanvasSource) -> Unit = {}

    @Volatile
    var intercept: (Interceptor.Chain, CanvasSource) -> Response = { chain, _ -> chain.proceed(chain.request()) }
}
