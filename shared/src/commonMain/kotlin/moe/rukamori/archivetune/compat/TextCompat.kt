package moe.rukamori.archivetune.compat

import moe.rukamori.archivetune.compat.time.DateTimeFormatter
import moe.rukamori.archivetune.compat.time.Instant
import moe.rukamori.archivetune.compat.time.LocalDateTime
import moe.rukamori.archivetune.compat.time.ZoneId
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToLong

/** java.util.Date (solo i millisecondi). */
class Date(
    val time: Long = System.currentTimeMillis(),
) : Comparable<Date> {
    override fun compareTo(other: Date): Int = time.compareTo(other.time)
}

/** java.util.TimeZone (solo quello di sistema). */
class TimeZone private constructor(
    internal val zone: ZoneId,
) {
    val id: String
        get() = zone.id

    companion object {
        fun getDefault(): TimeZone = TimeZone(ZoneId.systemDefault())

        fun getTimeZone(id: String): TimeZone = TimeZone(runCatching { ZoneId.of(id) }.getOrDefault(ZoneId.systemDefault()))
    }
}

/** java.text.SimpleDateFormat per formattare date con un modello (yyyy-MM-dd HH:mm:ss...). */
class SimpleDateFormat(
    pattern: String,
    @Suppress("UNUSED_PARAMETER") locale: Locale? = null,
) {
    private val formatter = DateTimeFormatter.ofPattern(pattern)
    var timeZone: TimeZone = TimeZone.getDefault()

    fun format(date: Date): String = formatter.format(LocalDateTime.ofInstant(Instant.ofEpochMilli(date.time), timeZone.zone))

    fun format(millis: Long): String = format(Date(millis))
}

/**
 * java.text.DecimalFormat per i modelli usati da ArchiveTune ("#.#", "#.##", "0.0", "#,###").
 * Il separatore decimale è il punto, quello delle migliaia la virgola.
 */
class DecimalFormat(
    private val pattern: String,
) {
    private val grouping = pattern.contains(',')
    private val decimalPart = pattern.substringAfter('.', "")
    private val maxDecimals = decimalPart.length
    private val minDecimals = decimalPart.count { it == '0' }

    fun format(value: Number): String = format(value.toDouble())

    fun format(value: Double): String {
        val negative = value < 0
        val factor = 10.0.pow(maxDecimals)
        val rounded = (abs(value) * factor).roundToLong()
        val integerPart = rounded / factor.toLong()
        var fraction = if (maxDecimals > 0) (rounded % factor.toLong()).toString().padStart(maxDecimals, '0') else ""
        while (fraction.length > minDecimals && fraction.endsWith('0')) fraction = fraction.dropLast(1)
        val integerText =
            if (grouping) {
                integerPart.toString().reversed().chunked(3).joinToString(",").reversed()
            } else {
                integerPart.toString()
            }
        return (if (negative && (integerPart > 0 || fraction.any { it != '0' })) "-" else "") +
            integerText + (if (fraction.isNotEmpty()) ".$fraction" else "")
    }
}

/** android.util.Log: livelli e scrittura sul log della piattaforma. */
object Log {
    const val VERBOSE = 2
    const val DEBUG = 3
    const val INFO = 4
    const val WARN = 5
    const val ERROR = 6
    const val ASSERT = 7

    fun v(tag: String?, msg: String): Int = 0.also { platformLog("V", tag.orEmpty(), msg) }

    fun d(tag: String?, msg: String): Int = 0.also { platformLog("D", tag.orEmpty(), msg) }

    fun i(tag: String?, msg: String): Int = 0.also { platformLog("I", tag.orEmpty(), msg) }

    fun w(tag: String?, msg: String): Int = 0.also { platformLog("W", tag.orEmpty(), msg) }

    fun w(tag: String?, msg: String, tr: Throwable?): Int = 0.also { platformLog("W", tag.orEmpty(), msg + (tr?.let { "\n$it" } ?: "")) }

    fun e(tag: String?, msg: String): Int = 0.also { platformLog("E", tag.orEmpty(), msg) }

    fun e(tag: String?, msg: String, tr: Throwable?): Int = 0.also { platformLog("E", tag.orEmpty(), msg + (tr?.let { "\n$it" } ?: "")) }
}

/** java.security.MessageDigest per MD5, SHA-1 e SHA-256 (implementati in Kotlin). */
class MessageDigest private constructor(
    private val algorithm: String,
) {
    private val buffer = mutableListOf<Byte>()

    fun update(input: ByteArray) {
        buffer += input.toList()
    }

    fun digest(): ByteArray = digest(ByteArray(0))

    fun digest(input: ByteArray): ByteArray {
        val data = buffer.toByteArray() + input
        buffer.clear()
        return when (algorithm) {
            "MD5" -> Hashes.md5(data)
            "SHA-1", "SHA1" -> Hashes.sha1(data)
            else -> Hashes.sha256(data)
        }
    }

    companion object {
        fun getInstance(algorithm: String): MessageDigest = MessageDigest(algorithm.uppercase())
    }
}

