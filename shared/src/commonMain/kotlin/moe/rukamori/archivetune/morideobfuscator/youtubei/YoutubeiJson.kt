/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 *
 * ArchiveTuneX: versione Kotlin Multiplatform (Android + iOS) del modulo morideobfuscator.
 */
package moe.rukamori.archivetune.morideobfuscator.youtubei

import kotlinx.serialization.json.Json
import kotlin.io.encoding.Base64

internal val YoutubeiJson = Json { ignoreUnknownKeys = true }

/** Come android.util.Base64.DEFAULT in decodifica: accetta anche il formato URL-safe e il padding mancante. */
internal object LenientBase64 {
    private val decoder = Base64.Default.withPadding(Base64.PaddingOption.PRESENT_OPTIONAL)

    fun decode(value: String): ByteArray {
        val normalized =
            value
                .filterNot { it.isWhitespace() }
                .replace('-', '+')
                .replace('_', '/')
        return decoder.decode(normalized)
    }
}
