package moe.rukamori.archivetune.compat

import kotlinx.io.IOException

/** java.io.InputStream (sottoinsieme). */
abstract class InputStream : AutoCloseable {
    abstract fun read(): Int

    open fun read(buffer: ByteArray, offset: Int = 0, length: Int = buffer.size - offset): Int {
        var count = 0
        while (count < length) {
            val b = read()
            if (b < 0) return if (count == 0) -1 else count
            buffer[offset + count] = b.toByte()
            count++
        }
        return count
    }

    open fun readBytes(): ByteArray {
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        while (true) {
            val n = read(buffer, 0, buffer.size)
            if (n < 0) break
            out.write(buffer, 0, n)
        }
        return out.toByteArray()
    }

    open fun readAllBytes(): ByteArray = readBytes()

    open fun available(): Int = 0

    open fun skip(n: Long): Long {
        var skipped = 0L
        while (skipped < n && read() >= 0) skipped++
        return skipped
    }

    override fun close() = Unit

    fun bufferedReader(charset: Charset = Charset.UTF_8): BufferedReader = BufferedReader(charset.decode(readBytes()))

    fun reader(charset: Charset = Charset.UTF_8): BufferedReader = bufferedReader(charset)

    fun buffered(): InputStream = this
}

class ByteArrayInputStream(
    private val data: ByteArray,
    private var position: Int = 0,
    length: Int = data.size - position,
) : InputStream() {
    private val end = (position + length).coerceAtMost(data.size)

    override fun read(): Int = if (position < end) data[position++].toInt() and 0xFF else -1

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        if (position >= end) return -1
        val n = minOf(length, end - position)
        data.copyInto(buffer, offset, position, position + n)
        position += n
        return n
    }

    override fun available(): Int = end - position
}

/** java.io.OutputStream (sottoinsieme). */
abstract class OutputStream : AutoCloseable {
    abstract fun write(b: Int)

    open fun write(bytes: ByteArray) = write(bytes, 0, bytes.size)

    open fun write(bytes: ByteArray, offset: Int, length: Int) {
        for (i in offset until offset + length) write(bytes[i].toInt())
    }

    open fun flush() = Unit

    override fun close() = Unit

    fun bufferedWriter(charset: Charset = Charset.UTF_8): Writer = Writer(this, charset)

    fun writer(charset: Charset = Charset.UTF_8): Writer = Writer(this, charset)

    fun buffered(): OutputStream = this
}

class ByteArrayOutputStream(
    initialSize: Int = 32,
) : OutputStream() {
    private var buffer = ByteArray(initialSize.coerceAtLeast(1))
    private var count = 0

    override fun write(b: Int) {
        ensure(count + 1)
        buffer[count++] = b.toByte()
    }

    override fun write(bytes: ByteArray, offset: Int, length: Int) {
        ensure(count + length)
        bytes.copyInto(buffer, count, offset, offset + length)
        count += length
    }

    private fun ensure(capacity: Int) {
        if (capacity > buffer.size) buffer = buffer.copyOf(maxOf(capacity, buffer.size * 2))
    }

    fun toByteArray(): ByteArray = buffer.copyOf(count)

    fun size(): Int = count

    fun reset() {
        count = 0
    }

    override fun toString(): String = buffer.decodeToString(0, count)
}

class FileInputStream(
    file: File,
) : InputStream() {
    constructor(path: String) : this(File(path))

    private val delegate = ByteArrayInputStream(file.readBytes())

    override fun read(): Int = delegate.read()

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int = delegate.read(buffer, offset, length)

    override fun available(): Int = delegate.available()
}

/** Scrive su file: i byte si accumulano e vengono salvati a ogni flush/close. */
class FileOutputStream(
    private val file: File,
    private val append: Boolean = false,
) : OutputStream() {
    constructor(path: String, append: Boolean = false) : this(File(path), append)

    private val pending = ByteArrayOutputStream()
    private var started = append

    override fun write(b: Int) = pending.write(b)

    override fun write(bytes: ByteArray, offset: Int, length: Int) = pending.write(bytes, offset, length)

    override fun flush() {
        if (pending.size() == 0 && started) return
        if (started) file.appendBytes(pending.toByteArray()) else file.writeBytes(pending.toByteArray())
        started = true
        pending.reset()
    }

    override fun close() = flush()
}