/** java.math.BigInteger quanto basta per BigInteger(1, bytes).toString(16). */
class BigInteger(
    @Suppress("UNUSED_PARAMETER") signum: Int,
    private val magnitude: ByteArray,
) {
    fun toString(radix: Int): String {
        require(radix == 16) { "Solo base 16" }
        return magnitude.joinToString("") { (it.toInt() and 0xFF).toString(16).padStart(2, '0') }.trimStart('0').ifEmpty { "0" }
    }

    override fun toString(): String = toString(16)
}

fun ByteArray.toHexString(): String = joinToString("") { (it.toInt() and 0xFF).toString(16).padStart(2, '0') }

internal object Hashes {
    fun md5(message: ByteArray): ByteArray {
        val s =
            intArrayOf(
                7, 12, 17, 22, 7, 12, 17, 22, 7, 12, 17, 22, 7, 12, 17, 22,
                5, 9, 14, 20, 5, 9, 14, 20, 5, 9, 14, 20, 5, 9, 14, 20,
                4, 11, 16, 23, 4, 11, 16, 23, 4, 11, 16, 23, 4, 11, 16, 23,
                6, 10, 15, 21, 6, 10, 15, 21, 6, 10, 15, 21, 6, 10, 15, 21,
            )
        val k = IntArray(64) { i -> (abs(kotlin.math.sin((i + 1).toDouble())) * 4294967296.0).toLong().toInt() }
        var a0 = 0x67452301
        var b0 = 0xefcdab89.toInt()
        var c0 = 0x98badcfe.toInt()
        var d0 = 0x10325476
        val padded = pad(message, littleEndian = true)
        for (chunk in padded.indices step 64) {
            val m = IntArray(16) { i -> readIntLE(padded, chunk + i * 4) }
            var a = a0
            var b = b0
            var c = c0
            var d = d0
            for (i in 0 until 64) {
                var f: Int
                val g: Int
                when (i / 16) {
                    0 -> {
                        f = (b and c) or (b.inv() and d)
                        g = i
                    }
                    1 -> {
                        f = (d and b) or (d.inv() and c)
                        g = (5 * i + 1) % 16
                    }
                    2 -> {
                        f = b xor c xor d
                        g = (3 * i + 5) % 16
                    }
                    else -> {
                        f = c xor (b or d.inv())
                        g = (7 * i) % 16
                    }
                }
                f += a + k[i] + m[g]
                a = d
                d = c
                c = b
                b += f.rotateLeft(s[i])
            }
            a0 += a
            b0 += b
            c0 += c
            d0 += d
        }
        return intArrayOf(a0, b0, c0, d0).flatMap { v -> (0 until 4).map { (v ushr (8 * it)).toByte() } }.toByteArray()
    }

    fun sha1(message: ByteArray): ByteArray {
        var h0 = 0x67452301
        var h1 = 0xEFCDAB89.toInt()
        var h2 = 0x98BADCFE.toInt()
        var h3 = 0x10325476
        var h4 = 0xC3D2E1F0.toInt()
        val padded = pad(message, littleEndian = false)
        val w = IntArray(80)
        for (chunk in padded.indices step 64) {
            for (i in 0 until 16) w[i] = readIntBE(padded, chunk + i * 4)
            for (i in 16 until 80) w[i] = (w[i - 3] xor w[i - 8] xor w[i - 14] xor w[i - 16]).rotateLeft(1)
            var a = h0
            var b = h1
            var c = h2
            var d = h3
            var e = h4
            for (i in 0 until 80) {
                val (f, k) =
                    when (i / 20) {
                        0 -> ((b and c) or (b.inv() and d)) to 0x5A827999
                        1 -> (b xor c xor d) to 0x6ED9EBA1
                        2 -> ((b and c) or (b and d) or (c and d)) to 0x8F1BBCDC.toInt()
                        else -> (b xor c xor d) to 0xCA62C1D6.toInt()
                    }
                val temp = a.rotateLeft(5) + f + e + k + w[i]
                e = d
                d = c
                c = b.rotateLeft(30)
                b = a
                a = temp
            }
            h0 += a
            h1 += b
            h2 += c
            h3 += d
            h4 += e
        }
        return intArrayOf(h0, h1, h2, h3, h4).flatMap { v -> (3 downTo 0).map { (v ushr (8 * it)).toByte() } }.toByteArray()
    }

