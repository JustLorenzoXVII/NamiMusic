package moe.rukamori.archivetune.compat

/**
 * Riconoscimento degli errori di rete senza le classi java.net (che su iPhone non esistono).
 * Su Android le eccezioni sono quelle Java; su iPhone Ktor/NSURLSession usa i messaggi di NSError.
 */
private fun Throwable.typeName(): String = this::class.simpleName.orEmpty()

private fun Throwable.messageContains(vararg parts: String): Boolean {
    val text = message.orEmpty()
    return parts.any { text.contains(it, ignoreCase = true) }
}

fun Throwable.isUnknownHostError(): Boolean =
    typeName() == "UnknownHostException" ||
        messageContains("hostname could not be found", "Unable to resolve host", "NSURLErrorDomain Code=-1003")

fun Throwable.isConnectionRefusedError(): Boolean =
    typeName() == "ConnectException" ||
        messageContains("Could not connect to the server", "Connection refused", "NSURLErrorDomain Code=-1004")

fun Throwable.isSocketTimeoutError(): Boolean =
    typeName() == "SocketTimeoutException" ||
        typeName().contains("Timeout") ||
        messageContains("timed out", "NSURLErrorDomain Code=-1001")

fun Throwable.isSslHandshakeError(): Boolean =
    typeName() == "SSLHandshakeException" ||
        messageContains("SSL error", "secure connection", "NSURLErrorDomain Code=-1200")

/** java.lang.SecurityException. */
open class SecurityException(
    message: String? = null,
    cause: Throwable? = null,
) : RuntimeException(message, cause)

/** Throwable.localizedMessage di Java. */
val Throwable.localizedMessage: String?
    get() = message
