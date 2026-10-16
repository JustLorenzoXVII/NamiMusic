package moe.rukamori.archivetune.innertube.network

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.darwin.Darwin
import moe.rukamori.archivetune.innertube.models.YouTubeLocale
import platform.Foundation.NSLocale
import platform.Foundation.NSLocaleCountryCode
import platform.Foundation.NSLocaleLanguageCode
import platform.Foundation.currentLocale

/**
 * Su iOS proxy e DNS-over-HTTPS per ora non sono gestiti: si usa la rete di sistema.
 */
internal actual fun createPlatformHttpClient(
    proxy: ProxySettings?,
    dnsOverHttpsUrl: String?,
    configure: HttpClientConfig<*>.() -> Unit,
): HttpClient =
    HttpClient(Darwin) {
        engine {
            configureRequest {
                setAllowsCellularAccess(true)
            }
        }
        configure()
    }

internal actual fun systemYouTubeLocale(): YouTubeLocale {
    val locale = NSLocale.currentLocale
    val country = locale.objectForKey(NSLocaleCountryCode) as? String ?: "US"
    val language = locale.objectForKey(NSLocaleLanguageCode) as? String ?: "en"
    return YouTubeLocale(
        gl = country,
        hl = "$language-$country",
    )
}
