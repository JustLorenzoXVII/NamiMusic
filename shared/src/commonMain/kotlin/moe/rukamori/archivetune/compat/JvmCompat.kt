package moe.rukamori.archivetune.compat

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.longOrNull

// region org.json

class JSONException(
    message: String,
) : RuntimeException(message)

private fun Any?.toJsonElement(): JsonElement =
    when (this) {
        null, JSONObject.NULL -> JsonNull
        is JsonElement -> this
        is JSONObject -> toJsonElement()
        is JSONArray -> toJsonElement()
        is String -> JsonPrimitive(this)
        is Number -> JsonPrimitive(this)
        is Boolean -> JsonPrimitive(this)
        is Map<*, *> -> JsonObject(entries.associate { it.key.toString() to it.value.toJsonElement() })
        is Collection<*> -> JsonArray(map { it.toJsonElement() })
        else -> JsonPrimitive(toString())
    }

private fun JsonElement.toValue(): Any? =
    when (this) {
        JsonNull -> JSONObject.NULL
        is JsonObject -> JSONObject(this)
        is JsonArray -> JSONArray(this)
        is JsonPrimitive ->
            if (isString) {
                content
            } else {
                booleanOrNull ?: longOrNull?.let { if (it in Int.MIN_VALUE..Int.MAX_VALUE) it.toInt() else it } ?: doubleOrNull ?: content
            }
    }

private fun Any?.asString(): String? =
    when (this) {
        null, JSONObject.NULL -> null
        else -> toString()
    }

private fun Any?.asLong(): Long? =
    when (this) {
        is Number -> toLong()
        is String -> toLongOrNull() ?: toDoubleOrNull()?.toLong()
        else -> null
    }

private fun Any?.asDouble(): Double? =
    when (this) {
        is Number -> toDouble()
        is String -> toDoubleOrNull()
        else -> null
    }

private fun Any?.asBoolean(): Boolean? =
    when (this) {
        is Boolean -> this
        is String -> toBooleanStrictOrNull()
        else -> null
    }

/** org.json.JSONObject su kotlinx.serialization. */
class JSONObject() {
    private val map = LinkedHashMap<String, Any?>()

    constructor(source: String) : this() {
        val element = runCatching { Json.parseToJsonElement(source) }.getOrElse { throw JSONException(it.message ?: "JSON non valido") }
        val obj = element as? JsonObject ?: throw JSONException("Non è un oggetto JSON")
        obj.forEach { (k, v) -> map[k] = v.toValue() }
    }

    internal constructor(obj: JsonObject) : this() {
        obj.forEach { (k, v) -> map[k] = v.toValue() }
    }

    constructor(values: Map<*, *>) : this() {
        values.forEach { (k, v) -> map[k.toString()] = v }
    }

    fun has(name: String): Boolean = name in map

    fun isNull(name: String): Boolean = map[name] == null || map[name] == NULL

    fun keys(): Iterator<String> = map.keys.toList().iterator()

    fun length(): Int = map.size

    fun names(): JSONArray? = if (map.isEmpty()) null else JSONArray(map.keys.toList())

    fun opt(name: String): Any? = map[name]

    fun get(name: String): Any = map[name] ?: throw JSONException("JSONObject[\"$name\"] not found.")

    fun put(name: String, value: Any?): JSONObject = apply { map[name] = value }

    fun put(name: String, value: Int): JSONObject = apply { map[name] = value }

    fun put(name: String, value: Long): JSONObject = apply { map[name] = value }

    fun put(name: String, value: Double): JSONObject = apply { map[name] = value }

    fun put(name: String, value: Boolean): JSONObject = apply { map[name] = value }

    fun putOpt(name: String?, value: Any?): JSONObject = apply { if (name != null && value != null) map[name] = value }

    fun remove(name: String): Any? = map.remove(name)

    fun optString(name: String): String = optString(name, "")

    fun optString(name: String, fallback: String?): String = map[name].asString() ?: fallback.orEmpty()

    fun getString(name: String): String = map[name].asString() ?: throw JSONException("JSONObject[\"$name\"] not a string.")

    fun optInt(name: String, fallback: Int = 0): Int = map[name].asLong()?.toInt() ?: fallback

    fun getInt(name: String): Int = map[name].asLong()?.toInt() ?: throw JSONException("JSONObject[\"$name\"] is not an int.")

