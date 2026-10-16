package moe.rukamori.archivetune.innertube.network

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.darwin.Darwin
import moe.rukamori.archivetune.innertube.models.YouTubeLocale
import platform.Foundation.NSLocale
import platform.Foundation.NSLocaleCountryCode
import platform.Foundation.NSLocaleLanguageCode
import platform.Foundation.NSLocaleScriptCode
import platform.Foundation.currentLocale
import platform.Foundation.preferredLanguages

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

/**
 * Lingua e paese per YouTube.
 *
 * Su iPhone lingua e regione sono separate: con lingua inglese e regione Italia
 * si otterrebbe "en-IT", che YouTube rifiuta con "400 invalid argument".
 * Per hl si usa quindi solo la lingua dell'app (prima lingua preferita), tranne
 * il cinese, dove YouTube vuole zh-CN / zh-TW.
 */
internal actual fun systemYouTubeLocale(): YouTubeLocale {
    val locale = NSLocale.currentLocale
    val country = (locale.objectForKey(NSLocaleCountryCode) as? String)?.uppercase() ?: "US"

    val preferred = NSLocale.preferredLanguages.firstOrNull() as? String
    val language =
        preferred?.substringBefore('-')?.lowercase()
            ?: (locale.objectForKey(NSLocaleLanguageCode) as? String)?.lowercase()
            ?: "en"

    val hl =
        if (language == "zh") {
            val script = locale.objectForKey(NSLocaleScriptCode) as? String
            val traditional = script == "Hant" || preferred?.contains("Hant") == true || country in setOf("TW", "HK", "MO")
            if (traditional) "zh-TW" else "zh-CN"
        } else {
            language
        }

    return YouTubeLocale(gl = country, hl = hl)
}
