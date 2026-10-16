package moe.rukamori.archivetune.ui.screens.settings

import android.os.Message
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import archivetunex.shared.generated.resources.*
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import moe.rukamori.archivetune.LocalPlayerAwareWindowInsets
import moe.rukamori.archivetune.compat.BackHandler
import moe.rukamori.archivetune.compat.Intent
import moe.rukamori.archivetune.compat.Uri
import moe.rukamori.archivetune.compat.startActivity
import moe.rukamori.archivetune.constants.ListenBrainzEnabledKey
import moe.rukamori.archivetune.constants.ListenBrainzTokenKey
import moe.rukamori.archivetune.constants.ShowSpotifyPlaylistsKey
import moe.rukamori.archivetune.spotify.SpotifyAccountUiState
import moe.rukamori.archivetune.spotify.SpotifyAccountViewModel
import moe.rukamori.archivetune.spotify.SpotifyAuth
import moe.rukamori.archivetune.ui.component.DefaultDialog
import moe.rukamori.archivetune.ui.component.IconButton
import moe.rukamori.archivetune.ui.component.InfoLabel
import moe.rukamori.archivetune.ui.component.PreferenceEntry
import moe.rukamori.archivetune.ui.component.PreferenceGroup
import moe.rukamori.archivetune.ui.component.PreferenceGroupScope
import moe.rukamori.archivetune.ui.component.SwitchPreference
import moe.rukamori.archivetune.ui.component.TextFieldDialog
import moe.rukamori.archivetune.ui.utils.backToMain
import moe.rukamori.archivetune.utils.rememberPreference
import moe.rukamori.archivetune.utils.resetAuthWebViewSession
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import android.os.Message
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout

@Composable
internal actual fun SpotifyLoginSheet(
    onDismiss: () -> Unit,
    onCookiesCaptured: (spDc: String, spKey: String) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var webView by remember { mutableStateOf<WebView?>(null) }
    var mainWebView by remember { mutableStateOf<WebView?>(null) }
    var captured by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose {
            webView?.destroySpotifyLoginWebView()
            mainWebView?.takeIf { it !== webView }?.destroySpotifyLoginWebView()
            webView = null
            mainWebView = null
        }
    }

    BackHandler(enabled = webView != null) {
        val activeWebView = webView
        val rootWebView = mainWebView
        when {
            activeWebView?.canGoBack() == true -> {
                activeWebView.goBack()
            }

            activeWebView != null && rootWebView != null && activeWebView !== rootWebView -> {
                activeWebView.destroySpotifyLoginWebView()
                webView = rootWebView
            }

            else -> {
                onDismiss()
            }
        }
    }

    ModalBottomSheet(
        modifier = Modifier.fillMaxHeight(),
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(Res.string.spotify_login_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(Res.string.spotify_waiting_for_login),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            AndroidView(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(MaterialTheme.shapes.large),
                factory = { context ->
                    val container = FrameLayout(context)
                    val spotifyWebView =
                        WebView(context).apply {
                            val cookieManager = CookieManager.getInstance()
                            cookieManager.setAcceptCookie(true)
                            cookieManager.setAcceptThirdPartyCookies(this, true)
                            configureSpotifyLoginWebView()

                            fun captureCookies(url: String?): Boolean {
                                if (captured) return true
                                val cookies = readSpotifyCookies(cookieManager, url)
                                val spDc = cookies["sp_dc"].orEmpty()
                                if (spDc.isBlank()) return false
                                captured = true
                                cookieManager.flush()
                                onCookiesCaptured(spDc, cookies["sp_key"].orEmpty())
                                return true
                            }

                            webViewClient =
                                object : WebViewClient() {
                                    override fun shouldOverrideUrlLoading(
                                        view: WebView,
                                        request: WebResourceRequest,
                                    ): Boolean =
                                        shouldOverrideSpotifyLoginUrl(
                                            view = view,
                                            url = request.url?.toString(),
                                            captureCookies = { url -> captureCookies(url) },
                                        )

                                    @Deprecated("Deprecated in Java")
                                    override fun shouldOverrideUrlLoading(
                                        view: WebView,
                                        url: String?,
                                    ): Boolean =
                                        shouldOverrideSpotifyLoginUrl(
                                            view = view,
                                            url = url,
                                            captureCookies = { targetUrl -> captureCookies(targetUrl) },
                                        )

                                    override fun onPageStarted(
                                        view: WebView,
                                        url: String?,
                                        favicon: android.graphics.Bitmap?,
                                    ) {
                                        captureCookies(url)
                                    }

                                    override fun onPageFinished(
                                        view: WebView,
                                        url: String?,
                                    ) {
                                        captureCookies(url)
                                    }
                                }
                            webChromeClient =
                                SpotifyLoginWebChromeClient(
                                    container = container,
                                    parentWebView = this,
                                    captureCookies = { url -> captureCookies(url) },
                                    onActiveWebViewChanged = { activeWebView -> webView = activeWebView },
                                )
                            webView = this
                            mainWebView = this
                            resetAuthWebViewSession(this) {
                                loadUrl(SpotifyAuth.LOGIN_URL)
                            }
                        }
                    container.addView(
                        spotifyWebView,
                        FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT,
                        ),
                    )
                    container
                },
                update = {
                    webView = webView ?: mainWebView
                },
            )
        }
    }
}