    fun optLong(name: String, fallback: Long = 0L): Long = map[name].asLong() ?: fallback

    fun getLong(name: String): Long = map[name].asLong() ?: throw JSONException("JSONObject[\"$name\"] is not a long.")

    fun optDouble(name: String, fallback: Double = Double.NaN): Double = map[name].asDouble() ?: fallback

    fun getDouble(name: String): Double = map[name].asDouble() ?: throw JSONException("JSONObject[\"$name\"] is not a number.")

    fun optBoolean(name: String, fallback: Boolean = false): Boolean = map[name].asBoolean() ?: fallback

    fun getBoolean(name: String): Boolean = map[name].asBoolean() ?: throw JSONException("JSONObject[\"$name\"] is not a boolean.")

    fun optJSONObject(name: String): JSONObject? = map[name] as? JSONObject

    fun getJSONObject(name: String): JSONObject = map[name] as? JSONObject ?: throw JSONException("JSONObject[\"$name\"] is not a JSONObject.")

    fun optJSONArray(name: String): JSONArray? = map[name] as? JSONArray

    fun getJSONArray(name: String): JSONArray = map[name] as? JSONArray ?: throw JSONException("JSONObject[\"$name\"] is not a JSONArray.")

    internal fun toJsonElement(): JsonObject = JsonObject(map.mapValues { it.value.toJsonElement() })

    override fun toString(): String = toJsonElement().toString()

    fun toString(indentFactor: Int): String = Json { prettyPrint = indentFactor > 0 }.encodeToString(JsonObject.serializer(), toJsonElement())

    companion object {
        val NULL: Any = object {
            override fun toString(): String = "null"

            override fun equals(other: Any?): Boolean = other == null || other === this

            override fun hashCode(): Int = 0
        }

        fun quote(string: String?): String = JsonPrimitive(string.orEmpty()).toString()
    }
}

/** org.json.JSONArray su kotlinx.serialization. */
class JSONArray() {
    private val list = ArrayList<Any?>()

    constructor(source: String) : this() {
        val element = runCatching { Json.parseToJsonElement(source) }.getOrElse { throw JSONException(it.message ?: "JSON non valido") }
        val arr = element as? JsonArray ?: throw JSONException("Non è un array JSON")
        arr.forEach { list += it.toValue() }
    }

    internal constructor(arr: JsonArray) : this() {
        arr.forEach { list += it.toValue() }
    }

    constructor(values: Collection<*>) : this() {
        list.addAll(values)
    }

    fun length(): Int = list.size

    fun isNull(index: Int): Boolean = list.getOrNull(index) == null || list[index] == JSONObject.NULL

    fun opt(index: Int): Any? = list.getOrNull(index)

    fun get(index: Int): Any = list.getOrNull(index) ?: throw JSONException("JSONArray[$index] not found.")

    fun put(value: Any?): JSONArray = apply { list += value }

    fun put(value: Int): JSONArray = apply { list += value }

    fun put(value: Long): JSONArray = apply { list += value }

    fun put(value: Double): JSONArray = apply { list += value }

    fun put(value: Boolean): JSONArray = apply { list += value }

    fun put(index: Int, value: Any?): JSONArray =
        apply {
            while (list.size <= index) list += JSONObject.NULL
            list[index] = value
        }

    fun remove(index: Int): Any? = list.removeAt(index)

    fun optString(index: Int): String = optString(index, "")

    fun optString(index: Int, fallback: String?): String = list.getOrNull(index).asString() ?: fallback.orEmpty()

    fun getString(index: Int): String = list.getOrNull(index).asString() ?: throw JSONException("JSONArray[$index] not a string.")

    fun optInt(index: Int, fallback: Int = 0): Int = list.getOrNull(index).asLong()?.toInt() ?: fallback

    fun getInt(index: Int): Int = list.getOrNull(index).asLong()?.toInt() ?: throw JSONException("JSONArray[$index] is not a number.")

    fun optLong(index: Int, fallback: Long = 0L): Long = list.getOrNull(index).asLong() ?: fallback

    fun getLong(index: Int): Long = list.getOrNull(index).asLong() ?: throw JSONException("JSONArray[$index] is not a number.")

    fun optDouble(index: Int, fallback: Double = Double.NaN): Double = list.getOrNull(index).asDouble() ?: fallback

