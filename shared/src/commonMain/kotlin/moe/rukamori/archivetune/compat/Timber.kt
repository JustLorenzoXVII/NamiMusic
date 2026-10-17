package moe.rukamori.archivetune.compat

/**
 * Sostituto minimo di timber.log.Timber: stessi metodi (tag, d, i, w, e, v),
 * scrive sulla console della piattaforma. Supporta i segnaposto %s / %d come String.format.
 */
object Timber {
    class Tree internal constructor(
        private val tag: String?,
    ) {
        fun v(message: String?, vararg args: Any?) = log("V", null, message, args)

        fun v(t: Throwable?, message: String?, vararg args: Any?) = log("V", t, message, args)

        fun d(message: String?, vararg args: Any?) = log("D", null, message, args)

        fun d(t: Throwable?, message: String?, vararg args: Any?) = log("D", t, message, args)

        fun d(t: Throwable?) = log("D", t, null, emptyArray())

        fun i(message: String?, vararg args: Any?) = log("I", null, message, args)

        fun i(t: Throwable?, message: String?, vararg args: Any?) = log("I", t, message, args)

        fun w(message: String?, vararg args: Any?) = log("W", null, message, args)

        fun w(t: Throwable?, message: String?, vararg args: Any?) = log("W", t, message, args)

        fun w(t: Throwable?) = log("W", t, null, emptyArray())

        fun e(message: String?, vararg args: Any?) = log("E", null, message, args)

        fun e(t: Throwable?, message: String?, vararg args: Any?) = log("E", t, message, args)

        fun e(t: Throwable?) = log("E", t, null, emptyArray())

        private fun log(
            level: String,
            t: Throwable?,
            message: String?,
            args: Array<out Any?>,
        ) {
            val text = message?.let { formatMessage(it, args) }.orEmpty()
            val suffix = t?.let { " | ${it::class.simpleName}: ${it.message}" }.orEmpty()
            platformLog(level, tag ?: "ArchiveTuneX", text + suffix)
        }
    }

    private val defaultTree = Tree(null)

    fun tag(tag: String): Tree = Tree(tag)

    fun v(message: String?, vararg args: Any?) = defaultTree.v(message, *args)

    fun d(message: String?, vararg args: Any?) = defaultTree.d(message, *args)

    fun i(message: String?, vararg args: Any?) = defaultTree.i(message, *args)

    fun w(message: String?, vararg args: Any?) = defaultTree.w(message, *args)

    fun w(t: Throwable?, message: String?, vararg args: Any?) = defaultTree.w(t, message, *args)

    fun w(t: Throwable?) = defaultTree.w(t)

    fun e(message: String?, vararg args: Any?) = defaultTree.e(message, *args)

    fun e(t: Throwable?, message: String?, vararg args: Any?) = defaultTree.e(t, message, *args)

    fun e(t: Throwable?) = defaultTree.e(t)

    fun d(t: Throwable?, message: String?, vararg args: Any?) = defaultTree.d(t, message, *args)

    fun i(t: Throwable?, message: String?, vararg args: Any?) = defaultTree.i(t, message, *args)
}

/** Sostituisce %s, %d, %.Nf e simili con gli argomenti, in ordine. */
fun formatMessage(
    template: String,
    args: Array<out Any?>,
): String {
    if (args.isEmpty()) return template
    var index = 0
    return Regex("%(\\d+\\$)?[-#+ 0,(]*\\d*(\\.\\d+)?[sdfxXc]").replace(template) { match ->
        val arg = args.getOrNull(index++)
        val spec = match.value
        val precision = Regex("\\.(\\d+)").find(spec)?.groupValues?.get(1)?.toIntOrNull()
        when {
            spec.endsWith("f") && arg is Number && precision != null -> arg.toDouble().toFixed(precision)
            spec.endsWith("x") && arg is Number -> arg.toLong().toString(16)
            spec.endsWith("X") && arg is Number -> arg.toLong().toString(16).uppercase()
            else -> arg.toString()
        }
    }
}

/** Come String.format("%.Nf", value). */
fun Double.toFixed(decimals: Int): String {
    if (decimals <= 0) return kotlin.math.round(this).toLong().toString()
    var factor = 1.0
    repeat(decimals) { factor *= 10 }
    val rounded = kotlin.math.round(kotlin.math.abs(this) * factor).toLong()
    val integer = rounded / factor.toLong()
    val fraction = (rounded % factor.toLong()).toString().padStart(decimals, '0')
    val sign = if (this < 0 && rounded != 0L) "-" else ""
    return "$sign$integer.$fraction"
}

internal expect fun platformLog(
    level: String,
    tag: String,
    message: String,
)
