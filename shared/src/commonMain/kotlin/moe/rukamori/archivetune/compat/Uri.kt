package moe.rukamori.archivetune.compat

/**
 * Sostituto multipiattaforma di android.net.Uri: tiene la stringa e ne ricava
 * schema, host, percorso e parametri come fa Android.
 */
class Uri private constructor(private val value: String) {
    val scheme: String?
        get() = value.substringBefore(':', "").takeIf { it.isNotEmpty() && !it.contains('/') }

    private val afterScheme: String
        get() = scheme?.let { value.substring(it.length + 1) } ?: value

    private val withoutFragment: String
        get() = afterScheme.substringBefore('#')

    val authority: String?
        get() = if (withoutFragment.startsWith("//")) withoutFragment.removePrefix("//").substringBefore('/').substringBefore('?') else null

    val host: String?
        get() = authority?.substringAfter('@')?.substringBefore(':')?.takeIf { it.isNotEmpty() }

    val port: Int
        get() = authority?.substringAfter('@')?.substringAfter(':', "")?.toIntOrNull() ?: -1

    val path: String?
        get() {
            val rest = withoutFragment.substringBefore('?')
            val p = if (rest.startsWith("//")) rest.removePrefix("//").let { a -> a.indexOf('/').let { if (it < 0) "" else a.substring(it) } } else rest
            return decode(p).takeIf { it.isNotEmpty() || authority != null }
        }

    val encodedPath: String?
        get() = path

    val query: String?
        get() = withoutFragment.substringAfter('?', "").takeIf { withoutFragment.contains('?') }?.let(::decode)

    val encodedQuery: String?
        get() = withoutFragment.substringAfter('?', "").takeIf { withoutFragment.contains('?') }

    val fragment: String?
        get() = afterScheme.substringAfter('#', "").takeIf { afterScheme.contains('#') }

    val pathSegments: List<String>
        get() = path.orEmpty().split('/').filter { it.isNotEmpty() }

    val lastPathSegment: String?
        get() = pathSegments.lastOrNull()

    val isAbsolute: Boolean
        get() = scheme != null

    fun getQueryParameter(key: String): String? = getQueryParameters(key).firstOrNull()

    fun getQueryParameters(key: String): List<String> =
        encodedQuery
            .orEmpty()
            .split('&')
            .filter { it.isNotEmpty() }
            .mapNotNull { part ->
                val name = decode(part.substringBefore('='))
                if (name == key) decode(part.substringAfter('=', "")) else null
            }

    val queryParameterNames: Set<String>
        get() = encodedQuery.orEmpty().split('&').filter { it.isNotEmpty() }.map { decode(it.substringBefore('=')) }.toSet()

    fun buildUpon(): Builder = Builder(value)

    override fun toString(): String = value

    override fun equals(other: Any?): Boolean = other is Uri && other.value == value

    override fun hashCode(): Int = value.hashCode()

    class Builder internal constructor(private var base: String = "") {
        constructor() : this("")

        private val params = mutableListOf<Pair<String, String>>()

        fun scheme(scheme: String) = apply { base = "$scheme:" + base.substringAfter(':', base) }

        fun authority(authority: String) = apply { base = base.substringBefore("//", base) + "//" + authority }

        fun path(path: String) = apply { base += path }

        fun appendPath(segment: String) = apply { base = base.trimEnd('/') + "/" + encode(segment) }

        fun appendQueryParameter(key: String, value: String?) = apply { params += key to value.orEmpty() }

        fun clearQuery() = apply { base = base.substringBefore('?') }

        fun build(): Uri {
            if (params.isEmpty()) return Uri(base)
            val separator = if (base.contains('?')) "&" else "?"
            return Uri(base + separator + params.joinToString("&") { "${encode(it.first)}=${encode(it.second)}" })
        }
    }

    companion object {
        val EMPTY = Uri("")

        fun parse(uriString: String): Uri = Uri(uriString)

        fun encode(s: String?): String {
            if (s == null) return ""
            val sb = StringBuilder()
            for (b in s.encodeToByteArray()) {
                val c = b.toInt() and 0xFF
                val ch = c.toChar()
                if (ch.isLetterOrDigit() && c < 128 || ch in "-_.~!*'()") sb.append(ch) else sb.append('%').append(c.toString(16).uppercase().padStart(2, '0'))
            }
            return sb.toString()
        }

        fun decode(s: String?): String {
            if (s == null) return ""
            if ('%' !in s) return s
            val bytes = ArrayList<Byte>(s.length)
            var i = 0
            while (i < s.length) {
                val c = s[i]
                if (c == '%' && i + 2 < s.length + 0 && i + 2 <= s.lastIndex) {
                    val v = s.substring(i + 1, i + 3).toIntOrNull(16)
                    if (v != null) {
                        bytes += v.toByte()
                        i += 3
                        continue
                    }
                }
                c.toString().encodeToByteArray().forEach { bytes += it }
                i++
            }
            return bytes.toByteArray().decodeToString()
        }

        fun fromParts(scheme: String, ssp: String, fragment: String?): Uri =
            Uri("$scheme:$ssp" + (fragment?.let { "#$it" } ?: ""))
    }
}

fun String.toUri(): Uri = Uri.parse(this)