    fun optBoolean(index: Int, fallback: Boolean = false): Boolean = list.getOrNull(index).asBoolean() ?: fallback

    fun optJSONObject(index: Int): JSONObject? = list.getOrNull(index) as? JSONObject

    fun getJSONObject(index: Int): JSONObject = list.getOrNull(index) as? JSONObject ?: throw JSONException("JSONArray[$index] is not a JSONObject.")

    fun optJSONArray(index: Int): JSONArray? = list.getOrNull(index) as? JSONArray

    fun getJSONArray(index: Int): JSONArray = list.getOrNull(index) as? JSONArray ?: throw JSONException("JSONArray[$index] is not a JSONArray.")

    internal fun toJsonElement(): JsonArray = JsonArray(list.map { it.toJsonElement() })

    override fun toString(): String = toJsonElement().toString()

    fun toString(indentFactor: Int): String = Json { prettyPrint = indentFactor > 0 }.encodeToString(JsonArray.serializer(), toJsonElement())
}

// endregion

// region Charset

/** java.nio.charset.Charset per le codifiche usate (UTF-8, UTF-16, ISO-8859-1, US-ASCII). */
class Charset private constructor(
    val name: String,
) {
    fun name(): String = name

    fun decode(bytes: ByteArray, offset: Int = 0, length: Int = bytes.size - offset): String {
        val end = offset + length
        return when (name) {
            "UTF-8" -> bytes.decodeToString(offset, end)
            "ISO-8859-1", "US-ASCII" -> buildString(length) { for (i in offset until end) append((bytes[i].toInt() and 0xFF).toChar()) }
            "UTF-16BE" -> decodeUtf16(bytes, offset, end, bigEndian = true)
            "UTF-16LE" -> decodeUtf16(bytes, offset, end, bigEndian = false)
            else -> {
                // UTF-16 con BOM (predefinito big endian)
                if (length >= 2 && (bytes[offset].toInt() and 0xFF) == 0xFF && (bytes[offset + 1].toInt() and 0xFF) == 0xFE) {
                    decodeUtf16(bytes, offset + 2, end, bigEndian = false)
                } else if (length >= 2 && (bytes[offset].toInt() and 0xFF) == 0xFE && (bytes[offset + 1].toInt() and 0xFF) == 0xFF) {
                    decodeUtf16(bytes, offset + 2, end, bigEndian = true)
                } else {
                    decodeUtf16(bytes, offset, end, bigEndian = true)
                }
            }
        }
    }

    fun encode(text: String): ByteArray =
        when (name) {
            "UTF-8" -> text.encodeToByteArray()
            "ISO-8859-1", "US-ASCII" -> ByteArray(text.length) { i -> text[i].code.coerceAtMost(0xFF).toByte() }
            "UTF-16LE" -> ByteArray(text.length * 2) { i -> (text[i / 2].code shr (if (i % 2 == 0) 0 else 8)).toByte() }
            "UTF-16BE" -> ByteArray(text.length * 2) { i -> (text[i / 2].code shr (if (i % 2 == 0) 8 else 0)).toByte() }
            else -> byteArrayOf(0xFE.toByte(), 0xFF.toByte()) + ByteArray(text.length * 2) { i -> (text[i / 2].code shr (if (i % 2 == 0) 8 else 0)).toByte() }
        }

    override fun toString(): String = name

    private fun decodeUtf16(bytes: ByteArray, from: Int, to: Int, bigEndian: Boolean): String =
        buildString {
            var i = from
            while (i + 1 < to) {
                val a = bytes[i].toInt() and 0xFF
                val b = bytes[i + 1].toInt() and 0xFF
                append((if (bigEndian) (a shl 8) or b else (b shl 8) or a).toChar())
                i += 2
            }
        }

    companion object {
        val UTF_8 = Charset("UTF-8")
        val UTF_16 = Charset("UTF-16")
        val UTF_16BE = Charset("UTF-16BE")
        val UTF_16LE = Charset("UTF-16LE")
        val ISO_8859_1 = Charset("ISO-8859-1")
        val US_ASCII = Charset("US-ASCII")

        fun forName(name: String): Charset =
            when (name.uppercase().replace("_", "-")) {
                "UTF8", "UTF-8" -> UTF_8
                "UTF-16" -> UTF_16
                "UTF-16BE" -> UTF_16BE
                "UTF-16LE" -> UTF_16LE
                "ISO-8859-1", "LATIN1" -> ISO_8859_1
                "US-ASCII", "ASCII" -> US_ASCII
                else -> UTF_8
            }

        fun defaultCharset(): Charset = UTF_8
    }
}

