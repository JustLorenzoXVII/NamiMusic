package moe.rukamori.archivetune.compat

/**
 * Sostituto minimo di timber.log.Timber: stessi metodi (tag, d, i, w, e, v),
 * scrive sulla console della piattaforma. Supporta i segnaposto %s / %d come String.format.
 */
object Timber {
    const val VERBOSE = 2
    const val DEBUG = 3
    const val INFO = 4
    const val WARN = 5
    const val ERROR = 6

    /** Un albero di log (come in Timber): riceve i messaggi con [log]. */
    abstract class Tree {
        internal var explicitTag: String? = null

        open fun log(
            priority: Int,
            tag: String?,
            message: String,
            t: Throwable?,
        ) {}

        fun v(message: String?, vararg args: Any?) = prepare(VERBOSE, null, message, args)

        fun v(t: Throwable?, message: String?, vararg args: Any?) = prepare(VERBOSE, t, message, args)

        fun d(message: String?, vararg args: Any?) = prepare(DEBUG, null, message, args)

        fun d(t: Throwable?, message: String?, vararg args: Any?) = prepare(DEBUG, t, message, args)

        fun d(t: Throwable?) = prepare(DEBUG, t, null, emptyArray())

        fun i(message: String?, vararg args: Any?) = prepare(INFO, null, message, args)

        fun i(t: Throwable?, message: String?, vararg args: Any?) = prepare(INFO, t, message, args)

        fun w(message: String?, vararg args: Any?) = prepare(WARN, null, message, args)

        fun w(t: Throwable?, message: String?, vararg args: Any?) = prepare(WARN, t, message, args)

        fun w(t: Throwable?) = prepare(WARN, t, null, emptyArray())

        fun e(message: String?, vararg args: Any?) = prepare(ERROR, null, message, args)

        fun e(t: Throwable?, message: String?, vararg args: Any?) = prepare(ERROR, t, message, args)

        fun e(t: Throwable?) = prepare(ERROR, t, null, emptyArray())

        private fun prepare(
            priority: Int,
            t: Throwable?,
            message: String?,
            args: Array<out Any?>,
        ) {
            val tag = explicitTag
            explicitTag = null
            val text = message?.let { formatMessage(it, args) }.orEmpty() + t?.let { " | ${it::class.simpleName}: ${it.message}" }.orEmpty()
            dispatch(priority, tag, text, t)
        }
    }

    /** Scrive sulla console della piattaforma. */
    open class DebugTree : Tree() {
        override fun log(
            priority: Int,
            tag: String?,
            message: String,
            t: Throwable?,
        ) {
            val level =
                when (priority) {
                    VERBOSE -> "V"
                    DEBUG -> "D"
                    INFO -> "I"
                    WARN -> "W"
                    else -> "E"
                }
            platformLog(level, tag ?: "ArchiveTuneX", message)
        }
    }

    private val forest = mutableListOf<Tree>()
    private val console = DebugTree()

    fun plant(tree: Tree) {
        forest += tree
    }

    fun uprootAll() = forest.clear()

    internal fun dispatch(
        priority: Int,
        tag: String?,
        message: String,
        t: Throwable?,
    ) {
        if (forest.none { it is DebugTree }) console.log(priority, tag, message, t)
        forest.toList().forEach { it.log(priority, tag, message, t) }
    }

    private val root = object : Tree() {}

    fun tag(tag: String): Tree = object : Tree() {}.also { it.explicitTag = tag }

    fun v(message: String?, vararg args: Any?) = root.v(message, *args)

    fun d(message: String?, vararg args: Any?) = root.d(message, *args)

    fun i(message: String?, vararg args: Any?) = root.i(message, *args)

    fun w(message: String?, vararg args: Any?) = root.w(message, *args)

    fun w(t: Throwable?, message: String?, vararg args: Any?) = root.w(t, message, *args)

    fun w(t: Throwable?) = root.w(t)

    fun e(message: String?, vararg args: Any?) = root.e(message, *args)

    fun e(t: Throwable?, message: String?, vararg args: Any?) = root.e(t, message, *args)

    fun e(t: Throwable?) = root.e(t)

    fun d(t: Throwable?, message: String?, vararg args: Any?) = root.d(t, message, *args)

    fun i(t: Throwable?, message: String?, vararg args: Any?) = root.i(t, message, *args)
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

/** Alberi di log dell'avvio dell'app (Timber.plant in App.onCreate dell'originale). */
internal expect fun plantPlatformLogTrees()

fun plantArchiveTuneLogTrees() {
    Timber.uprootAll()
    plantPlatformLogTrees()
}

internal expect fun platformLog(
    level: String,
    tag: String,
    message: String,
)
