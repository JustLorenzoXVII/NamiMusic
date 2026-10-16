package moe.rukamori.archivetune.compat.okhttp3

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.header
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsBytes
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.content.ByteArrayContent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.io.IOException
import moe.rukamori.archivetune.compat.TimeUnit
import moe.rukamori.archivetune.compat.Uri
import moe.rukamori.archivetune.innertube.network.createPlatformHttpClient

/**
 * Sottoinsieme dell'API di OkHttp (okhttp3) implementato su Ktor, perché OkHttp esiste solo
 * su Android/JVM. Le classi hanno gli stessi nomi così il codice di ArchiveTune resta uguale.
 */
class OkHttpClient private constructor(
    internal val builder: Builder,
) {
    constructor() : this(Builder())

    internal val ktor: HttpClient by lazy {
        createPlatformHttpClient(proxy = null, dnsOverHttpsUrl = null) {
            followRedirects = builder.followRedirects
            install(HttpTimeout) {
                builder.callTimeoutMs?.let { requestTimeoutMillis = it }
                builder.connectTimeoutMs?.let { connectTimeoutMillis = it }
                builder.readTimeoutMs?.let { socketTimeoutMillis = it }
            }
        }
    }

    fun newCall(request: Request): Call = Call(this, request)

    fun newBuilder(): Builder = builder.copy()

    class Builder {
        internal var callTimeoutMs: Long? = null
        internal var connectTimeoutMs: Long? = null
        internal var readTimeoutMs: Long? = null
        internal var followRedirects: Boolean = true
        internal val interceptors = mutableListOf<Interceptor>()

        fun callTimeout(timeout: Long, unit: TimeUnit) = apply { callTimeoutMs = unit.toMillis(timeout) }

        fun connectTimeout(timeout: Long, unit: TimeUnit) = apply { connectTimeoutMs = unit.toMillis(timeout) }

        fun readTimeout(timeout: Long, unit: TimeUnit) = apply { readTimeoutMs = unit.toMillis(timeout) }

        fun writeTimeout(timeout: Long, unit: TimeUnit) = apply { }

        fun callTimeout(timeout: Int, unit: TimeUnit) = callTimeout(timeout.toLong(), unit)

        fun connectTimeout(timeout: Int, unit: TimeUnit) = connectTimeout(timeout.toLong(), unit)

        fun readTimeout(timeout: Int, unit: TimeUnit) = readTimeout(timeout.toLong(), unit)

        fun writeTimeout(timeout: Int, unit: TimeUnit) = apply { }

        fun followRedirects(follow: Boolean) = apply { followRedirects = follow }

        fun followSslRedirects(follow: Boolean) = apply { }

        fun retryOnConnectionFailure(retry: Boolean) = apply { }

        fun addInterceptor(interceptor: Interceptor) = apply { interceptors += interceptor }

        fun addNetworkInterceptor(interceptor: Interceptor) = apply { interceptors += interceptor }

        fun cache(@Suppress("UNUSED_PARAMETER") cache: Any?) = apply { }

        fun proxy(@Suppress("UNUSED_PARAMETER") proxy: Any?) = apply { }

        internal fun copy(): Builder =
            Builder().also {
                it.callTimeoutMs = callTimeoutMs
                it.connectTimeoutMs = connectTimeoutMs
                it.readTimeoutMs = readTimeoutMs
                it.followRedirects = followRedirects
                it.interceptors += interceptors
            }

        fun build(): OkHttpClient = OkHttpClient(this)
    }
}

class HttpUrl internal constructor(
    private val value: String,
) {
    private val uri = Uri.parse(value)
    val host: String get() = uri.host.orEmpty()
    val scheme: String get() = uri.scheme.orEmpty()
    val encodedPath: String get() = uri.path.orEmpty()
    val pathSegments: List<String> get() = uri.pathSegments

    fun queryParameter(name: String): String? = uri.getQueryParameter(name)

    override fun toString(): String = value

    companion object {
        fun String.toHttpUrlOrNull(): HttpUrl? = if (startsWith("http://") || startsWith("https://")) HttpUrl(this) else null

        fun String.toHttpUrl(): HttpUrl = toHttpUrlOrNull() ?: throw IllegalArgumentException("URL non valido: $this")
    }
}

class MediaType(
    val value: String,
) {
    override fun toString(): String = value

    companion object {
        fun String.toMediaType(): MediaType = MediaType(this)

        fun String.toMediaTypeOrNull(): MediaType? = MediaType(this)
    }
}

class RequestBody(
    val bytes: ByteArray,
    val contentType: MediaType?,
) {
    companion object {
        fun String.toRequestBody(contentType: MediaType? = null): RequestBody = RequestBody(encodeToByteArray(), contentType)

        fun ByteArray.toRequestBody(contentType: MediaType? = null): RequestBody = RequestBody(this, contentType)
    }
}