/** java.io.Writer / BufferedWriter. */
class Writer internal constructor(
    private val out: OutputStream,
    private val charset: Charset,
) : Appendable, AutoCloseable {
    private val sb = StringBuilder()

    fun write(text: String) {
        sb.append(text)
    }

    fun write(c: Int) {
        sb.append(c.toChar())
    }

    fun newLine() {
        sb.append('\n')
    }

    override fun append(value: Char): Writer = apply { sb.append(value) }

    override fun append(value: CharSequence?): Writer = apply { sb.append(value) }

    override fun append(value: CharSequence?, startIndex: Int, endIndex: Int): Writer = apply { sb.append(value, startIndex, endIndex) }

    fun flush() {
        if (sb.isNotEmpty()) {
            out.write(charset.encode(sb.toString()))
            sb.clear()
        }
        out.flush()
    }

    override fun close() {
        flush()
        out.close()
    }
}

/** java.io.BufferedReader su un testo già letto. */
class BufferedReader(
    private val text: String,
) : AutoCloseable {
    private var position = 0

    fun readText(): String = text.substring(position).also { position = text.length }

    fun readLine(): String? {
        if (position >= text.length) return null
        val end = text.indexOf('\n', position).let { if (it < 0) text.length else it }
        val line = text.substring(position, end).removeSuffix("\r")
        position = end + 1
        return line
    }

    fun lineSequence(): Sequence<String> = generateSequence { readLine() }

    fun readLines(): List<String> = lineSequence().toList()

    fun <T> useLines(block: (Sequence<String>) -> T): T = block(lineSequence())

    fun forEachLine(action: (String) -> Unit) = lineSequence().forEach(action)

    fun read(): Int = if (position < text.length) text[position++].code else -1

    override fun close() = Unit
}

/** android.content.ContentResolver: legge e scrive i documenti scelti dall'utente. */
class ContentResolver internal constructor() {
    fun openInputStream(uri: Uri): InputStream? = platformOpenInputStream(uri)

    fun openOutputStream(uri: Uri): OutputStream? = platformOpenOutputStream(uri, "w")

    fun openOutputStream(uri: Uri, mode: String): OutputStream? = platformOpenOutputStream(uri, mode)

    fun takePersistableUriPermission(uri: Uri, modeFlags: Int) = platformTakePersistablePermission(uri, modeFlags, take = true)

    fun releasePersistableUriPermission(uri: Uri, modeFlags: Int) = platformTakePersistablePermission(uri, modeFlags, take = false)

    fun getType(uri: Uri): String? = platformGetType(uri)

    /** Permessi persistenti sugli URI scelti dall'utente (su iPhone non esistono: lista vuota). */
    val persistedUriPermissions: List<UriPermission>
        get() = platformPersistedUriPermissions()
}

private val sharedContentResolver = ContentResolver()

val Context.contentResolver: ContentResolver
    get() = sharedContentResolver

internal expect fun platformOpenInputStream(uri: Uri): InputStream?

internal expect fun platformOpenOutputStream(uri: Uri, mode: String): OutputStream?

internal expect fun platformTakePersistablePermission(uri: Uri, modeFlags: Int, take: Boolean)

internal expect fun platformPersistedUriPermissions(): List<UriPermission>

/** android.content.UriPermission. */
class UriPermission(
    val uri: Uri,
    val isReadPermission: Boolean,
    val isWritePermission: Boolean,
)

internal expect fun platformGetType(uri: Uri): String?

/** Percorso locale di un uri file:// (anche senza schema). */
internal fun Uri.localPath(): String? =
    when (scheme) {
        null -> toString()
        "file" -> path
        else -> null
    }

@Suppress("unused")
private fun ioError(message: String): Nothing = throw IOException(message)

/** java.io.Reader (lettura carattere per carattere). */
abstract class Reader : AutoCloseable {
    abstract fun read(): Int

    override fun close() = Unit
}

class StringReader(
    private val text: String,
) : Reader() {
    private var position = 0

    override fun read(): Int = if (position < text.length) text[position++].code else -1
}

class InputStreamReader(
    stream: InputStream,
    charset: Charset = Charset.UTF_8,
) : Reader() {
    private val delegate = StringReader(charset.decode(stream.readBytes()))

    override fun read(): Int = delegate.read()
}

