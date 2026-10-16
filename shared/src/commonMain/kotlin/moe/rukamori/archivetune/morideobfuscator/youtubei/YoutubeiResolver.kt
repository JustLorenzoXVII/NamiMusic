/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 *
 * ArchiveTuneX: versione Kotlin Multiplatform (Android + iOS) del modulo morideobfuscator.
 */
package moe.rukamori.archivetune.morideobfuscator.youtubei
package moe.rukamori.archivetune.morideobfuscator.youtubei

import io.ktor.http.Url
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.io.IOException
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import kotlin.time.TimeSource

class YoutubeiResolver(
    private val diagnostics: (String) -> Unit = {},
) {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val httpClient = YoutubeiHttpClient(diagnostics)
    private val diskCache = YoutubeiDiskCache()
    private val worker =
        YoutubeiQuickJsWorker(
            name = "Primary",
            httpClient = httpClient,
            diskCache = diskCache,
            diagnostics = diagnostics,
        )
    private val backgroundPermit = Semaphore(1)

    suspend fun preWarm() {
        worker.preWarm()
    }

    suspend fun resolve(
        request: YoutubeiStreamRequest,
        priority: YoutubeiResolutionPriority,
        videoPoTokenProvider: suspend (String) -> String? = { null },
    ): YoutubeiResolvedStream =
        when (priority) {
            YoutubeiResolutionPriority.FOREGROUND ->
                resolveWithWorker(
                    request = request,
                    videoPoTokenProvider = videoPoTokenProvider,
                )
            YoutubeiResolutionPriority.BACKGROUND ->
                backgroundPermit.withPermit {
                    resolveWithWorker(
                        request = request,
                        videoPoTokenProvider = videoPoTokenProvider,
                    )
                }
        }

    suspend fun invalidateSessions() {
        worker.closeRuntime()
    }

    /** Libera la memoria di QuickJS quando l'app va in background. */
    fun trimMemory() {
        applicationScope.launch { invalidateSessions() }
    }

    private suspend fun resolveWithWorker(
        request: YoutubeiStreamRequest,
        videoPoTokenProvider: suspend (String) -> String?,
    ): YoutubeiResolvedStream {
        val requestJson = request.toJson().toString()
        val mark = TimeSource.Monotonic.markNow()
        diagnostics("Resolution queued mediaId=${request.mediaId}")
        val response =
            try {
                worker.resolve(
                    requestJson = requestJson,
                    videoPoTokenProvider = videoPoTokenProvider,
                )
            } catch (cancellation: CancellationException) {
                diagnostics("Resolution cancelled elapsedMs=${mark.elapsedNow().inWholeMilliseconds}")
                throw cancellation
            } catch (failure: YoutubeiException) {
                throw failure
            } catch (network: IOException) {
                throw YoutubeiException(
                    kind = YoutubeiFailureKind.NETWORK,
                    message = network.message ?: "youtubei.js network request failed",
                    cause = network,
                )
            } catch (throwable: Throwable) {
                currentCoroutineContext().ensureActive()
                val interrupted = throwable::class.simpleName.orEmpty().contains("Interrupted", ignoreCase = true)
                throw YoutubeiException(
                    kind = if (interrupted) YoutubeiFailureKind.TIMEOUT else YoutubeiFailureKind.INTERNAL,
                    message = throwable.message ?: if (interrupted) "youtubei.js execution timed out" else "youtubei.js execution failed",
                    cause = throwable,
                )
            }
        return parseResponse(response).also {
            diagnostics("Resolution complete elapsedMs=${mark.elapsedNow().inWholeMilliseconds}")
        }
    }

    private fun parseResponse(response: String): YoutubeiResolvedStream {
        val json =
            runCatching { YoutubeiJson.parseToJsonElement(response).jsonObject }.getOrElse { throwable ->
                throw YoutubeiException(
                    kind = YoutubeiFailureKind.INVALID_RESPONSE,
                    message = "youtubei.js returned malformed JSON",
                    cause = throwable,
                )
            }
        if (json.bool("ok") != true) {
            val error = json["error"] as? JsonObject
            val kind =
                runCatching {
                    YoutubeiFailureKind.valueOf(error?.string("kind").orEmpty())
                }.getOrDefault(YoutubeiFailureKind.INTERNAL)
            throw YoutubeiException(
                kind = kind,
                message = error?.string("message")?.takeIf(String::isNotBlank)
                    ?: "youtubei.js resolution failed",
                httpStatus = error?.int("httpStatus")?.takeIf { it > 0 },
            )
        }
        val value =
            json["value"] as? JsonObject
                ?: throw YoutubeiException(
                    kind = YoutubeiFailureKind.INVALID_RESPONSE,
                    message = "youtubei.js returned no stream payload",
                )
        val url = value.string("url").orEmpty().trim()
        val scheme = runCatching { Url(url).protocol.name.lowercase() }.getOrNull()
        if (scheme !in HTTP_SCHEMES) {
            throw YoutubeiException(
                kind = YoutubeiFailureKind.INVALID_RESPONSE,
                message = "youtubei.js returned an unsupported stream URL",
            )
        }
        val headers =
            buildMap {
                (value["headers"] as? JsonObject)?.forEach { (name, element) ->
                    val headerValue = runCatching { element.jsonPrimitive.contentOrNull }.getOrNull().orEmpty()
                    if (isSafeHeader(name, headerValue)) put(name, headerValue)
                }
            }
        val now = currentTimeMillis()
        return YoutubeiResolvedStream(
            url = url,
            requestHeaders = headers,
            formatId = value.int("formatId") ?: -1,
            mimeType = value.string("mimeType") ?: "audio/webm",
            codecs = value.string("codecs").orEmpty(),
            bitrate = (value.int("bitrate") ?: 0).coerceAtLeast(0),
            sampleRate = value.int("sampleRate")?.takeIf { it > 0 },
            contentLength = (value.long("contentLength") ?: 0L).coerceAtLeast(0L),
            expiresAtMs = value.long("expiresAtMs")?.takeIf { it > now } ?: (now + DEFAULT_STREAM_LIFETIME_MS),
            runtimeVersion = value.string("runtimeVersion") ?: YOUTUBEI_VERSION,
            title = value.string("title")?.takeIf(String::isNotBlank),
            durationSeconds = value.int("durationSeconds")?.takeIf { it > 0 },
            thumbnailUrl = value.string("thumbnailUrl")?.takeIf(String::isNotBlank),
            loudnessDb = value.double("loudnessDb")?.takeIf(Double::isFinite),
            perceptualLoudnessDb = value.double("perceptualLoudnessDb")?.takeIf(Double::isFinite),
            playbackTrackingUrl = value.string("playbackTrackingUrl")?.takeIf(String::isNotBlank),
        )
    }

    private fun YoutubeiStreamRequest.toJson(): JsonObject =
        buildJsonObject {
            put("mediaId", mediaId)
            put("quality", quality.name)
            put("networkMetered", networkMetered)
            put("authFingerprint", authFingerprint)
            put("pinnedItag", pinnedItag)
            put("requiresSongMetadata", requiresSongMetadata)
            put("cookie", cookie)
            put("visitorData", visitorData)
            put("dataSyncId", dataSyncId)
            put("sessionPoToken", sessionPoToken)
            put("videoPoToken", videoPoToken)
            put("language", language)
            put("location", location)
            put("timezone", timezone)
        }

    private fun isSafeHeader(
        name: String,
        value: String,
    ): Boolean =
        name.isNotBlank() &&
            value.isNotBlank() &&
            name.none { it == '\r' || it == '\n' } &&
            value.none { it == '\r' || it == '\n' }

    private fun JsonObject.primitive(name: String): JsonPrimitive? =
        (this[name] as? JsonPrimitive)?.takeUnless { it is JsonNull }

    private fun JsonObject.string(name: String): String? = primitive(name)?.contentOrNull

    private fun JsonObject.bool(name: String): Boolean? = primitive(name)?.booleanOrNull

    private fun JsonObject.int(name: String): Int? = primitive(name)?.intOrNull ?: primitive(name)?.doubleOrNull?.toInt()

    private fun JsonObject.long(name: String): Long? = primitive(name)?.longOrNull ?: primitive(name)?.doubleOrNull?.toLong()

    private fun JsonObject.double(name: String): Double? = primitive(name)?.doubleOrNull

    private companion object {
        const val YOUTUBEI_VERSION = "18.0.0"
        const val DEFAULT_STREAM_LIFETIME_MS = 5L * 60L * 1000L
        val HTTP_SCHEMES = setOf("http", "https")
    }
}