private fun WebView.destroySpotifyLoginWebView() {
    stopLoading()
    loadUrl("about:blank")
    (parent as? ViewGroup)?.removeView(this)
    destroy()
}

private fun WebView.configureSpotifyLoginWebView() {
    settings.apply {
        javaScriptEnabled = true
        domStorageEnabled = true
        javaScriptCanOpenWindowsAutomatically = true
        setSupportMultipleWindows(true)
        setSupportZoom(true)
        builtInZoomControls = true
        displayZoomControls = false
        mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
        userAgentString = SpotifyLoginUserAgent
    }
}

private class SpotifyLoginWebChromeClient(
    private val container: FrameLayout,
    private val parentWebView: WebView,
    private val captureCookies: (String?) -> Boolean,
    private val onActiveWebViewChanged: (WebView) -> Unit,
) : WebChromeClient() {
    override fun onCreateWindow(
        view: WebView,
        isDialog: Boolean,
        isUserGesture: Boolean,
        resultMsg: Message,
    ): Boolean {
        closePopupWebViews()

        val popupWebView =
            WebView(view.context).apply {
                val cookieManager = CookieManager.getInstance()
                cookieManager.setAcceptCookie(true)
                cookieManager.setAcceptThirdPartyCookies(this, true)
                configureSpotifyLoginWebView()
                webViewClient =
                    object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(
                            view: WebView,
                            request: WebResourceRequest,
                        ): Boolean =
                            shouldOverrideSpotifyLoginUrl(
                                view = view,
                                url = request.url?.toString(),
                                captureCookies = captureCookies,
                            )

                        @Deprecated("Deprecated in Java")
                        override fun shouldOverrideUrlLoading(
                            view: WebView,
                            url: String?,
                        ): Boolean =
                            shouldOverrideSpotifyLoginUrl(
                                view = view,
                                url = url,
                                captureCookies = captureCookies,
                            )

                        override fun onPageStarted(
                            view: WebView,
                            url: String?,
                            favicon: android.graphics.Bitmap?,
                        ) {
                            captureCookies(url)
                        }

                        override fun onPageFinished(
                            view: WebView,
                            url: String?,
                        ) {
                            captureCookies(url)
                        }
                    }
            }

        val transport = resultMsg.obj as? WebView.WebViewTransport ?: return false
        container.addView(
            popupWebView,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            ),
        )
        popupWebView.bringToFront()
        popupWebView.requestFocus()
        onActiveWebViewChanged(popupWebView)
        transport.webView = popupWebView
        resultMsg.sendToTarget()
        return true
    }

    override fun onCloseWindow(window: WebView) {
        window.destroySpotifyLoginWebView()
        onActiveWebViewChanged(parentWebView)
    }

    private fun closePopupWebViews() {
        for (index in container.childCount - 1 downTo 0) {
            val child = container.getChildAt(index) as? WebView ?: continue
            if (child !== parentWebView) {
                child.destroySpotifyLoginWebView()
            }
        }
        onActiveWebViewChanged(parentWebView)
    }
}

private fun shouldOverrideSpotifyLoginUrl(
    view: WebView,
    url: String?,
    captureCookies: (String?) -> Boolean,
): Boolean {
    if (captureCookies(url)) return true

    val targetUrl = url?.takeIf(String::isNotBlank) ?: return false
    if (targetUrl.isWebViewLoadableUrl()) return false

    targetUrl.intentBrowserFallbackUrl()?.let { fallbackUrl -> view.loadUrl(fallbackUrl) }
    return true
}

private fun String.isWebViewLoadableUrl(): Boolean {
    val scheme = runCatching { Uri.parse(this).scheme?.lowercase() }.getOrNull()
    return scheme == "http" ||
        scheme == "https" ||
        scheme == "javascript" ||
        scheme == "data" ||
        scheme == "blob"
}

private fun String.intentBrowserFallbackUrl(): String? =
    runCatching { Intent.parseUri(this, Intent.URI_INTENT_SCHEME) }
        .getOrNull()
        ?.getStringExtra("browser_fallback_url")
        ?.takeIf { it.isWebViewLoadableUrl() }

private fun readSpotifyCookies(
    cookieManager: CookieManager,
    currentUrl: String?,
): Map<String, String> {
    val urls =
        linkedSetOf(
            "https://open.spotify.com",
            "https://accounts.spotify.com",
            "https://spotify.com",
        )
    currentUrl?.toSpotifyCookieOrigin()?.let(urls::add)
    val cookies = linkedMapOf<String, String>()
    cookieManager.flush()
    urls.forEach { url ->
        cookieManager
            .getCookie(url)
            ?.split(";")
            ?.map(String::trim)
            ?.filter(String::isNotBlank)
            ?.forEach { part ->
                val separator = part.indexOf('=')
                if (separator <= 0) return@forEach
                val key = part.substring(0, separator).trim()
                val value = part.substring(separator + 1).trim()
                if (key.isNotBlank()) {
                    cookies[key] = value
                }
            }
    }
    return cookies
}

private fun String.toSpotifyCookieOrigin(): String? {
    val uri = runCatching { Uri.parse(this) }.getOrNull() ?: return null
    val host = uri.host?.lowercase() ?: return null
    if (host != "spotify.com" && !host.endsWith(".spotify.com")) return null
    val scheme =
        uri.scheme
            ?.takeIf { it.equals("https", ignoreCase = true) || it.equals("http", ignoreCase = true) }
            ?: "https"
    return "$scheme://$host"
}