object StandardCharsets {
    val UTF_8 = Charset.UTF_8
    val UTF_16 = Charset.UTF_16
    val UTF_16BE = Charset.UTF_16BE
    val UTF_16LE = Charset.UTF_16LE
    val ISO_8859_1 = Charset.ISO_8859_1
    val US_ASCII = Charset.US_ASCII
}

/** kotlin.text.Charsets (solo JVM). */
object Charsets {
    val UTF_8 = Charset.UTF_8
    val UTF_16 = Charset.UTF_16
    val UTF_16BE = Charset.UTF_16BE
    val UTF_16LE = Charset.UTF_16LE
    val ISO_8859_1 = Charset.ISO_8859_1
    val US_ASCII = Charset.US_ASCII
}

fun String.toByteArray(charset: Charset): ByteArray = charset.encode(this)

fun ByteArray.toString(charset: Charset): String = charset.decode(this)

fun String(bytes: ByteArray, charset: Charset): String = charset.decode(bytes)

fun String(bytes: ByteArray, offset: Int, length: Int, charset: Charset): String = charset.decode(bytes, offset, length)

// endregion

// region java.net

object URLEncoder {
    fun encode(s: String, @Suppress("UNUSED_PARAMETER") enc: String = "UTF-8"): String = Uri.encode(s).replace("%20", "+")

    fun encode(s: String, @Suppress("UNUSED_PARAMETER") charset: Charset): String = encode(s)
}

object URLDecoder {
    fun decode(s: String, @Suppress("UNUSED_PARAMETER") enc: String = "UTF-8"): String = Uri.decode(s.replace("+", " "))

    fun decode(s: String, @Suppress("UNUSED_PARAMETER") charset: Charset): String = decode(s)
}

/** java.net.URI (lettura delle parti). */
class URI(
    str: String,
) {
    private val uri = Uri.parse(str)
    val scheme: String? get() = uri.scheme
    val host: String? get() = uri.host
    val port: Int get() = uri.port
    val path: String? get() = uri.path
    val rawPath: String? get() = uri.path
    val query: String? get() = uri.query
    val rawQuery: String? get() = uri.encodedQuery
    val fragment: String? get() = uri.fragment
    val authority: String? get() = uri.authority
    val userInfo: String? get() = uri.authority?.takeIf { '@' in it }?.substringBefore('@')

    override fun toString(): String = uri.toString()

    override fun equals(other: Any?): Boolean = other is URI && other.toString() == toString()

    override fun hashCode(): Int = toString().hashCode()

    /** Risolve un indirizzo relativo rispetto a questo (come java.net.URI.resolve). */
    fun resolve(reference: String): URI {
        if (reference.contains("://")) return URI(reference)
        val origin = "${scheme.orEmpty()}://${authority.orEmpty()}"
        if (reference.startsWith("//")) return URI("${scheme.orEmpty()}:$reference")
        if (reference.startsWith("/")) return URI(origin + reference)
        val basePath = path.orEmpty().substringBeforeLast('/', "")
        val segments = ArrayDeque<String>()
        (basePath + "/" + reference).split('/').forEach { segment ->
            when (segment) {
                "", "." -> Unit
                ".." -> segments.removeLastOrNull()
                else -> segments.addLast(segment)
            }
        }
        return URI(origin + "/" + segments.joinToString("/"))
    }

    companion object {
        fun create(str: String): URI = URI(str)
    }
}

open class SocketTimeoutException(
    message: String? = null,
) : kotlinx.io.IOException(message)

open class InterruptedIOException(
    message: String? = null,
) : kotlinx.io.IOException(message)

// endregion

// region Varie

/** java.util.concurrent.TimeUnit. */
enum class TimeUnit(
    private val nanos: Long,
) {
    NANOSECONDS(1L),
    MICROSECONDS(1_000L),
    MILLISECONDS(1_000_000L),
    SECONDS(1_000_000_000L),
    MINUTES(60_000_000_000L),
    HOURS(3_600_000_000_000L),
    DAYS(86_400_000_000_000L),
    ;

    fun toNanos(duration: Long): Long = duration * nanos

    fun toMillis(duration: Long): Long = duration * nanos / 1_000_000L

    fun toSeconds(duration: Long): Long = duration * nanos / 1_000_000_000L

    fun toMinutes(duration: Long): Long = duration * nanos / 60_000_000_000L

    fun toHours(duration: Long): Long = duration * nanos / 3_600_000_000_000L

    fun toDays(duration: Long): Long = duration * nanos / 86_400_000_000_000L
}

