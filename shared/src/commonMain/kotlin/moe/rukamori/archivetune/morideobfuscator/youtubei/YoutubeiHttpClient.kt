/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 *
 * ArchiveTuneX: versione Kotlin Multiplatform (Android + iOS) del modulo morideobfuscator.
 */
package moe.rukamori.archivetune.morideobfuscator.youtubei

import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.header
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsBytes
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.URLBuilder
import io.ktor.http.URLProtocol
import io.ktor.http.Url
import io.ktor.http.content.ByteArrayContent
import io.ktor.http.takeFrom
import kotlinx.coroutines.CancellationException
import kotlinx.io.IOException
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import moe.rukamori.archivetune.innertube.network.createPlatformHttpClient
import kotlin.io.encoding.Base64
import kotlin.time.TimeSource

/**
 * Il "fetch" di youtubei.js: QuickJS non ha la rete, quindi ogni richiesta passa di qui.
 * Era OkHttp nell'originale; qui è Ktor (OkHttp su Android, NSURLSession su iPhone).
 */
internal class YoutubeiHttpClient(
    private val diagnostics: (String) -> Unit,
) {
    private val client =
        createPlatformHttpClient(proxy = null, dnsOverHttpsUrl = null) {
            expectSuccess = false
            followRedirects = false
            install(HttpTimeout) {
                connectTimeoutMillis = CONNECT_TIMEOUT_MS
                socketTimeoutMillis = READ_TIMEOUT_MS
                requestTimeoutMillis = CALL_TIMEOUT_MS
            }
        }

    suspend fun execute(
        requestJson: String,
        authentication: YoutubeiRequestAuthentication?,
    ): String =
        try {
            executeRequest(requestJson, authentication)
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: HttpRequestTimeoutException) {
            failure(FailureKind.TIMEOUT, "youtubei.js network request timed out")
        } catch (exception: IOException) {
            failure(FailureKind.NETWORK, "youtubei.js network request failed")
        } catch (exception: IllegalArgumentException) {
            failure(FailureKind.INVALID_RESPONSE, "youtubei.js request was rejected")
        } catch (exception: Exception) {
            if (exception.isTimeout()) {
                failure(FailureKind.TIMEOUT, "youtubei.js network request timed out")
            } else {
                failure(FailureKind.NETWORK, "youtubei.js HTTP bridge failed: ${exception.message}")
            }
        }

    suspend fun executePlayerScript(requestJson: String): String =
        try {
            executePlayerScriptRequest(requestJson)
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: HttpRequestTimeoutException) {
            playerSourceFailure(FailureKind.TIMEOUT, "YouTube player script request timed out")
        } catch (exception: IOException) {
            playerSourceFailure(FailureKind.NETWORK, "YouTube player script request failed")
        } catch (exception: IllegalArgumentException) {
            playerSourceFailure(FailureKind.INVALID_RESPONSE, "YouTube player script request was rejected")
        } catch (exception: Exception) {
            playerSourceFailure(FailureKind.INTERNAL, "YouTube player script bridge failed")
        }

    private suspend fun executeRequest(
        requestJson: String,
        authentication: YoutubeiRequestAuthentication?,
    ): String {
        val parsed = YoutubeiJson.parseToJsonElement(requestJson).jsonObject
        var url =
            runCatching { Url(parsed.string("url").orEmpty()) }.getOrNull()
                ?: return failure(FailureKind.INVALID_RESPONSE, "Invalid request URL")
        var method = (parsed.string("method") ?: "GET").uppercase()
        require(method in ALLOWED_METHODS)
        validateUrl(url)
        var headers: Map<String, String> = parsed.headers()
        var body: ByteArray? =
            parsed
                .string("bodyBase64")
                ?.takeIf(String::isNotEmpty)
                ?.let { LenientBase64.decode(it) }
        var redirected = false

        repeat(MAX_REDIRECTS + 1) { redirectCount ->
            val requestHeaders = headers.filterAllowed().toMutableMap()
            authentication?.applyTo(url, requestHeaders)
            val response = send(url, method, requestHeaders, body)
            if (response.status.value in 300..399 && response.headers["Location"] != null) {
                if (redirectCount == MAX_REDIRECTS) throw IOException("Too many extraction redirects")
                val location = response.headers["Location"] ?: throw IOException("Extraction redirect did not include a location")
                val redirectedUrl = URLBuilder(url).takeFrom(location).build()
                validateUrl(redirectedUrl)
                if (!url.isSameOrigin(redirectedUrl)) {
                    headers = headers.withoutHeaders(SENSITIVE_REDIRECT_HEADERS)
                }
                val code = response.status.value
                if ((code == 301 || code == 302) && method == "POST" || code == 303 && method != "GET" && method != "HEAD") {
                    method = "GET"
                    body = null
                    headers = headers.withoutHeaders(BODY_HEADERS)
                }
                url = redirectedUrl
                redirected = true
                return@repeat
            }
            val finalUrl = response.call.request.url
            validateUrl(finalUrl)
            val responseBytes = response.bodyAsBytes()
            require(responseBytes.size <= MAX_RESPONSE_BYTES)
            return buildJsonObject {
                put("ok", true)
                put("status", response.status.value)
                put("statusText", response.status.description)
                put("url", finalUrl.toString())
                put("redirected", redirected || finalUrl.toString() != url.toString())
                put("headers", response.headersJson())
                put("bodyBase64", Base64.Default.encode(responseBytes))
            }.toString()
        }
        return failure(FailureKind.NETWORK, "Extraction redirect failed")
    }

    private suspend fun executePlayerScriptRequest(requestJson: String): String {
        val parsed = YoutubeiJson.parseToJsonElement(requestJson).jsonObject
        var url =
            runCatching { Url(parsed.string("url").orEmpty()) }.getOrNull()
                ?: return playerSourceFailure(FailureKind.INVALID_RESPONSE, "Invalid player script URL")
        require((parsed.string("method") ?: "GET").uppercase() == "GET")
        validatePlayerScriptUrl(url)
        var headers: Map<String, String> = parsed.headers()

        repeat(MAX_REDIRECTS + 1) { redirectCount ->
            val response = send(url, "GET", headers.filterAllowed(), null)
            if (response.status.value in 300..399 && response.headers["Location"] != null) {
                if (redirectCount == MAX_REDIRECTS) throw IOException("Too many player script redirects")
                val location = response.headers["Location"] ?: throw IOException("Player script redirect did not include a location")
                val redirectedUrl = URLBuilder(url).takeFrom(location).build()
                validatePlayerScriptUrl(redirectedUrl)
                if (!url.isSameOrigin(redirectedUrl)) {
                    headers = headers.withoutHeaders(SENSITIVE_REDIRECT_HEADERS)
                }
                url = redirectedUrl
                return@repeat
            }
            validatePlayerScriptUrl(response.call.request.url)
            if (response.status.value !in 200..299) {
                return playerSourceFailure(
                    FailureKind.HTTP,
                    "YouTube player script request failed with HTTP ${response.status.value}",
                )
            }
            val bytes = response.bodyAsBytes()
            require(bytes.size <= MAX_PLAYER_SCRIPT_BYTES)
            val source = bytes.decodeToString()
            if (source.isBlank() || source.startsWith(PLAYER_SOURCE_ERROR_PREFIX)) {
                return playerSourceFailure(FailureKind.INVALID_RESPONSE, "YouTube returned an invalid player script")
            }
            return source
        }
        return playerSourceFailure(FailureKind.NETWORK, "Player script redirect failed")
    }

    private suspend fun send(
        url: Url,
        method: String,
        headers: Map<String, String>,
        body: ByteArray?,
    ): HttpResponse {
        val mark = TimeSource.Monotonic.markNow()
        val stage =
            when {
                url.encodedPath == "/iframe_api" -> "player-id"
                PLAYER_SCRIPT_PATH.matches(url.encodedPath) -> "player-script"
                url.encodedPath == "/youtubei/v1/player" -> "player-response"
                else -> "youtube-api"
            }
        diagnostics("HTTP $stage started")
        val response =
            client.request(url) {
                this.method = HttpMethod.parse(method)
                headers.forEach { (name, value) ->
                    // Content-Type lo imposta Ktor dal corpo della richiesta.
                    if (name != "content-type") header(name, value)
                }
                if (method == "POST") {
                    val contentType =
                        headers["content-type"]?.let { runCatching { ContentType.parse(it) }.getOrNull() }
                            ?: ContentType.Application.OctetStream
                    setBody(ByteArrayContent(body ?: ByteArray(0), contentType))
                }
            }
        diagnostics("HTTP $stage status=${response.status.value} elapsedMs=${mark.elapsedNow().inWholeMilliseconds}")
        return response
    }

    private fun validateUrl(url: Url) {
        require(url.protocol == URLProtocol.HTTPS)
        val host = url.host.lowercase()
        require(ALLOWED_HOST_SUFFIXES.any { suffix -> host == suffix || host.endsWith(".$suffix") })
    }

    private fun validatePlayerScriptUrl(url: Url) {
        require(url.protocol == URLProtocol.HTTPS)
        require(url.host.equals("www.youtube.com", ignoreCase = true) || url.host.equals("youtube.com", ignoreCase = true))
        require(PLAYER_SCRIPT_PATH.matches(url.encodedPath))
    }

    private fun Map<String, String>.filterAllowed(): Map<String, String> =
        filter { (name, value) -> isAllowedHeader(name, value) }

    private fun isAllowedHeader(
        name: String,
        value: String,
    ): Boolean {
        if (name.isBlank() || value.isBlank() || name.any { it == '\r' || it == '\n' } || value.any { it == '\r' || it == '\n' }) {
            return false
        }
        val normalized = name.lowercase()
        return normalized in ALLOWED_HEADERS || normalized.startsWith("x-goog-") || normalized.startsWith("x-youtube-")
    }

    private fun Url.isSameOrigin(other: Url): Boolean =
        protocol == other.protocol && host == other.host && port == other.port

    private fun Map<String, String>.withoutHeaders(excluded: Set<String>): Map<String, String> =
        filterKeys { it.lowercase() !in excluded }

    private fun HttpResponse.headersJson(): JsonObject =
        buildJsonObject {
            headers.names().forEach { name ->
                if (name.equals("set-cookie", ignoreCase = true)) return@forEach
                put(name.lowercase(), headers.getAll(name).orEmpty().joinToString(", "))
            }
        }

    /** Intestazioni della richiesta JS, con i nomi in minuscolo. */
    private fun JsonObject.headers(): Map<String, String> =
        (this["headers"] as? JsonObject)
            ?.mapNotNull { (name, value) ->
                val text = runCatching { value.jsonPrimitive.contentOrNull }.getOrNull() ?: return@mapNotNull null
                name.lowercase() to text
            }?.toMap()
            .orEmpty()

    private fun JsonObject.string(name: String): String? =
        runCatching { this[name]?.jsonPrimitive?.contentOrNull }.getOrNull()

    private fun Throwable.isTimeout(): Boolean {
        var current: Throwable? = this
        while (current != null) {
            if (current is HttpRequestTimeoutException) return true
            val simple = current::class.simpleName.orEmpty()
            if (simple.contains("Timeout", ignoreCase = true)) return true
            current = current.cause
        }
        return false
    }

    private fun failure(
        kind: FailureKind,
        message: String,
    ): String =
        buildJsonObject {
            put("ok", false)
            put("kind", kind.name)
            put("message", message)
        }.toString()

    private fun playerSourceFailure(
        kind: FailureKind,
        message: String,
    ): String = "$PLAYER_SOURCE_ERROR_PREFIX${kind.name}|$message"

    private enum class FailureKind {
        HTTP,
        TIMEOUT,
        NETWORK,
        INVALID_RESPONSE,
        INTERNAL,
    }

    private companion object {
        const val MAX_RESPONSE_BYTES = 8 * 1024 * 1024
        const val MAX_PLAYER_SCRIPT_BYTES = 4 * 1024 * 1024
        const val MAX_REDIRECTS = 5
        const val CONNECT_TIMEOUT_MS = 15_000L
        const val READ_TIMEOUT_MS = 20_000L
        const val CALL_TIMEOUT_MS = 30_000L
        const val PLAYER_SOURCE_ERROR_PREFIX = "ARCHIVETUNE_PLAYER_SOURCE_ERROR|"
        val PLAYER_SCRIPT_PATH =
            Regex("^/s/player/[A-Za-z0-9._-]+/player_es6\\.vflset/[A-Za-z0-9._-]+/base\\.js$")
        val ALLOWED_METHODS = setOf("GET", "POST", "HEAD")
        val BODY_HEADERS = setOf("content-type")
        val SENSITIVE_REDIRECT_HEADERS = setOf("authorization", "cookie")
        val ALLOWED_HOST_SUFFIXES =
            setOf(
                "youtube.com",
                "google.com",
                "googleapis.com",
                "googlevideo.com",
                "ytimg.com",
                "ggpht.com",
            )
        val ALLOWED_HEADERS =
            setOf(
                "accept",
                "accept-language",
                "authorization",
                "content-type",
                "cookie",
                "origin",
                "referer",
                "user-agent",
            )
    }
}