class Headers internal constructor(
    private val map: Map<String, List<String>>,
) {
    operator fun get(name: String): String? = map.entries.firstOrNull { it.key.equals(name, ignoreCase = true) }?.value?.firstOrNull()

    fun names(): Set<String> = map.keys

    companion object {
        fun Map<String, String>.toHeaders(): Headers = Headers(mapValues { listOf(it.value) })
    }
}

class Request internal constructor(
    val url: HttpUrl,
    val method: String,
    val headers: Map<String, String>,
    val body: RequestBody?,
    internal val tags: Map<kotlin.reflect.KClass<*>, Any> = emptyMap(),
) {
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> tag(type: kotlin.reflect.KClass<T>): T? = tags[type] as T?

    fun header(name: String): String? = headers.entries.firstOrNull { it.key.equals(name, ignoreCase = true) }?.value

    fun newBuilder(): Builder =
        Builder().also { b ->
            b.url(url.toString())
            headers.forEach { (k, v) -> b.header(k, v) }
            b.method(method, body)
            tags.forEach { (k, v) -> b.putTag(k, v) }
        }

    class Builder {
        private var url: String? = null
        private var method = "GET"
        private var body: RequestBody? = null
        private val headers = LinkedHashMap<String, String>()
        private val tags = LinkedHashMap<kotlin.reflect.KClass<*>, Any>()

        internal fun putTag(type: kotlin.reflect.KClass<*>, value: Any) {
            tags[type] = value
        }

        fun <T : Any> tag(type: kotlin.reflect.KClass<in T>, value: T?) = apply { if (value == null) tags.remove(type) else tags[type] = value }

        fun url(url: String) = apply { this.url = url }

        fun url(url: HttpUrl) = apply { this.url = url.toString() }

        fun header(name: String, value: String) = apply { headers[name] = value }

        fun addHeader(name: String, value: String) = apply { headers[name] = headers[name]?.let { "$it, $value" } ?: value }

        fun removeHeader(name: String) = apply { headers.remove(name) }

        fun headers(headers: Headers) = apply { headers.names().forEach { n -> headers[n]?.let { this.headers[n] = it } } }

        fun get() = apply { method = "GET" }

        fun head() = apply { method = "HEAD" }

        fun post(body: RequestBody) = apply {
            method = "POST"
            this.body = body
        }

        fun put(body: RequestBody) = apply {
            method = "PUT"
            this.body = body
        }

        fun delete(body: RequestBody? = null) = apply {
            method = "DELETE"
            this.body = body
        }

        fun method(method: String, body: RequestBody?) = apply {
            this.method = method
            this.body = body
        }

        fun build(): Request = Request(HttpUrl(url ?: error("url mancante")), method, headers.toMap(), body, tags.toMap())
    }
}

class ResponseBody internal constructor(
    private val data: ByteArray,
    private val type: String?,
) {
    fun string(): String = data.decodeToString()

    fun bytes(): ByteArray = data

    fun contentLength(): Long = data.size.toLong()

    fun contentType(): MediaType? = type?.let { MediaType(it) }

    fun close() = Unit

    fun byteStream(): ByteArray = data
}

class Response internal constructor(
    val request: Request,
    val code: Int,
    val message: String,
    private val headerMap: Map<String, List<String>>,
    val body: ResponseBody?,
) : AutoCloseable {
    val isSuccessful: Boolean
        get() = code in 200..299

    val isRedirect: Boolean
        get() = code in 300..399

    val headers: Headers
        get() = Headers(headerMap)

    fun header(name: String, defaultValue: String? = null): String? =
        headerMap.entries.firstOrNull { it.key.equals(name, ignoreCase = true) }?.value?.firstOrNull() ?: defaultValue

    override fun close() = Unit

    fun newBuilder(): Builder = Builder(this)

    class Builder internal constructor(
        private val base: Response,
    ) {
        private var body: ResponseBody? = base.body

        fun body(body: ResponseBody?) = apply { this.body = body }

        fun build(): Response = Response(base.request, base.code, base.message, base.headerMap, body)
    }
}

/** Interceptor di OkHttp: qui riceve la richiesta e chiama [Chain.proceed]. */
fun interface Interceptor {
    fun intercept(chain: Chain): Response

    interface Chain {
        fun request(): Request

        fun proceed(request: Request): Response

        fun call(): Call
    }
}

interface Callback {
    fun onFailure(call: Call, e: IOException)

    fun onResponse(call: Call, response: Response)
}

