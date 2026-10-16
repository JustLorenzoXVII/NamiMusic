package moe.rukamori.archivetune.utils

import android.webkit.CookieManager
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewDatabase
import com.justlorenzo.archivetunex.player.AndroidAppContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import moe.rukamori.archivetune.compat.Context
import kotlin.coroutines.resume

actual fun clearPlaybackWebAuthSession(context: Context) {
    clearWebAuthStorage()
    val cookieManager = CookieManager.getInstance()
    cookieManager.removeSessionCookies(null)
    cookieManager.removeAllCookies(null)
    cookieManager.flush()
}

actual suspend fun clearWebAuthSession(context: Context) {
    withContext(Dispatchers.Main.immediate) {
        clearWebAuthStorage()
        val cookieManager = CookieManager.getInstance()
        suspendCancellableCoroutine<Unit> { continuation ->
            cookieManager.removeSessionCookies {
                cookieManager.removeAllCookies {
                    cookieManager.flush()
                    if (continuation.isActive) {
                        continuation.resume(Unit)
                    }
                }
            }
        }
    }
}

fun resetAuthWebViewSession(
    webView: WebView,
    clearCookies: Boolean = true,
    onReady: () -> Unit,
) {
    webView.stopLoading()
    webView.clearHistory()
    webView.clearFormData()
    webView.clearCache(true)
    clearWebAuthStorage()

    val cookieManager = CookieManager.getInstance()
    cookieManager.setAcceptCookie(true)
    cookieManager.setAcceptThirdPartyCookies(webView, true)
    if (!clearCookies) {
        onReady()
        return
    }

    cookieManager.removeSessionCookies {
        cookieManager.removeAllCookies {
            cookieManager.flush()
            cookieManager.setAcceptCookie(true)
            cookieManager.setAcceptThirdPartyCookies(webView, true)
            onReady()
        }
    }
}

private fun clearWebAuthStorage() {
    val appContext = AndroidAppContext.context
    WebStorage.getInstance().deleteAllData()
    WebViewDatabase.getInstance(appContext).apply {
        clearFormData()
        clearHttpAuthUsernamePassword()
        clearUsernamePassword()
    }
}
