package moe.rukamori.archivetune.compat

import com.justlorenzo.archivetunex.player.AndroidAppContext

private fun Uri.toAndroid(): android.net.Uri = android.net.Uri.parse(toString())

private class JavaInputStream(
    private val stream: java.io.InputStream,
) : InputStream() {
    override fun read(): Int = stream.read()

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int = stream.read(buffer, offset, length)

    override fun available(): Int = stream.available()

    override fun close() = stream.close()
}

private class JavaOutputStream(
    private val stream: java.io.OutputStream,
) : OutputStream() {
    override fun write(b: Int) = stream.write(b)

    override fun write(bytes: ByteArray, offset: Int, length: Int) = stream.write(bytes, offset, length)

    override fun flush() = stream.flush()

    override fun close() = stream.close()
}

internal actual fun platformOpenInputStream(uri: Uri): InputStream? =
    AndroidAppContext.context.contentResolver.openInputStream(uri.toAndroid())?.let(::JavaInputStream)

internal actual fun platformOpenOutputStream(uri: Uri, mode: String): OutputStream? =
    AndroidAppContext.context.contentResolver.openOutputStream(uri.toAndroid(), mode)?.let(::JavaOutputStream)

internal actual fun platformTakePersistablePermission(uri: Uri, modeFlags: Int, take: Boolean) {
    val resolver = AndroidAppContext.context.contentResolver
    runCatching {
        if (take) resolver.takePersistableUriPermission(uri.toAndroid(), modeFlags) else resolver.releasePersistableUriPermission(uri.toAndroid(), modeFlags)
    }
}

internal actual fun platformGetType(uri: Uri): String? = AndroidAppContext.context.contentResolver.getType(uri.toAndroid())

internal actual fun platformInflateRaw(data: ByteArray, expectedSize: Int): ByteArray {
    val inflater = java.util.zip.Inflater(true)
    inflater.setInput(data)
    val out = java.io.ByteArrayOutputStream(expectedSize.coerceAtLeast(64))
    val buffer = ByteArray(64 * 1024)
    while (!inflater.finished()) {
        val n = inflater.inflate(buffer)
        if (n == 0 && (inflater.needsInput() || inflater.needsDictionary())) break
        out.write(buffer, 0, n)
    }
    inflater.end()
    return out.toByteArray()
}