class PushbackReader(
    private val reader: Reader,
    @Suppress("UNUSED_PARAMETER") size: Int = 1,
) : Reader() {
    private val pushed = ArrayDeque<Int>()

    override fun read(): Int = pushed.removeLastOrNull() ?: reader.read()

    fun unread(c: Int) {
        pushed.addLast(c)
    }

    override fun close() = reader.close()
}

/** java.nio.ByteOrder. */
enum class ByteOrder {
    BIG_ENDIAN,
    LITTLE_ENDIAN,
    ;

    companion object {
        fun nativeOrder(): ByteOrder = LITTLE_ENDIAN
    }
}

/** java.nio.ByteBuffer (put/get di interi e array). */
class ByteBuffer private constructor(
    private val data: ByteArray,
) {
    private var order = ByteOrder.BIG_ENDIAN
    private var pos = 0

    fun order(order: ByteOrder): ByteBuffer = apply { this.order = order }

    fun order(): ByteOrder = order

    fun array(): ByteArray = data

    fun position(): Int = pos

    fun position(newPosition: Int): ByteBuffer = apply { pos = newPosition }

    fun remaining(): Int = data.size - pos

    fun hasRemaining(): Boolean = pos < data.size

    fun capacity(): Int = data.size

    fun put(b: Byte): ByteBuffer = apply { data[pos++] = b }

    fun put(bytes: ByteArray): ByteBuffer = apply {
        bytes.copyInto(data, pos)
        pos += bytes.size
    }

    fun get(): Byte = data[pos++]

    fun get(index: Int): Byte = data[index]

    fun get(dst: ByteArray): ByteBuffer = apply {
        data.copyInto(dst, 0, pos, pos + dst.size)
        pos += dst.size
    }

    private fun writeInt(index: Int, v: Int) {
        if (order == ByteOrder.LITTLE_ENDIAN) {
            for (i in 0 until 4) data[index + i] = (v ushr (8 * i)).toByte()
        } else {
            for (i in 0 until 4) data[index + i] = (v ushr (8 * (3 - i))).toByte()
        }
    }

    private fun readInt(index: Int): Int {
        var v = 0
        for (i in 0 until 4) {
            val b = data[index + i].toInt() and 0xFF
            v = if (order == ByteOrder.LITTLE_ENDIAN) v or (b shl (8 * i)) else (v shl 8) or b
        }
        return v
    }

    fun putInt(v: Int): ByteBuffer = apply {
        writeInt(pos, v)
        pos += 4
    }

    fun putInt(index: Int, v: Int): ByteBuffer = apply { writeInt(index, v) }

    fun getInt(): Int = readInt(pos).also { pos += 4 }

    fun getInt(index: Int): Int = readInt(index)

    fun putShort(v: Short): ByteBuffer = apply {
        val x = v.toInt()
        if (order == ByteOrder.LITTLE_ENDIAN) {
            data[pos] = x.toByte()
            data[pos + 1] = (x ushr 8).toByte()
        } else {
            data[pos] = (x ushr 8).toByte()
            data[pos + 1] = x.toByte()
        }
        pos += 2
    }

    fun getShort(): Short {
        val a = data[pos].toInt() and 0xFF
        val b = data[pos + 1].toInt() and 0xFF
        pos += 2
        return (if (order == ByteOrder.LITTLE_ENDIAN) (b shl 8) or a else (a shl 8) or b).toShort()
    }

    fun putLong(v: Long): ByteBuffer = apply {
        for (i in 0 until 8) {
            val shift = if (order == ByteOrder.LITTLE_ENDIAN) 8 * i else 8 * (7 - i)
            data[pos + i] = (v ushr shift).toByte()
        }
        pos += 8
    }

    fun getFloat(): Float = Float.fromBits(getInt())

    fun putFloat(v: Float): ByteBuffer = putInt(v.toRawBits())

    companion object {
        fun allocate(capacity: Int): ByteBuffer = ByteBuffer(ByteArray(capacity))

        fun wrap(array: ByteArray): ByteBuffer = ByteBuffer(array)
    }
}

/** File.inputStream() / File.outputStream() di Kotlin per la File comune. */
fun File.inputStream(): InputStream = FileInputStream(this)

fun File.outputStream(): OutputStream = FileOutputStream(this)

fun File.bufferedWriter(charset: Charset = Charset.UTF_8): Writer = FileOutputStream(this).bufferedWriter(charset)

fun File.bufferedReader(charset: Charset = Charset.UTF_8): BufferedReader = BufferedReader(charset.decode(readBytes()))