    fun sha256(message: ByteArray): ByteArray {
        val k =
            intArrayOf(
                0x428a2f98, 0x71374491, -0x4a3f0431, -0x164a245b, 0x3956c25b, 0x59f111f1, -0x6dc07d5c, -0x54e3a12b,
                -0x27f85568, 0x12835b01, 0x243185be, 0x550c7dc3, 0x72be5d74, -0x7f214e02, -0x6423f959, -0x3e640e8c,
                -0x1b64963f, -0x1041b87a, 0x0fc19dc6, 0x240ca1cc, 0x2de92c6f, 0x4a7484aa, 0x5cb0a9dc, 0x76f988da,
                -0x67c1aeae, -0x57ce3993, -0x4ffcd838, -0x40a68039, -0x391ff40d, -0x2a586eb9, 0x06ca6351, 0x14292967,
                0x27b70a85, 0x2e1b2138, 0x4d2c6dfc, 0x53380d13, 0x650a7354, 0x766a0abb, -0x7e3d36d2, -0x6d8dd37b,
                -0x5d40175f, -0x57e599b5, -0x3db47490, -0x3893ae5d, -0x2e6d17e7, -0x2966f9dc, -0xbf1ca7b, 0x106aa070,
                0x19a4c116, 0x1e376c08, 0x2748774c, 0x34b0bcb5, 0x391c0cb3, 0x4ed8aa4a, 0x5b9cca4f, 0x682e6ff3,
                0x748f82ee, 0x78a5636f, -0x7b3787ec, -0x7338fdf8, -0x6f410006, -0x5baf9315, -0x41065c09, -0x398e870e,
            )
        val h = intArrayOf(0x6a09e667, -0x4498517b, 0x3c6ef372, -0x5ab00ac6, 0x510e527f, -0x64fa9774, 0x1f83d9ab, 0x5be0cd19)
        val padded = pad(message, littleEndian = false)
        val w = IntArray(64)
        for (chunk in padded.indices step 64) {
            for (i in 0 until 16) w[i] = readIntBE(padded, chunk + i * 4)
            for (i in 16 until 64) {
                val s0 = w[i - 15].rotateRight(7) xor w[i - 15].rotateRight(18) xor (w[i - 15] ushr 3)
                val s1 = w[i - 2].rotateRight(17) xor w[i - 2].rotateRight(19) xor (w[i - 2] ushr 10)
                w[i] = w[i - 16] + s0 + w[i - 7] + s1
            }
            var a = h[0]
            var b = h[1]
            var c = h[2]
            var d = h[3]
            var e = h[4]
            var f = h[5]
            var g = h[6]
            var hh = h[7]
            for (i in 0 until 64) {
                val s1 = e.rotateRight(6) xor e.rotateRight(11) xor e.rotateRight(25)
                val ch = (e and f) xor (e.inv() and g)
                val temp1 = hh + s1 + ch + k[i] + w[i]
                val s0 = a.rotateRight(2) xor a.rotateRight(13) xor a.rotateRight(22)
                val maj = (a and b) xor (a and c) xor (b and c)
                val temp2 = s0 + maj
                hh = g
                g = f
                f = e
                e = d + temp1
                d = c
                c = b
                b = a
                a = temp1 + temp2
            }
            h[0] += a
            h[1] += b
            h[2] += c
            h[3] += d
            h[4] += e
            h[5] += f
            h[6] += g
            h[7] += hh
        }
        return h.flatMap { v -> (3 downTo 0).map { (v ushr (8 * it)).toByte() } }.toByteArray()
    }

    private fun pad(
        message: ByteArray,
        littleEndian: Boolean,
    ): ByteArray {
        val bitLength = message.size.toLong() * 8
        var length = message.size + 1
        while (length % 64 != 56) length++
        val out = ByteArray(length + 8)
        message.copyInto(out)
        out[message.size] = 0x80.toByte()
        for (i in 0 until 8) {
            val byte = (bitLength ushr (8 * i)).toByte()
            if (littleEndian) out[length + i] = byte else out[length + 7 - i] = byte
        }
        return out
    }

    private fun readIntLE(
        b: ByteArray,
        o: Int,
    ): Int = (b[o].toInt() and 0xFF) or ((b[o + 1].toInt() and 0xFF) shl 8) or ((b[o + 2].toInt() and 0xFF) shl 16) or ((b[o + 3].toInt() and 0xFF) shl 24)

    private fun readIntBE(
        b: ByteArray,
        o: Int,
    ): Int = ((b[o].toInt() and 0xFF) shl 24) or ((b[o + 1].toInt() and 0xFF) shl 16) or ((b[o + 2].toInt() and 0xFF) shl 8) or (b[o + 3].toInt() and 0xFF)
}

/** String.toByteArray() di Java (UTF-8). */
fun String.toByteArray(): ByteArray = encodeToByteArray()

/** java.util.UUID (solo randomUUID e toString). */
class UUID private constructor(
    private val value: String,
) {
    override fun toString(): String = value

    override fun equals(other: Any?): Boolean = other is UUID && other.value == value

    override fun hashCode(): Int = value.hashCode()

    companion object {
        @OptIn(kotlin.uuid.ExperimentalUuidApi::class)
        fun randomUUID(): UUID = UUID(kotlin.uuid.Uuid.random().toString())

        fun fromString(name: String): UUID = UUID(name.lowercase())

        @OptIn(kotlin.uuid.ExperimentalUuidApi::class)
        fun nameUUIDFromBytes(name: ByteArray): UUID {
            val md5 = Hashes.md5(name)
            md5[6] = ((md5[6].toInt() and 0x0f) or 0x30).toByte()
            md5[8] = ((md5[8].toInt() and 0x3f) or 0x80).toByte()
            return UUID(kotlin.uuid.Uuid.fromByteArray(md5).toString())
        }
    }
}
