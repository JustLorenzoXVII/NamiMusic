/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.rukamori.archivetune.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import moe.rukamori.archivetune.compat.Uri

/** Navigazione della pagina web di login (indietro). */
class WebNavigator {
    var canGoBack by mutableStateOf(false)
        internal set
    internal var goBackAction: () -> Unit = {}

    fun goBack() = goBackAction()
}

/**
 * Pagina di login Google/YouTube Music: WebView su Android, WKWebView su iPhone.
 * Cattura i cookie di YouTube, il visitorData e il dataSyncId come l'originale.
 */
@Composable
expect fun YouTubeLoginWebView(
    startUrl: String,
    navigator: WebNavigator,
    onCookiesCaptured: (String) -> Unit,
    onVisitorData: (String?) -> Unit,
    onDataSyncId: (String?) -> Unit,
    modifier: Modifier = Modifier,
)

internal const val YOUTUBE_MUSIC_PACKAGE_NAME = "com.google.android.apps.youtube.music"
internal const val LOGIN_CONTEXT_RETRY_DELAY_MS = 1_000L
internal const val LOGIN_CONTEXT_EXTRACTION_ATTEMPTS = 10

/** Script che legge VISITOR_DATA e DATASYNC_ID dalla pagina; [callback] riceve (v, d). */
internal fun loginContextScript(callback: String): String =
    "(function(){try{var c=window.ytcfg;var y=window.yt&&window.yt.config_;var s=document.querySelectorAll('script');var v=c&&c.get&&c.get('VISITOR_DATA')||y&&y.VISITOR_DATA;var d=c&&c.get&&c.get('DATASYNC_ID')||y&&y.DATASYNC_ID;for(var i=0;i<s.length&&(!v||!d);i++){var x=s[i].textContent;if(!v){var vm=x.match(/[\"']VISITOR_DATA[\"']\\s*:\\s*[\"']([^\"']+)[\"']/);if(vm)v=vm[1]}if(!d){var dm=x.match(/[\"'](?:DATASYNC_ID|dataSyncId)[\"']\\s*:\\s*[\"']([^\"']+)[\"']/);if(dm)d=dm[1]}}$callback}catch(e){}})();"

internal val YOUTUBE_COOKIE_URLS =
    listOf(
        "https://music.youtube.com",
        "https://www.youtube.com",
        "https://youtube.com",
    )

internal fun String.isWebViewLoadableUrl(): Boolean {
    val scheme = runCatching { Uri.parse(this).scheme?.lowercase() }.getOrNull()
    return scheme == "http" ||
        scheme == "https" ||
        scheme == "javascript" ||
        scheme == "data" ||
        scheme == "blob"
}

internal fun String.intentUriAsHttpsUrl(): String? {
    val intentUri = runCatching { Uri.parse(this) }.getOrNull() ?: return null
    if (!intentUri.scheme.equals("intent", ignoreCase = true)) return null

    val authority = intentUri.authority ?: return null
    val path = intentUri.encodedPath.orEmpty()
    val query = intentUri.encodedQuery?.let { "?$it" }.orEmpty()
    return "https://$authority$path$query"
}

internal fun String.isHttpUrl(): Boolean {
    val scheme = runCatching { Uri.parse(this).scheme?.lowercase() }.getOrNull()
    return scheme == "http" || scheme == "https"
}

internal fun String.isYouTubeMusicStoreListingUrl(): Boolean {
    val uri = runCatching { Uri.parse(this) }.getOrNull() ?: return false
    return (uri.host.equals("play.google.com", ignoreCase = true) &&
        uri.path.equals("/store/apps/details", ignoreCase = true) &&
        uri.getQueryParameter("id") == YOUTUBE_MUSIC_PACKAGE_NAME) ||
        uri.host.equals("apps.apple.com", ignoreCase = true)
}

internal fun String?.isYouTubeUrl(): Boolean {
    val host = this?.let(Uri::parse)?.host?.lowercase() ?: return false
    return host == "youtube.com" || host.endsWith(".youtube.com")
}

internal fun String?.toYouTubeCookieOrigin(): String? {
    val parsed = this?.let(Uri::parse) ?: return null
    val host = parsed.host?.lowercase() ?: return null
    if (host != "youtube.com" && !host.endsWith(".youtube.com")) return null

    val scheme =
        parsed.scheme
            ?.takeIf { it.equals("https", ignoreCase = true) || it.equals("http", ignoreCase = true) }
            ?: "https"

    return "$scheme://$host"
}

/** Unisce le stringhe "a=b; c=d" dei cookie, tenendo il primo valore di ogni nome. */
internal fun mergeCookieHeaders(headers: List<String?>): String? {
    val cookieParts = linkedMapOf<String, String>()
    headers.forEach { header ->
        header
            ?.split(";")
            ?.map(String::trim)
            ?.filter(String::isNotBlank)
            ?.forEach { part ->
                val separatorIndex = part.indexOf('=')
                if (separatorIndex <= 0) return@forEach
                val key = part.substring(0, separatorIndex).trim()
                val value = part.substring(separatorIndex + 1).trim()
                if (key.isNotEmpty() && value.isNotEmpty() && key !in cookieParts) {
                    cookieParts[key] = value
                }
            }
    }
    return cookieParts
        .takeIf { it.isNotEmpty() }
        ?.entries
        ?.joinToString(separator = "; ") { (key, value) -> "$key=$value" }
}