/** android.util.LruCache. */
open class LruCache<K : Any, V : Any>(
    private var maxSize: Int,
) {
    private val map = LinkedHashMap<K, V>()
    private var size = 0

    protected open fun sizeOf(key: K, value: V): Int = 1

    protected open fun entryRemoved(evicted: Boolean, key: K, oldValue: V, newValue: V?) {}

    protected open fun create(key: K): V? = null

    fun get(key: K): V? {
        map.remove(key)?.let { value ->
            map[key] = value
            return value
        }
        val created = create(key) ?: return null
        put(key, created)
        return created
    }

    fun put(key: K, value: V): V? {
        val previous = map.remove(key)
        if (previous != null) size -= sizeOf(key, previous)
        map[key] = value
        size += sizeOf(key, value)
        if (previous != null) entryRemoved(false, key, previous, value)
        trimToSize(maxSize)
        return previous
    }

    fun remove(key: K): V? {
        val previous = map.remove(key) ?: return null
        size -= sizeOf(key, previous)
        entryRemoved(false, key, previous, null)
        return previous
    }

    fun trimToSize(max: Int) {
        while (size > max && map.isNotEmpty()) {
            val eldest = map.entries.first()
            map.remove(eldest.key)
            size -= sizeOf(eldest.key, eldest.value)
            entryRemoved(true, eldest.key, eldest.value, null)
        }
    }

    fun evictAll() = trimToSize(-1)

    fun size(): Int = size

    fun maxSize(): Int = maxSize

    fun resize(newMax: Int) {
        maxSize = newMax
        trimToSize(newMax)
    }

    fun snapshot(): Map<K, V> = LinkedHashMap(map)
}

/** android.util.AtomicFile: scrive in un file temporaneo e poi lo rinomina. */
class AtomicFile(
    val baseFile: File,
) {
    private val newFile = File(baseFile.path + ".new")

    fun readFully(): ByteArray = baseFile.readBytes()

    fun exists(): Boolean = baseFile.exists()

    fun delete() {
        baseFile.delete()
        newFile.delete()
    }

    /** Scrittura atomica: il blocco riceve i byte da scrivere. */
    fun write(bytes: ByteArray) {
        newFile.writeBytes(bytes)
        baseFile.delete()
        newFile.renameTo(baseFile)
    }

    fun writeText(text: String) = write(text.encodeToByteArray())

    /** Come android.util.AtomicFile: si scrive su un file ".new" che sostituisce l'originale a fine scrittura. */
    fun startWrite(): FileOutputStream {
        baseFile.parentFile?.mkdirs()
        newFile.delete()
        return FileOutputStream(newFile)
    }

    fun finishWrite(stream: FileOutputStream) {
        stream.close()
        baseFile.delete()
        newFile.renameTo(baseFile)
    }

    fun failWrite(stream: FileOutputStream) {
        runCatching { stream.close() }
        newFile.delete()
    }

    fun openRead(): InputStream = ByteArrayInputStream(baseFile.readBytes())
}

/** android.text.format.Formatter. */
object Formatter {
    fun formatShortFileSize(@Suppress("UNUSED_PARAMETER") context: Context?, sizeBytes: Long): String = formatFileSize(context, sizeBytes)

    fun formatFileSize(@Suppress("UNUSED_PARAMETER") context: Context?, sizeBytes: Long): String {
        val units = listOf("B", "kB", "MB", "GB", "TB")
        var value = sizeBytes.toDouble()
        var unit = 0
        while (value >= 1000 && unit < units.lastIndex) {
            value /= 1000
            unit++
        }
        return if (unit == 0) "$sizeBytes B" else DecimalFormat(if (value < 10) "#.##" else if (value < 100) "#.#" else "#").format(value) + " " + units[unit]
    }
}

