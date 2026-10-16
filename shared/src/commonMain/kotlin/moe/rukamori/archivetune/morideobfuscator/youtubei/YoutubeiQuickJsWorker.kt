/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 *
 * ArchiveTuneX: versione Kotlin Multiplatform (Android + iOS) del modulo morideobfuscator.
 */
package moe.rukamori.archivetune.morideobfuscator.youtubei

import archivetunex.shared.generated.resources.Res
import com.dokar.quickjs.QuickJs
import com.dokar.quickjs.binding.asyncFunction
import com.dokar.quickjs.binding.function
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import moe.rukamori.archivetune.innertube.utils.sha1
import org.jetbrains.compose.resources.ExperimentalResourceApi
import kotlin.concurrent.Volatile
import kotlin.io.encoding.Base64
import kotlin.random.Random
import kotlin.time.TimeSource
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Esegue youtubei.js dentro QuickJS (stessa libreria su Android e iPhone).
 * I file JS stanno nelle risorse Compose (composeResources/files/youtubei).
 */
internal class YoutubeiQuickJsWorker(
    private val name: String,
    private val httpClient: YoutubeiHttpClient,
    private val diskCache: YoutubeiDiskCache,
    private val diagnostics: (String) -> Unit,
) {
    private val dispatcher = createYoutubeiDispatcher("ArchiveTune-Youtubei-$name", WORKER_STACK_SIZE_BYTES)
    private val mutex = Mutex()

    @Volatile
    private var initialized = false

    private var quickJs: QuickJs? = null
    private var activeVideoPoTokenProvider: (suspend (String) -> String?)? = null
    private var activeRequestAuthentication: YoutubeiRequestAuthentication? = null

    suspend fun preWarm() {
        mutex.withLock {
            withContext(dispatcher) {
                ensureInitialized()
            }
        }
    }

    suspend fun resolve(
        requestJson: String,
        videoPoTokenProvider: suspend (String) -> String?,
    ): String =
        mutex.withLock {
            withContext(dispatcher) {
                val runtime = ensureInitialized()
                activeVideoPoTokenProvider = videoPoTokenProvider
                try {
                    activeRequestAuthentication =
                        YoutubeiRequestAuthentication.fromRequest(YoutubeiJson.parseToJsonElement(requestJson).jsonObject)
                    val quotedRequest = JsonPrimitive(requestJson).toString()
                    val preparation =
                        traceStage("prepare-session") {
                            evaluateRequest(
                                runtime = runtime,
                                code = "await globalThis.ArchiveTuneYoutubei.prepare($quotedRequest);",
                                filename = "archivetune-prepare.js",
                            )
                        }
                    runtime.gc()
                    if (!preparation.isOk()) {
                        return@withContext preparation
                    }
                    val response =
                        traceStage("resolve-stream") {
                            evaluateRequest(
                                runtime = runtime,
                                code = "await globalThis.ArchiveTuneYoutubei.resolvePrepared($quotedRequest);",
                                filename = "archivetune-resolve.js",
                            )
                        }
                    runtime.gc()
                    response
                } catch (throwable: Throwable) {
                    if (
                        throwable is CancellationException ||
                        throwable.isInterrupted() ||
                        throwable is YoutubeiException && throwable.kind == YoutubeiFailureKind.TIMEOUT ||
                        throwable.isOutOfMemory()
                    ) {
                        discardRuntime(runtime, throwable)
                    } else {
                        try {
                            runtime.gc()
                        } catch (gcFailure: Throwable) {
                            throwable.addSuppressed(gcFailure)
                        }
                    }
                    throw throwable
                } finally {
                    activeVideoPoTokenProvider = null
                    activeRequestAuthentication = null
                }
            }
        }

    private suspend fun evaluateRequest(
        runtime: QuickJs,
        code: String,
        filename: String,
    ): String {
        val response = runtime.evaluateString(code, filename)
        currentCoroutineContext().ensureActive()
        val error = runCatching { YoutubeiJson.parseToJsonElement(response).jsonObject["error"] as? JsonObject }.getOrNull()
        val message = error?.get("message")?.jsonPrimitive?.contentOrNull?.trim()
        if (message.equals("interrupted", ignoreCase = true)) {
            throw YoutubeiException(
                kind = YoutubeiFailureKind.TIMEOUT,
                message = "youtubei.js evaluation was interrupted",
            )
        }
        return response
    }

    suspend fun closeRuntime() {
        mutex.withLock {
            withContext(dispatcher) {
                val runtime = quickJs
                quickJs = null
                initialized = false
                runtime?.close()
            }
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    private suspend fun ensureInitialized(): QuickJs {
        quickJs?.takeIf { initialized }?.let { return it }
        quickJs?.close()
        val runtime = QuickJs.create(dispatcher)
        try {
            runtime.memoryLimit = JAVASCRIPT_MEMORY_LIMIT_BYTES
            runtime.maxStackSize = JAVASCRIPT_STACK_LIMIT_BYTES
            runtime.asyncFunction<String, String>("__archiveTuneHttp") { request ->
                httpClient.execute(request, activeRequestAuthentication)
            }
            runtime.asyncFunction<String, String>("__archiveTunePlayerSource") { request ->
                httpClient.executePlayerScript(request)
            }
            runtime.asyncFunction<String, String?>("__archiveTuneVideoPoToken") { mediaId ->
                activeVideoPoTokenProvider?.invoke(mediaId)
            }
            runtime.asyncFunction<String, String?>("__archiveTuneCacheRead") { key ->
                diskCache.read(key)
            }
            runtime.asyncFunction<String, Unit>("__archiveTuneCacheWrite") { request ->
                diskCache.write(request)
            }
            runtime.asyncFunction<String, Unit>("__archiveTuneCacheRemove") { key ->
                diskCache.remove(key)
            }
            runtime.asyncFunction<String, String>("__archiveTuneSha1") { value ->
                sha1(value)
            }
            runtime.function("__archiveTuneUuid") { Uuid.random().toString() }
            runtime.function("__archiveTuneRandom") { arguments ->
                val size = (arguments.firstOrNull() as? Number)?.toInt()?.coerceIn(0, MAX_RANDOM_BYTES) ?: 0
                Base64.Default.encode(Random.nextBytes(size))
            }
            evaluateResource(runtime, RUNTIME_POLYFILLS_RESOURCE)
            evaluateResource(runtime, BUNDLE_RESOURCE)
            val version = runtime.evaluateString("globalThis.ArchiveTuneYoutubei.version;", "version.js")
            check(version == YOUTUBEI_VERSION) { "Versione youtubei.js inattesa: $version" }
            quickJs = runtime
            initialized = true
            return runtime
        } catch (throwable: Throwable) {
            discardRuntime(runtime, throwable)
            throw throwable
        }
    }

    @OptIn(ExperimentalResourceApi::class)
    private suspend fun evaluateResource(
        runtime: QuickJs,
        path: String,
    ) {
        val source = Res.readBytes(path).decodeToString()
        traceStage("load-${path.substringAfterLast('/')}") {
            runtime.evaluateUnit(source, path.substringAfterLast('/'))
        }
    }

    private suspend fun <T> traceStage(
        stage: String,
        block: suspend () -> T,
    ): T {
        val mark = TimeSource.Monotonic.markNow()
        diagnostics("Worker $name $stage started")
        return try {
            block().also {
                diagnostics("Worker $name $stage complete elapsedMs=${mark.elapsedNow().inWholeMilliseconds}")
            }
        } catch (exception: Exception) {
            diagnostics("Worker $name $stage failed type=${exception::class.simpleName} elapsedMs=${mark.elapsedNow().inWholeMilliseconds}")
            throw exception
        }
    }

    private fun discardRuntime(
        runtime: QuickJs,
        failure: Throwable,
    ) {
        if (quickJs === runtime) {
            quickJs = null
            initialized = false
        }
        diagnostics("Worker $name discarded type=${failure::class.simpleName}")
        try {
            runtime.close()
        } catch (closeFailure: Throwable) {
            failure.addSuppressed(closeFailure)
        }
    }

    private fun String.isOk(): Boolean =
        runCatching { YoutubeiJson.parseToJsonElement(this).jsonObject["ok"]?.jsonPrimitive?.booleanOrNull }.getOrNull() == true

    private fun Throwable.isInterrupted(): Boolean =
        this::class.simpleName.orEmpty().contains("Interrupted", ignoreCase = true)

    private fun Throwable.isOutOfMemory(): Boolean =
        message.orEmpty().contains("out of memory", ignoreCase = true)

    private companion object {
        const val YOUTUBEI_VERSION = "18.0.0"
        const val BUNDLE_RESOURCE = "files/youtubei/youtubei.bundle.js"
        const val RUNTIME_POLYFILLS_RESOURCE = "files/youtubei/runtime-polyfills.js"
        const val JAVASCRIPT_MEMORY_LIMIT_BYTES = 256L * 1024L * 1024L
        const val JAVASCRIPT_STACK_LIMIT_BYTES = 2L * 1024L * 1024L
        const val WORKER_STACK_SIZE_BYTES = 4L * JAVASCRIPT_STACK_LIMIT_BYTES
        const val MAX_RANDOM_BYTES = 4096
    }
}
