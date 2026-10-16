package moe.rukamori.archivetune.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import moe.rukamori.archivetune.innertube.utils.hasCompleteYouTubeLoginCookies
import platform.Foundation.NSDate
import platform.Foundation.NSHTTPCookie
import platform.Foundation.NSURL
import platform.Foundation.NSURLRequest
import platform.WebKit.WKNavigation
import platform.WebKit.WKNavigationAction
import platform.WebKit.WKNavigationActionPolicy
import platform.WebKit.WKNavigationDelegateProtocol
import platform.WebKit.WKWebView
import platform.WebKit.WKWebViewConfiguration
import platform.WebKit.WKWebsiteDataStore
import platform.darwin.NSObject

private val IOS_LOGIN_CONTEXT_SCRIPT = loginContextScript("return JSON.stringify({v:v||null,d:d||null})")

/** Login Google/YouTube Music con WKWebView (al posto della WebView Android). */
@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun YouTubeLoginWebView(
    startUrl: String,
    navigator: WebNavigator,
    onCookiesCaptured: (String) -> Unit,
    onVisitorData: (String?) -> Unit,
    onDataSyncId: (String?) -> Unit,
    modifier: Modifier,
) {
    val controller =
        remember {
            LoginWebController(navigator, onCookiesCaptured, onVisitorData, onDataSyncId)
        }
    DisposableEffect(controller) {
        onDispose { controller.release() }
    }
    UIKitView(
        factory = { controller.createWebView(startUrl) },
        modifier = modifier,
    )
}

@OptIn(ExperimentalForeignApi::class)
private class LoginWebController(
    private val navigator: WebNavigator,
    private val onCookiesCaptured: (String) -> Unit,
    private val onVisitorData: (String?) -> Unit,
    private val onDataSyncId: (String?) -> Unit,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var extractionJob: Job? = null
    private var lastCapturedCookie: String? = null
    private var webView: WKWebView? = null

    private val delegate =
        object : NSObject(), WKNavigationDelegateProtocol {
            override fun webView(
                webView: WKWebView,
                didFinishNavigation: WKNavigation?,
            ) {
                navigator.canGoBack = webView.canGoBack
                scheduleExtraction(webView)
            }

            override fun webView(
                webView: WKWebView,
                decidePolicyForNavigationAction: WKNavigationAction,
                decisionHandler: (WKNavigationActionPolicy) -> Unit,
            ) {
                val url = decidePolicyForNavigationAction.request.URL?.absoluteString
                val allow =
                    when {
                        url == null -> true
                        url.isYouTubeMusicStoreListingUrl() -> false
                        url.isWebViewLoadableUrl() || url.startsWith("about:") -> true
                        else -> {
                            url.intentUriAsHttpsUrl()?.let { https ->
                                NSURL.URLWithString(https)?.let { webView.loadRequest(NSURLRequest.requestWithURL(it)) }
                            }
                            false
                        }
                    }
                decisionHandler(if (allow) WKNavigationActionPolicy.WKNavigationActionPolicyAllow else WKNavigationActionPolicy.WKNavigationActionPolicyCancel)
            }
        }

    fun createWebView(startUrl: String): WKWebView {
        val configuration = WKWebViewConfiguration()
        configuration.websiteDataStore = WKWebsiteDataStore.defaultDataStore()
        val view = WKWebView(frame = platform.CoreGraphics.CGRectMake(0.0, 0.0, 0.0, 0.0), configuration = configuration)
        view.navigationDelegate = delegate
        view.allowsBackForwardNavigationGestures = true
        webView = view
        navigator.goBackAction = { if (view.canGoBack) view.goBack() }
        // Come l'originale: si parte da una sessione pulita.
        WKWebsiteDataStore.defaultDataStore().removeDataOfTypes(
            WKWebsiteDataStore.allWebsiteDataTypes(),
            modifiedSince = NSDate(timeIntervalSinceReferenceDate = 0.0),
        ) {
            NSURL.URLWithString(startUrl)?.let { view.loadRequest(NSURLRequest.requestWithURL(it)) }
        }
        return view
    }

    private fun scheduleExtraction(view: WKWebView) {
        extractionJob?.cancel()
        if (!view.URL?.absoluteString.isYouTubeUrl()) return
        extractionJob =
            scope.launch {
                repeat(LOGIN_CONTEXT_EXTRACTION_ATTEMPTS) {
                    if (!view.URL?.absoluteString.isYouTubeUrl()) return@launch
                    view.evaluateJavaScript(IOS_LOGIN_CONTEXT_SCRIPT) { result, _ ->
                        val json = result as? String ?: return@evaluateJavaScript
                        runCatching {
                            val obj = Json.parseToJsonElement(json).jsonObject
                            obj["v"]?.jsonPrimitive?.takeIf { it.isString }?.content?.let(onVisitorData)
                            obj["d"]?.jsonPrimitive?.takeIf { it.isString }?.content?.let(onDataSyncId)
                        }
                    }
                    captureCookies(view)
                    delay(LOGIN_CONTEXT_RETRY_DELAY_MS)
                }
            }
    }

    private fun captureCookies(view: WKWebView) {
        view.configuration.websiteDataStore.httpCookieStore.getAllCookies { cookies ->
            val header =
                cookies
                    .orEmpty()
                    .filterIsInstance<NSHTTPCookie>()
                    .filter { it.domain.trimStart('.').let { d -> d == "youtube.com" || d.endsWith(".youtube.com") } }
                    .joinToString("; ") { "${it.name}=${it.value}" }
            val merged = mergeCookieHeaders(listOf(header)) ?: return@getAllCookies
            if (!hasCompleteYouTubeLoginCookies(merged) || merged == lastCapturedCookie) return@getAllCookies
            lastCapturedCookie = merged
            onCookiesCaptured(merged)
        }
    }

    fun release() {
        extractionJob?.cancel()
        webView?.navigationDelegate = null
        webView?.stopLoading()
        webView = null
        navigator.goBackAction = {}
        navigator.canGoBack = false
    }
}
