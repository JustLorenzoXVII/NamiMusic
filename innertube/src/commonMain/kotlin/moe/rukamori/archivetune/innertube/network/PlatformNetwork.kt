package moe.rukamori.archivetune.innertube.network

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import moe.rukamori.archivetune.innertube.models.YouTubeLocale
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Proxy per le richieste verso YouTube. Sostituisce java.net.Proxy, che su iOS non esiste.
 */
data class ProxySettings(
    val type: Type,
    val host: String,
    val port: Int,
    val username: String? = null,
    val password: String? = null,
) {
    enum class Type { HTTP, SOCKS }
}

/**
 * Crea il client HTTP con il motore giusto per la piattaforma:
 * OkHttp su Android, NSURLSession (Darwin) su iOS.
 */
internal expect fun createPlatformHttpClient(
    proxy: ProxySettings?,
    dnsOverHttpsUrl: String?,
    configure: HttpClientConfig<*>.() -> Unit,
): HttpClient

/** Paese e lingua del telefono, nel formato che si aspetta YouTube (gl = "IT", hl = "it-IT"). */
internal expect fun systemYouTubeLocale(): YouTubeLocale

@OptIn(ExperimentalTime::class)
internal fun currentEpochSeconds(): Long = Clock.System.now().epochSeconds
