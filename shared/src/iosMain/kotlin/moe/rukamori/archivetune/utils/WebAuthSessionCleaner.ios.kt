package moe.rukamori.archivetune.utils

import kotlinx.coroutines.suspendCancellableCoroutine
import moe.rukamori.archivetune.compat.Context
import platform.Foundation.NSDate
import platform.WebKit.WKWebsiteDataStore
import kotlin.coroutines.resume

actual fun clearPlaybackWebAuthSession(context: Context) {
    val store = WKWebsiteDataStore.defaultDataStore()
    store.removeDataOfTypes(WKWebsiteDataStore.allWebsiteDataTypes(), modifiedSince = NSDate(timeIntervalSinceReferenceDate = 0.0)) {}
}

actual suspend fun clearWebAuthSession(context: Context) {
    suspendCancellableCoroutine { continuation ->
        val store = WKWebsiteDataStore.defaultDataStore()
        store.removeDataOfTypes(WKWebsiteDataStore.allWebsiteDataTypes(), modifiedSince = NSDate(timeIntervalSinceReferenceDate = 0.0)) {
            if (continuation.isActive) continuation.resume(Unit)
        }
    }
}