/** android.text.format.DateUtils. */
object DateUtils {
    const val SECOND_IN_MILLIS = 1000L
    const val MINUTE_IN_MILLIS = 60_000L
    const val HOUR_IN_MILLIS = 3_600_000L
    const val DAY_IN_MILLIS = 86_400_000L
    const val WEEK_IN_MILLIS = 604_800_000L
}

/** java.text.DateFormat (formati brevi/medi). */
class DateFormat private constructor(
    private val pattern: String,
) {
    fun format(date: Date): String = SimpleDateFormat(pattern).format(date)

    fun format(millis: Long): String = SimpleDateFormat(pattern).format(Date(millis))

    companion object {
        const val FULL = 0
        const val LONG = 1
        const val MEDIUM = 2
        const val SHORT = 3
        const val DEFAULT = MEDIUM

        /** android.text.format.DateFormat.is24HourFormat. */
        fun is24HourFormat(
            @Suppress("UNUSED_PARAMETER") context: Context,
        ): Boolean = platformIs24HourFormat()

        fun getTimeInstance(style: Int = DEFAULT, @Suppress("UNUSED_PARAMETER") locale: Locale? = null): DateFormat = DateFormat(if (style == SHORT) "HH:mm" else "HH:mm:ss")

        fun getDateInstance(style: Int = DEFAULT, @Suppress("UNUSED_PARAMETER") locale: Locale? = null): DateFormat = DateFormat(if (style == SHORT) "dd/MM/yy" else "d MMM yyyy")

        fun getDateTimeInstance(dateStyle: Int = DEFAULT, timeStyle: Int = DEFAULT, @Suppress("UNUSED_PARAMETER") locale: Locale? = null): DateFormat =
            DateFormat((if (dateStyle == SHORT) "dd/MM/yy" else "d MMM yyyy") + " " + (if (timeStyle == SHORT) "HH:mm" else "HH:mm:ss"))
    }
}

/** java.lang.UnicodeScript (solo gli script usati per i testi). */
enum class UnicodeScript {
    COMMON,
    LATIN,
    GREEK,
    CYRILLIC,
    ARABIC,
    HEBREW,
    DEVANAGARI,
    THAI,
    HANGUL,
    HIRAGANA,
    KATAKANA,
    HAN,
    INHERITED,
    UNKNOWN,
    ;

    companion object {
        fun of(codePoint: Int): UnicodeScript =
            when (codePoint) {
                in 0x0041..0x005A, in 0x0061..0x007A, in 0x00C0..0x024F, in 0x1E00..0x1EFF -> LATIN
                in 0x0370..0x03FF -> GREEK
                in 0x0400..0x052F -> CYRILLIC
                in 0x0590..0x05FF -> HEBREW
                in 0x0600..0x06FF, in 0x0750..0x077F -> ARABIC
                in 0x0900..0x097F -> DEVANAGARI
                in 0x0E00..0x0E7F -> THAI
                in 0x1100..0x11FF, in 0x3130..0x318F, in 0xAC00..0xD7AF -> HANGUL
                in 0x3040..0x309F -> HIRAGANA
                in 0x30A0..0x30FF, in 0x31F0..0x31FF, in 0xFF66..0xFF9F -> KATAKANA
                in 0x4E00..0x9FFF, in 0x3400..0x4DBF, in 0xF900..0xFAFF, in 0x20000..0x2A6DF -> HAN
                in 0x0300..0x036F -> INHERITED
                else -> COMMON
            }
    }
}

// endregion

/** MutableMap.putIfAbsent di Java. */
fun <K, V> MutableMap<K, V>.putIfAbsent(key: K, value: V): V? {
    val existing = get(key)
    if (existing == null) put(key, value)
    return existing
}

/** MutableMap.computeIfAbsent di Java. */
fun <K, V> MutableMap<K, V>.computeIfAbsent(key: K, mappingFunction: (K) -> V): V = getOrPut(key) { mappingFunction(key) }

/** MutableMap.getOrDefault di Java. */
fun <K, V> Map<K, V>.getOrDefault(key: K, defaultValue: V): V = get(key) ?: defaultValue

open class ConnectException(
    message: String? = null,
) : kotlinx.io.IOException(message)

open class UnknownHostException(
    message: String? = null,
) : kotlinx.io.IOException(message)

/** java.net.Proxy: qui solo il tipo (le impostazioni vere sono innertube ProxySettings). */
object Proxy {
    enum class Type { DIRECT, HTTP, SOCKS }
}

