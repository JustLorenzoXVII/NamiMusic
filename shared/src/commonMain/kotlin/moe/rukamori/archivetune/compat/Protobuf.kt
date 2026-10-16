package moe.rukamori.archivetune.compat

import kotlinx.io.IOException

/** Sottoinsieme di com.google.protobuf.CodedOutputStream: campi stringa/bytes e varint. */
class CodedOutputStream private constructor(
    private val out: OutputStream,
) {
    fun writeTag(fieldNumber: Int, wireType: Int) = writeVarint(((fieldNumber shl 3) or wireType).toLong())

    fun writeVarint(value: Long) {
        var v = value
        while (true) {
            if (v and 0x7FL.inv() == 0L) {
                out.write(v.toInt())
                return
            }
            out.write(((v and 0x7F) or 0x80).toInt())
            v = v ushr 7
        }
    }

    fun writeString(fieldNumber: Int, value: String) = writeByteArray(fieldNumber, value.encodeToByteArray())

    fun writeByteArray(fieldNumber: Int, value: ByteArray) {
        writeTag(fieldNumber, 2)
        writeVarint(value.size.toLong())
        out.write(value)
    }

    fun writeBytes(fieldNumber: Int, value: ByteArray) = writeByteArray(fieldNumber, value)

    fun writeInt32(fieldNumber: Int, value: Int) {
        writeTag(fieldNumber, 0)
        writeVarint(value.toLong())
    }

    fun writeInt64(fieldNumber: Int, value: Long) {
        writeTag(fieldNumber, 0)
        writeVarint(value)
    }

    fun writeBool(fieldNumber: Int, value: Boolean) = writeInt32(fieldNumber, if (value) 1 else 0)

    fun flush() = out.flush()

    companion object {
        fun newInstance(out: OutputStream): CodedOutputStream = CodedOutputStream(out)
    }
}

/** Sottoinsieme di com.google.protobuf.CodedInputStream. */
class CodedInputStream private constructor(
    private val data: ByteArray,
) {
    private var pos = 0

    val isAtEnd: Boolean
        get() = pos >= data.size

    fun readTag(): Int = if (isAtEnd) 0 else readRawVarint64().toInt()

    fun readRawVarint64(): Long {
        var shift = 0
        var result = 0L
        while (shift < 64) {
            if (pos >= data.size) throw IOException("Varint troncato")
            val b = data[pos++].toInt()
            result = result or ((b and 0x7F).toLong() shl shift)
            if (b and 0x80 == 0) return result
            shift += 7
        }
        throw IOException("Varint non valido")
    }

    fun readInt32(): Int = readRawVarint64().toInt()

    fun readInt64(): Long = readRawVarint64()

    fun readBool(): Boolean = readRawVarint64() != 0L

    fun readByteArray(): ByteArray {
        val length = readRawVarint64().toInt()
        if (length < 0 || pos + length > data.size) throw IOException("Lunghezza non valida")
        return data.copyOfRange(pos, pos + length).also { pos += length }
    }

    fun readString(): String = readByteArray().decodeToString()

    fun readStringRequireUtf8(): String = readString()

    fun skipField(tag: Int): Boolean {
        when (tag and 7) {
            0 -> readRawVarint64()
            1 -> pos += 8
            2 -> readByteArray()
            5 -> pos += 4
            else -> return false
        }
        return pos <= data.size
    }

    companion object {
        fun newInstance(data: ByteArray): CodedInputStream = CodedInputStream(data)
    }
}

/** java.util.Base64 su kotlin.io.encoding.Base64. */
@OptIn(kotlin.io.encoding.ExperimentalEncodingApi::class)
object Base64 {
    class Decoder internal constructor(
        private val codec: kotlin.io.encoding.Base64,
    ) {
        fun decode(value: String): ByteArray = codec.decode(value)

        fun decode(value: ByteArray): ByteArray = codec.decode(value)
    }

    class Encoder internal constructor(
        private val codec: kotlin.io.encoding.Base64,
        private val padding: Boolean,
    ) {
        fun encodeToString(value: ByteArray): String = codec.encode(value).let { if (padding) it else it.trimEnd('=') }

        fun encode(value: ByteArray): ByteArray = encodeToString(value).encodeToByteArray()

        fun withoutPadding(): Encoder = Encoder(codec, padding = false)
    }

    private val lenientUrl = kotlin.io.encoding.Base64.UrlSafe.withPadding(kotlin.io.encoding.Base64.PaddingOption.ABSENT_OPTIONAL)
    private val lenientDefault = kotlin.io.encoding.Base64.Default.withPadding(kotlin.io.encoding.Base64.PaddingOption.ABSENT_OPTIONAL)

    fun getDecoder(): Decoder = Decoder(lenientDefault)

    fun getUrlDecoder(): Decoder = Decoder(lenientUrl)

    fun getMimeDecoder(): Decoder = Decoder(kotlin.io.encoding.Base64.Mime)

    fun getEncoder(): Encoder = Encoder(kotlin.io.encoding.Base64.Default, padding = true)

    fun getUrlEncoder(): Encoder = Encoder(kotlin.io.encoding.Base64.UrlSafe, padding = true)
}

/** android.util.Base64 (flag DEFAULT, NO_PADDING, NO_WRAP, URL_SAFE). */
@OptIn(kotlin.io.encoding.ExperimentalEncodingApi::class)
object AndroidBase64 {
    const val DEFAULT = 0
    const val NO_PADDING = 1
    const val NO_WRAP = 2
    const val CRLF = 4
    const val URL_SAFE = 8

    private fun codec(flags: Int): kotlin.io.encoding.Base64 {
        val base = if (flags and URL_SAFE != 0) kotlin.io.encoding.Base64.UrlSafe else kotlin.io.encoding.Base64.Default
        return base.withPadding(kotlin.io.encoding.Base64.PaddingOption.ABSENT_OPTIONAL)
    }

    fun encodeToString(input: ByteArray, flags: Int): String {
        val base = if (flags and URL_SAFE != 0) kotlin.io.encoding.Base64.UrlSafe else kotlin.io.encoding.Base64.Default
        val encoded = base.encode(input)
        val unpadded = if (flags and NO_PADDING != 0) encoded.trimEnd('=') else encoded
        return if (flags and NO_WRAP != 0) unpadded else unpadded.chunked(76).joinToString("\n", postfix = "\n")
    }

    fun encode(input: ByteArray, flags: Int): ByteArray = encodeToString(input, flags).encodeToByteArray()

    fun decode(input: String, flags: Int): ByteArray = codec(flags).decode(input.filterNot { it.isWhitespace() })

    fun decode(input: ByteArray, flags: Int): ByteArray = decode(input.decodeToString(), flags)
}
