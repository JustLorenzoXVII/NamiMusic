/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 *
 * ArchiveTuneX: versione Kotlin Multiplatform (Android + iOS) del modulo morideobfuscator.
 */
package moe.rukamori.archivetune.morideobfuscator.youtubei

import io.ktor.http.URLProtocol
import io.ktor.http.Url
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import moe.rukamori.archivetune.innertube.utils.sha1

internal class YoutubeiRequestAuthentication private constructor(
    private val cookie: String,
    private val delegatedSessionId: String?,
) {
    private val cookies =
        cookie.split(';').mapNotNull { entry ->
            val separator = entry.indexOf('=')
            if (separator <= 0) return@mapNotNull null
            val name = entry.substring(0, separator).trim()
            val value = entry.substring(separator + 1).trim()
            if (name.isEmpty() || value.isEmpty()) null else name to value
        }.toMap()

    /** [headers] ha le chiavi in minuscolo. */
    fun applyTo(
        url: Url,
        headers: MutableMap<String, String>,
    ) {
        if (url.host !in AUTHENTICATED_HOSTS ||
            !url.encodedPath.startsWith("/youtubei/") ||
            url.protocol != URLProtocol.HTTPS ||
            headers["cookie"].isNullOrBlank()
        ) {
            return
        }
        val origin = "https://${url.host}"
        val timestamp = currentTimeMillis() / 1_000L
        val authorization =
            listOfNotNull(
                signature("SAPISIDHASH", cookies["SAPISID"] ?: cookies["__Secure-3PAPISID"], origin, timestamp),
                signature("SAPISID1PHASH", cookies["__Secure-1PAPISID"], origin, timestamp),
                signature("SAPISID3PHASH", cookies["__Secure-3PAPISID"], origin, timestamp),
            ).joinToString(" ")
        if (authorization.isEmpty()) return
        headers["cookie"] = cookie
        headers["authorization"] = authorization
        headers["origin"] = origin
        headers["x-origin"] = origin
        headers["x-goog-authuser"] = "0"
        headers["x-youtube-bootstrap-logged-in"] = "true"
        if (delegatedSessionId != null) {
            headers["x-goog-pageid"] = delegatedSessionId
        } else {
            headers.remove("x-goog-pageid")
        }
    }

    private fun signature(
        scheme: String,
        sid: String?,
        origin: String,
        timestamp: Long,
    ): String? {
        if (sid == null) return null
        val hash = sha1("$timestamp $sid $origin")
        return "$scheme ${timestamp}_$hash"
    }

    companion object {
        private val AUTHENTICATED_HOSTS = setOf("www.youtube.com", "music.youtube.com")

        fun fromRequest(request: JsonObject): YoutubeiRequestAuthentication? {
            val cookie = request.optString("cookie")?.trim()?.takeIf { it.isNotEmpty() && it != "null" }
                ?: return null
            val dataSyncId = request.optString("dataSyncId")?.trim()?.takeIf { it.isNotEmpty() && it != "null" }
            val first = dataSyncId?.substringBefore("||")?.trim()?.takeIf(String::isNotEmpty)
            val second = dataSyncId?.substringAfter("||", "")?.trim()?.takeIf(String::isNotEmpty)
            return YoutubeiRequestAuthentication(
                cookie = cookie,
                delegatedSessionId = first.takeIf { second != null },
            )
        }

        private fun JsonObject.optString(name: String): String? =
            runCatching { this[name]?.jsonPrimitive?.contentOrNull }.getOrNull()
    }
}
