package moe.rukamori.archivetune.compat

actual open class SecurityException : RuntimeException {
    actual constructor() : super()

    actual constructor(message: String?) : super(message)
}
