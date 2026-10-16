package moe.rukamori.archivetune.compat

import kotlinx.cinterop.addressOf
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned

/** Su iPhone i documenti scelti arrivano come file:// (copiati nella sandbox dell'app). */
internal actual fun platformOpenInputStream(uri: Uri): InputStream? {
    val path = uri.localPath() ?: return null
    val file = File(path)
    return if (file.exists()) FileInputStream(file) else null
}

internal actual fun platformOpenOutputStream(uri: Uri, mode: String): OutputStream? {
    val path = uri.localPath() ?: return null
    return FileOutputStream(File(path), append = mode.contains('a'))
}

internal actual fun platformTakePersistablePermission(uri: Uri, modeFlags: Int, take: Boolean) = Unit

internal actual fun platformGetType(uri: Uri): String? =
    when (uri.toString().substringAfterLast('.', "").lowercase()) {
        "json" -> "application/json"
        "zip", "backup" -> "application/zip"
        "csv" -> "text/csv"
        "txt", "lrc" -> "text/plain"
        "m4a" -> "audio/mp4"
        "mp3" -> "audio/mpeg"
        else -> null
    }

@OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
internal actual fun platformInflateRaw(data: ByteArray, expectedSize: Int): ByteArray {
    if (data.isEmpty()) return ByteArray(0)
    // Compression di Apple: COMPRESSION_ZLIB è proprio DEFLATE raw (RFC 1951).
    var capacity = maxOf(expectedSize, data.size * 4, 1024)
    while (true) {
        val out = ByteArray(capacity)
        val written =
            data.usePinned { src ->
                out.usePinned { dst ->
                    platform.compression.compression_decode_buffer(
                        dst.addressOf(0).reinterpret(),
                        capacity.toULong(),
                        src.addressOf(0).reinterpret(),
                        data.size.toULong(),
                        null,
                        platform.compression.COMPRESSION_ZLIB,
                    )
                }
            }.toInt()
        if (written <= 0) throw kotlinx.io.IOException("Decompressione zip non riuscita")
        if (written < capacity || expectedSize in 1..capacity) return out.copyOf(written)
        capacity *= 2
    }
}
