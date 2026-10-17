package moe.rukamori.archivetune.compat

import kotlinx.io.IOException

/** java.util.zip.ZipEntry. */
class ZipEntry(
    val name: String,
) {
    var size: Long = -1
        internal set
    var compressedSize: Long = -1
        internal set
    var method: Int = STORED
        internal set
    val isDirectory: Boolean
        get() = name.endsWith("/")

    companion object {
        const val STORED = 0
        const val DEFLATED = 8
    }
}

/** CRC-32 (java.util.zip.CRC32). */
class CRC32 {
    private var crc = 0xFFFFFFFF.toInt()

    fun update(bytes: ByteArray, offset: Int = 0, length: Int = bytes.size - offset) {
        var c = crc
        for (i in offset until offset + length) {
            c = TABLE[(c xor bytes[i].toInt()) and 0xFF] xor (c ushr 8)
        }
        crc = c
    }

    fun update(b: Int) = update(byteArrayOf(b.toByte()))

    val value: Long
        get() = (crc.inv().toLong() and 0xFFFFFFFFL)

    fun reset() {
        crc = 0xFFFFFFFF.toInt()
    }

    private companion object {
        val TABLE =
            IntArray(256) { n ->
                var c = n
                repeat(8) { c = if (c and 1 != 0) (0xEDB88320.toInt() xor (c ushr 1)) else (c ushr 1) }
                c
            }
    }
}

/** Scrive un archivio zip con voci non compresse (STORED). */
class ZipOutputStream(
    private val out: OutputStream,
) : OutputStream() {
    private class Written(
        val name: ByteArray,
        val crc: Long,
        val size: Long,
        val offset: Long,
    )

    private var current: ZipEntry? = null
    private val buffer = ByteArrayOutputStream()
    private val written = mutableListOf<Written>()
    private var offset = 0L

    fun putNextEntry(entry: ZipEntry) {
        if (current != null) closeEntry()
        current = entry
        buffer.reset()
    }

    override fun write(b: Int) {
        buffer.write(b)
    }

    override fun write(bytes: ByteArray, offset: Int, length: Int) {
        buffer.write(bytes, offset, length)
    }

    fun closeEntry() {
        val entry = current ?: return
        val data = buffer.toByteArray()
        val crc = CRC32().apply { update(data) }.value
        val name = entry.name.encodeToByteArray()
        val header = ByteArrayOutputStream()
        header.le32(0x04034b50)
        header.le16(20)
        header.le16(0x0800) // nomi in UTF-8
        header.le16(0)
        header.le16(0)
        header.le16(0x21)
        header.le32(crc.toInt())
        header.le32(data.size)
        header.le32(data.size)
        header.le16(name.size)
        header.le16(0)
        header.write(name)
        val headerBytes = header.toByteArray()
        out.write(headerBytes)
        out.write(data)
        written += Written(name, crc, data.size.toLong(), offset)
        offset += headerBytes.size + data.size
        current = null
        buffer.reset()
    }

    fun finish() {
        if (current != null) closeEntry()
        val central = ByteArrayOutputStream()
        written.forEach { w ->
            central.le32(0x02014b50)
            central.le16(20)
            central.le16(20)
            central.le16(0x0800)
            central.le16(0)
            central.le16(0)
            central.le16(0x21)
            central.le32(w.crc.toInt())
            central.le32(w.size.toInt())
            central.le32(w.size.toInt())
            central.le16(w.name.size)
            central.le16(0)
            central.le16(0)
            central.le16(0)
            central.le16(0)
            central.le32(0)
            central.le32(w.offset.toInt())
            central.write(w.name)
        }
        val centralBytes = central.toByteArray()
        out.write(centralBytes)
        val end = ByteArrayOutputStream()
        end.le32(0x06054b50)
        end.le16(0)
        end.le16(0)
        end.le16(written.size)
        end.le16(written.size)
        end.le32(centralBytes.size)
        end.le32(offset.toInt())
        end.le16(0)
        out.write(end.toByteArray())
        out.flush()
    }

    override fun flush() = out.flush()

    override fun close() {
        finish()
        out.close()
    }
}

