package moe.rukamori.archivetune.ui.screens

import android.annotation.SuppressLint
import android.content.Intent
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import moe.rukamori.archivetune.innertube.utils.hasCompleteYouTubeLoginCookies
import moe.rukamori.archivetune.utils.resetAuthWebViewSession

@SuppressLint("SetJavaScriptEnabled")
@Composable
actual fun YouTubeLoginWebView(
    startUrl: String,
    navigator: WebNavigator,
    onCookiesCaptured: (String) -> Unit,
    onVisitorData: (String?) -> Unit,
    onDataSyncId: (String?) -> Unit,
    modifier: Modifier,
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                val cookieManager = CookieManager.getInstance()
                webViewClient =
                    YouTubeLoginWebViewClient(
                        cookieManager = cookieManager,
                        onCookiesCaptured = onCookiesCaptured,
                        onHistoryChanged = { view -> navigator.canGoBack = view.canGoBack() },
                    )
                settings.apply {
                    javaScriptEnabled = true
                    setSupportZoom(true)
                    builtInZoomControls = true
                    displayZoomControls = false
                }
                val loginWebView = this
                addJavascriptInterface(
                    object {
                        @JavascriptInterface
                        fun onRetrieveVisitorData(newVisitorData: String?) {
                            loginWebView.post { onVisitorData(newVisitorData) }
                        }

                        @JavascriptInterface
                        fun onRetrieveDataSyncId(newDataSyncId: String?) {
                            loginWebView.post { onDataSyncId(newDataSyncId) }
                        }
                    },
                    "Android",
                )
                navigator.goBackAction = { if (loginWebView.canGoBack()) loginWebView.goBack() }
                resetAuthWebViewSession(this, clearCookies = true) {
                    loadUrl(startUrl)
                }
            }
        },
        onRelease = { releasedWebView ->
            (releasedWebView.webViewClient as? YouTubeLoginWebViewClient)?.release(releasedWebView)
            releasedWebView.removeJavascriptInterface("Android")
            releasedWebView.stopLoading()
            releasedWebView.destroy()
            navigator.goBackAction = {}
            navigator.canGoBack = false
        },
    )
}

private val ANDROID_LOGIN_CONTEXT_SCRIPT =
    loginContextScript("if(v)Android.onRetrieveVisitorData(v);if(d)Android.onRetrieveDataSyncId(d)")

private class YouTubeLoginWebViewClient(
    private val cookieManager: CookieManager,
    private val onCookiesCaptured: (String) -> Unit,
    private val onHistoryChanged: (WebView) -> Unit,
) : WebViewClient() {
    private var extractionRunnable: Runnable? = null
    private var lastCapturedCookie: String? = null

    override fun shouldOverrideUrlLoading(
        view: WebView,
        request: WebResourceRequest,
    ): Boolean {
        val targetUrl = request.url.toString()
        if (!request.isForMainFrame && !targetUrl.isWebViewLoadableUrl()) return true
        return handleUrlLoading(view, targetUrl)
    }

    @Deprecated("Deprecated in Java")
    override fun shouldOverrideUrlLoading(
        view: WebView,
        url: String?,
    ): Boolean = handleUrlLoading(view, url)

    override fun onPageFinished(
        view: WebView,
        url: String?,
    ) {
        super.onPageFinished(view, url)
        onHistoryChanged(view)
        scheduleLoginContextExtraction(view, url)
    }

    override fun doUpdateVisitedHistory(
        view: WebView,
        url: String?,
        isReload: Boolean,
    ) {
        super.doUpdateVisitedHistory(view, url, isReload)
        onHistoryChanged(view)
        scheduleLoginContextExtraction(view, url)
    }

    fun release(view: WebView) {
        extractionRunnable?.let(view::removeCallbacks)
        extractionRunnable = null
    }

    private fun scheduleLoginContextExtraction(
        view: WebView,
        url: String?,
    ) {
        extractionRunnable?.let(view::removeCallbacks)
        extractionRunnable = null

        if (!url.isYouTubeUrl()) return

        var remainingAttempts = LOGIN_CONTEXT_EXTRACTION_ATTEMPTS
        val runnable =
            object : Runnable {
                override fun run() {
                    if (!view.url.isYouTubeUrl()) return

                    view.evaluateJavascript(ANDROID_LOGIN_CONTEXT_SCRIPT, null)
                    captureCookies(view.url)

                    remainingAttempts -= 1
                    if (remainingAttempts > 0) {
                        view.postDelayed(this, LOGIN_CONTEXT_RETRY_DELAY_MS)
                    }
                }
            }
        extractionRunnable = runnable
        view.post(runnable)
    }

    private fun captureCookies(currentUrl: String?) {
        cookieManager.flush()
        val urls = linkedSetOf<String>()
        currentUrl.toYouTubeCookieOrigin()?.let(urls::add)
        urls.addAll(YOUTUBE_COOKIE_URLS)
        val mergedCookie = mergeCookieHeaders(urls.map { cookieManager.getCookie(it) }) ?: return
        if (!hasCompleteYouTubeLoginCookies(mergedCookie) || mergedCookie == lastCapturedCookie) return

        lastCapturedCookie = mergedCookie
        onCookiesCaptured(mergedCookie)
    }

    private fun handleUrlLoading(
        view: WebView,
        url: String?,
    ): Boolean {
        val targetUrl = url?.takeIf(String::isNotBlank) ?: return false
        if (targetUrl.isYouTubeMusicStoreListingUrl()) return true
        if (targetUrl.isWebViewLoadableUrl()) return false

        targetUrl.intentWebViewUrl()?.let(view::loadUrl)
        return true
    }
}

private fun String.intentWebViewUrl(): String? {
    val parsedIntent =
        runCatching { Intent.parseUri(this, Intent.URI_INTENT_SCHEME) }.getOrNull()
    return sequenceOf(
        parsedIntent?.dataString,
        intentUriAsHttpsUrl(),
        parsedIntent
            ?.getStringExtra("browser_fallback_url")
            ?.takeUnless(String::isYouTubeMusicStoreListingUrl),
    ).firstOrNull { it?.isHttpUrl() == true }
}