class Call internal constructor(
    private val client: OkHttpClient,
    private val originalRequest: Request,
) {
    private var job: Job? = null
    private var canceled = false

    fun request(): Request = originalRequest

    fun isCanceled(): Boolean = canceled

    fun cancel() {
        canceled = true
        job?.cancel()
    }

    /** Esegue la richiesta bloccando il thread (da usare fuori dal thread principale). */
    fun execute(): Response = runBlocking { executeSuspending() }

    fun enqueue(callback: Callback) {
        job =
            scope.launch {
                try {
                    val response = executeSuspending()
                    callback.onResponse(this@Call, response)
                } catch (e: IOException) {
                    callback.onFailure(this@Call, e)
                } catch (e: kotlinx.coroutines.CancellationException) {
                    callback.onFailure(this@Call, IOException("Canceled"))
                } catch (e: Exception) {
                    callback.onFailure(this@Call, IOException(e.message, e))
                }
            }
    }

    suspend fun executeSuspending(): Response = proceedAt(0, originalRequest)

    private suspend fun proceedAt(
        index: Int,
        request: Request,
    ): Response {
        val interceptors = client.builder.interceptors
        if (index >= interceptors.size) return send(request)
        val chain =
            object : Interceptor.Chain {
                override fun request(): Request = request

                override fun proceed(request: Request): Response = runBlocking { proceedAt(index + 1, request) }

                override fun call(): Call = this@Call
            }
        return interceptors[index].intercept(chain)
    }

    private suspend fun send(request: Request): Response {
        val response: HttpResponse =
            try {
                client.ktor.request(request.url.toString()) {
                    method = HttpMethod.parse(request.method)
                    request.headers.forEach { (k, v) -> header(k, v) }
                    request.body?.let { body ->
                        setBody(ByteArrayContent(body.bytes, body.contentType?.value?.let { ContentType.parse(it) } ?: ContentType.Application.OctetStream))
                    }
                }
            } catch (e: IOException) {
                throw e
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                throw IOException(e.message, e)
            }
        val bytes = response.bodyAsBytes()
        return Response(
            request = request,
            code = response.status.value,
            message = response.status.description,
            headerMap = response.headers.entries().associate { it.key to it.value },
            body = ResponseBody(bytes, response.headers["Content-Type"]),
        )
    }

    private companion object {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }
}

// region WebSocket (OkHttp) su Ktor

interface WebSocket {
    fun send(text: String): Boolean

    fun close(code: Int, reason: String?): Boolean

    fun cancel()

    fun request(): Request
}

abstract class WebSocketListener {
    open fun onOpen(webSocket: WebSocket, response: Response) {}

    open fun onMessage(webSocket: WebSocket, text: String) {}

    open fun onClosing(webSocket: WebSocket, code: Int, reason: String) {}

    open fun onClosed(webSocket: WebSocket, code: Int, reason: String) {}

    open fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {}
}

private class KtorWebSocket(
    private val client: OkHttpClient,
    private val request: Request,
    private val listener: WebSocketListener,
) : WebSocket {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val outgoing = kotlinx.coroutines.channels.Channel<String>(kotlinx.coroutines.channels.Channel.UNLIMITED)
    private var session: io.ktor.websocket.WebSocketSession? = null
    private var job: Job? = null

    fun start() {
        job =
            scope.launch {
                try {
                    val ktor =
                        createPlatformHttpClient(proxy = null, dnsOverHttpsUrl = null) {
                            install(io.ktor.client.plugins.websocket.WebSockets)
                        }
                    ktor.webSocketSessionFor(request).let { ws ->
                        session = ws
                        val opened = Response(request, 101, "Switching Protocols", emptyMap(), null)
                        listener.onOpen(this@KtorWebSocket, opened)
                        val sender =
                            launch {
                                for (text in outgoing) ws.send(io.ktor.websocket.Frame.Text(text))
                            }
                        try {
                            for (frame in ws.incoming) {
                                if (frame is io.ktor.websocket.Frame.Text) {
                                    listener.onMessage(this@KtorWebSocket, frame.data.decodeToString())
                                }
                            }
                            val reason = ws.closeReasonOrNull()
                            listener.onClosing(this@KtorWebSocket, reason?.code?.toInt() ?: 1000, reason?.message.orEmpty())
                            listener.onClosed(this@KtorWebSocket, reason?.code?.toInt() ?: 1000, reason?.message.orEmpty())
                        } finally {
                            sender.cancel()
                        }
                    }
                } catch (e: kotlinx.coroutines.CancellationException) {
                    throw e
                } catch (e: Throwable) {
                    listener.onFailure(this@KtorWebSocket, e, null)
                }
            }
    }

    override fun send(text: String): Boolean = outgoing.trySend(text).isSuccess

    override fun close(code: Int, reason: String?): Boolean {
        val ws = session ?: return false
        scope.launch { runCatching { ws.close(io.ktor.websocket.CloseReason(code.toShort(), reason.orEmpty())) } }
        return true
    }

    override fun cancel() {
        job?.cancel()
        outgoing.close()
    }

    override fun request(): Request = request
}

private suspend fun HttpClient.webSocketSessionFor(request: Request): io.ktor.websocket.DefaultWebSocketSession =
    io.ktor.client.plugins.websocket.webSocketSession(request.url.toString().replaceFirst("https://", "wss://").replaceFirst("http://", "ws://")) {
        request.headers.forEach { (k, v) -> header(k, v) }
    }

private suspend fun io.ktor.websocket.DefaultWebSocketSession.closeReasonOrNull(): io.ktor.websocket.CloseReason? = runCatching { closeReason.await() }.getOrNull()

fun OkHttpClient.newWebSocket(request: Request, listener: WebSocketListener): WebSocket = KtorWebSocket(this, request, listener).also { it.start() }

// endregion
