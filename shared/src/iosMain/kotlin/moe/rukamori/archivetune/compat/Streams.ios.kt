package moe.rukamori.archivetune.compat


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

/** DEFLATE "raw" (RFC 1951) dei file zip, decodificato in Kotlin puro. */
internal actual fun platformInflateRaw(data: ByteArray, expectedSize: Int): ByteArray = RawInflater(data, expectedSize).inflate()

internal actual fun platformPersistedUriPermissions(): List<UriPermission> = emptyList()
