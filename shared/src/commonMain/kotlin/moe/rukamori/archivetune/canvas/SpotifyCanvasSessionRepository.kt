/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.rukamori.archivetune.canvas

import kotlinx.coroutines.IO

import kotlin.concurrent.Volatile

import moe.rukamori.archivetune.compat.System

import moe.rukamori.archivetune.compat.Context
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json
import moe.rukamori.archivetune.constants.SpotifySpDcKey
import moe.rukamori.archivetune.spotify.models.SpotifyInternalToken
import moe.rukamori.archivetune.utils.dataStore
import moe.rukamori.archivetune.compat.Timber
import moe.rukamori.archivetune.compat.ByteArrayInputStream
import kotlinx.io.IOException
import kotlin.coroutines.resume

class SpotifyCanvasSessionRepository constructor(
    private val context: Context,
) {
    private val credentials = context.dataStore.data.map { it[SpotifySpDcKey].orEmpty() }.distinctUntilChanged()
        .onEach { if (cached?.credential != it) cached = null }
    val connected = credentials.map(String::isNotBlank).distinctUntilChanged()
    private val mutex = Mutex()
    @Volatile
    private var cached: CachedSession? = null
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun <T> withSession(block: suspend (SpotifyInternalToken) -> T): T = withContext(Dispatchers.IO) {
        mutex.withLock {
            val credential = credentials.first().takeIf(String::isNotBlank)
                ?: throw IOException("Spotify is not connected")
            coroutineScope {
                val work = async {
                    for (attempt in 0..1) {
                        CanvasNetworkAccess.check(CanvasSource.SPOTIFY)
                        val session = cached?.takeIf {
                            it.credential == credential && it.token.accessTokenExpirationTimestampMs > System.currentTimeMillis() + 30_000
                        }?.token ?: harvest(credential).also {
                            if (credentials.first() != credential) throw CancellationException("Spotify account changed")
                            cached = CachedSession(credential, it)
                        }
                        try {
                            val result = block(session)
                            if (credentials.first() != credential) throw CancellationException("Spotify account changed")
                            CanvasNetworkAccess.check(CanvasSource.SPOTIFY)
                            return@async result
                        } catch (error: SpotifyCanvasProvider.RequestException) {
                            if (error.statusCode != 401 || attempt > 0) throw error
                            cached = null
                        }
                    }
                    throw IOException("Spotify session could not be established")
                }
                val accountWatcher = launch {
                    credentials.first { it != credential }
                    cached = null
                    work.cancel(CancellationException("Spotify account changed"))
                }
                val policyWatcher = launch {
                    CanvasNetworkAccess.policy.first { !it.networkAllowed || !it.configuration.source.accepts(CanvasSource.SPOTIFY) }
                    work.cancel(CancellationException("Spotify Canvas network access changed"))
                }
                try {
                    work.await()
                } finally {
                    accountWatcher.cancel()
                    policyWatcher.cancel()
                }
            }
        }
    }

    /**
     * Nell'originale il token di Spotify si otteneva aprendo open.spotify.com in una WebView Android.
     * Non è ancora disponibile su iPhone: le copertine animate di Spotify restano disattivate.
     */
    private suspend fun harvest(credential: String): SpotifyInternalToken =
        throw IOException("Spotify Canvas non disponibile su questa piattaforma")

    private data class CachedSession(val credential: String, val token: SpotifyInternalToken)

    private companion object {
        const val ORIGIN = "https://open.spotify.com"
        const val BRIDGE = "ArchiveTuneCanvasSession"
        const val USER_AGENT = "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36"
        val allowedOrigins = setOf(ORIGIN)
        val allowedHosts = setOf("open.spotify.com", "accounts.spotify.com")
        val TOKEN_SCRIPT = """
            (() => {
                if (window.__archiveTuneCanvasSession) return;
                window.__archiveTuneCanvasSession = true;
                const matches = value => {
                    try {
                        const url = new URL(value, location.href);
                        return url.origin === 'https://open.spotify.com' && url.pathname === '/api/token';
                    } catch (_) { return false; }
                };
                const report = value => window.ArchiveTuneCanvasSession.postMessage(value);
                const originalFetch = window.fetch;
                window.fetch = function(input) {
                    const result = originalFetch.apply(this, arguments);
                    if (matches(input && input.url ? input.url : input)) {
                        result.then(response => response.clone().text()).then(report).catch(() => {});
                    }
                    return result;
                };
                const originalOpen = XMLHttpRequest.prototype.open;
                const originalSend = XMLHttpRequest.prototype.send;
                XMLHttpRequest.prototype.open = function(method, url) {
                    this.__archiveTuneCanvasToken = matches(url);
                    return originalOpen.apply(this, arguments);
                };
                XMLHttpRequest.prototype.send = function() {
                    if (this.__archiveTuneCanvasToken) {
                        this.addEventListener('load', () => {
                            if (this.responseType === '' || this.responseType === 'text') report(this.responseText);
                            else if (this.responseType === 'json') report(JSON.stringify(this.response));
                        }, { once: true });
                    }
                    return originalSend.apply(this, arguments);
                };
            })();
        """.trimIndent()
    }
}