/** java.security.SecureRandom (byte casuali sicuri della piattaforma). */
class SecureRandom {
    fun nextBytes(bytes: ByteArray) = platformSecureRandomBytes(bytes)

    fun nextInt(bound: Int): Int {
        val b = ByteArray(4)
        nextBytes(b)
        val v = ((b[0].toInt() and 0x7F) shl 24) or ((b[1].toInt() and 0xFF) shl 16) or ((b[2].toInt() and 0xFF) shl 8) or (b[3].toInt() and 0xFF)
        return v % bound
    }
}

internal expect fun platformSecureRandomBytes(bytes: ByteArray)

/** java.net.URL (solo per aprire connessioni HTTP). */
class URL(
    private val spec: String,
) {
    fun openConnection(): HttpURLConnection = HttpURLConnection(spec)

    override fun toString(): String = spec
}

/** java.net.HttpURLConnection su OkHttp compat (Ktor): la richiesta parte alla prima lettura. */
class HttpURLConnection internal constructor(
    private val url: String,
) {
    var requestMethod: String = "GET"
    var connectTimeout: Int = 0
    var readTimeout: Int = 0
    var doOutput: Boolean = false
    var doInput: Boolean = true
    var instanceFollowRedirects: Boolean = true
    var useCaches: Boolean = false
    private val properties = LinkedHashMap<String, String>()
    private val body = ByteArrayOutputStream()
    private var response: moe.rukamori.archivetune.compat.okhttp3.Response? = null

    fun setRequestProperty(key: String, value: String) {
        properties[key] = value
    }

    fun addRequestProperty(key: String, value: String) {
        properties[key] = properties[key]?.let { "$it, $value" } ?: value
    }

    val outputStream: OutputStream
        get() = body

    private fun execute(): moe.rukamori.archivetune.compat.okhttp3.Response {
        response?.let { return it }
        val builder = moe.rukamori.archivetune.compat.okhttp3.Request.Builder().url(url)
        properties.forEach { (k, v) -> builder.header(k, v) }
        if (doOutput || requestMethod != "GET") {
            val type = properties.entries.firstOrNull { it.key.equals("Content-Type", true) }?.value
            builder.method(requestMethod, moe.rukamori.archivetune.compat.okhttp3.RequestBody(body.toByteArray(), type?.let { moe.rukamori.archivetune.compat.okhttp3.MediaType(it) }))
        }
        val client =
            moe.rukamori.archivetune.compat.okhttp3.OkHttpClient
                .Builder()
                .apply {
                    if (connectTimeout > 0) connectTimeout(connectTimeout.toLong(), TimeUnit.MILLISECONDS)
                    if (readTimeout > 0) readTimeout(readTimeout.toLong(), TimeUnit.MILLISECONDS)
                    followRedirects(instanceFollowRedirects)
                }.build()
        return client.newCall(builder.build()).execute().also { response = it }
    }

    val responseCode: Int
        get() = execute().code

    val responseMessage: String
        get() = execute().message

    val inputStream: InputStream
        get() {
            val r = execute()
            if (r.code >= 400) throw kotlinx.io.IOException("HTTP ${r.code}")
            return ByteArrayInputStream(r.body?.bytes() ?: ByteArray(0))
        }

    val errorStream: InputStream?
        get() = execute().takeIf { it.code >= 400 }?.body?.bytes()?.let(::ByteArrayInputStream)

    fun getHeaderField(name: String): String? = execute().header(name)

    val contentLength: Int
        get() = execute().body?.bytes()?.size ?: -1

    fun connect() {
        execute()
    }

    fun disconnect() = Unit

    companion object {
        const val HTTP_OK = 200
        const val HTTP_NOT_FOUND = 404
        const val HTTP_UNAUTHORIZED = 401
    }
}

/** Map.toSortedMap() di Kotlin/JVM: qui una LinkedHashMap ordinata per chiave. */
fun <K : Comparable<K>, V> Map<out K, V>.toSortedMap(): Map<K, V> =
    entries.sortedBy { it.key }.associateTo(LinkedHashMap()) { it.key to it.value }

fun <K, V> Map<out K, V>.toSortedMap(comparator: Comparator<in K>): Map<K, V> =
    entries.sortedWith { a, b -> comparator.compare(a.key, b.key) }.associateTo(LinkedHashMap()) { it.key to it.value }
