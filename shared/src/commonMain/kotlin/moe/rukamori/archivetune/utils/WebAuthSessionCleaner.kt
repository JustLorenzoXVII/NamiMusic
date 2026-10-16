/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.rukamori.archivetune.utils

import moe.rukamori.archivetune.compat.Context

/** Cancella cookie e dati della WebView di login (WebView su Android, WKWebView su iPhone). */
expect fun clearPlaybackWebAuthSession(context: Context)

expect suspend fun clearWebAuthSession(context: Context)