/** Legge un archivio zip (voci STORED o DEFLATED) usando la directory centrale. */
class ZipInputStream(
    input: InputStream,
) : InputStream() {
    private class Item(
        val entry: ZipEntry,
        val localOffset: Int,
    )

    private val data = input.readBytes()
    private val items: List<Item> = readCentralDirectory()
    private var index = -1
    private var current: ByteArrayInputStream? = null

    val nextEntry: ZipEntry?
        get() = getNextEntry()

    @kotlin.jvm.JvmName("getNextEntryCall")
    fun getNextEntry(): ZipEntry? {
        index++
        val item = items.getOrNull(index) ?: run {
            current = null
            return null
        }
        val o = item.localOffset
        if (le32(o) != 0x04034b50) throw IOException("Zip non valido")
        val nameLength = le16(o + 26)
        val extraLength = le16(o + 28)
        val start = o + 30 + nameLength + extraLength
        val raw = data.copyOfRange(start, (start + item.entry.compressedSize.toInt()).coerceAtMost(data.size))
        val bytes =
            when (item.entry.method) {
                ZipEntry.STORED -> raw
                ZipEntry.DEFLATED -> platformInflateRaw(raw, item.entry.size.toInt())
                else -> throw IOException("Compressione zip non supportata: ${item.entry.method}")
            }
        current = ByteArrayInputStream(bytes)
        return item.entry
    }

    fun closeEntry() {
        current = null
    }

    override fun read(): Int = current?.read() ?: -1

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int = current?.read(buffer, offset, length) ?: -1

    override fun available(): Int = current?.available() ?: 0

    private fun le16(o: Int): Int = (data[o].toInt() and 0xFF) or ((data[o + 1].toInt() and 0xFF) shl 8)

    private fun le32(o: Int): Int = le16(o) or (le16(o + 2) shl 16)

    private fun readCentralDirectory(): List<Item> {
        var end = data.size - 22
        while (end >= 0 && le32(end) != 0x06054b50) end--
        if (end < 0) throw IOException("Zip non valido: directory centrale mancante")
        val count = le16(end + 10)
        var p = le32(end + 16)
        val result = ArrayList<Item>(count)
        repeat(count) {
            if (le32(p) != 0x02014b50) throw IOException("Zip non valido")
            val method = le16(p + 10)
            val compressed = le32(p + 20).toLong() and 0xFFFFFFFFL
            val size = le32(p + 24).toLong() and 0xFFFFFFFFL
            val nameLength = le16(p + 28)
            val extraLength = le16(p + 30)
            val commentLength = le16(p + 32)
            val localOffset = le32(p + 42)
            val name = data.decodeToString(p + 46, p + 46 + nameLength)
            val entry =
                ZipEntry(name).also {
                    it.method = method
                    it.compressedSize = compressed
                    it.size = size
                }
            result += Item(entry, localOffset)
            p += 46 + nameLength + extraLength + commentLength
        }
        return result
    }
}

private fun ByteArrayOutputStream.le16(v: Int) {
    write(v and 0xFF)
    write((v ushr 8) and 0xFF)
}

private fun ByteArrayOutputStream.le32(v: Int) {
    le16(v and 0xFFFF)
    le16((v ushr 16) and 0xFFFF)
}

/** Decomprime dati DEFLATE "raw" (senza intestazione zlib). */
internal expect fun platformInflateRaw(data: ByteArray, expectedSize: Int): ByteArray

fun InputStream.copyTo(out: OutputStream, bufferSize: Int = 8192): Long {
    var total = 0L
    val buffer = ByteArray(bufferSize)
    while (true) {
        val n = read(buffer, 0, buffer.size)
        if (n < 0) break
        out.write(buffer, 0, n)
        total += n
    }
    return total
}
