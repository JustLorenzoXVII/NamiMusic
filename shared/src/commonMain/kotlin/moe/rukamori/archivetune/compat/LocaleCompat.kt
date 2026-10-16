package moe.rukamori.archivetune.compat

/**
 * Sottoinsieme di java.util.Locale e java.text.Collator usato da ArchiveTune.
 * Android: avvolge le classi Java vere; iPhone: NSLocale.
 */
class Locale(
    val language: String,
    val country: String = "",
) {
    fun getLanguage(): String = language

    fun getCountry(): String = country

    fun toLanguageTag(): String = if (country.isBlank()) language else "$language-$country"

    val displayLanguage: String get() = displayLanguageOf(this)

    fun getDisplayLanguage(): String = displayLanguage

    fun getDisplayLanguage(inLocale: Locale): String = displayLanguageOf(this, inLocale)

    val displayName: String get() = displayNameOf(this)

    fun getDisplayName(): String = displayName

    fun getDisplayName(inLocale: Locale): String = displayNameOf(this, inLocale)

    val displayCountry: String get() = displayCountryOf(this)

    override fun equals(other: Any?): Boolean = other is Locale && other.language == language && other.country == country

    override fun hashCode(): Int = 31 * language.hashCode() + country.hashCode()

    override fun toString(): String = if (country.isBlank()) language else "${language}_$country"

    companion object {
        val ROOT = Locale("", "")
        val US = Locale("en", "US")
        val ENGLISH = Locale("en", "")
        val UK = Locale("en", "GB")

        fun getDefault(): Locale = platformDefaultLocale()

        fun forLanguageTag(languageTag: String): Locale {
            val parts = languageTag.replace('_', '-').split('-')
            val language = parts.getOrNull(0).orEmpty()
            val country = parts.drop(1).firstOrNull { it.length == 2 && it.all(Char::isLetter) }.orEmpty()
            return Locale(language.lowercase(), country.uppercase())
        }

        fun getAvailableLocales(): Array<Locale> = platformAvailableLocales()
    }
}

/** Come String.uppercase(Locale) / lowercase(Locale) di Java: qui la conversione è indipendente dalla lingua. */
@Suppress("UNUSED_PARAMETER")
fun String.uppercase(locale: Locale): String = uppercase()

@Suppress("UNUSED_PARAMETER")
fun String.lowercase(locale: Locale): String = lowercase()

@Suppress("UNUSED_PARAMETER")
fun Char.uppercase(locale: Locale): String = uppercase()

@Suppress("UNUSED_PARAMETER")
fun Char.lowercase(locale: Locale): String = lowercase()

@Suppress("UNUSED_PARAMETER")
fun Char.titlecase(locale: Locale): String = titlecase()

/** Come java.lang.String.format(Locale, ...). */
@Suppress("UNUSED_PARAMETER")
fun String.Companion.format(
    locale: Locale?,
    format: String,
    vararg args: Any?,
): String = formatMessage(format, args)

/** Come java.lang.String.format(...). */
fun String.Companion.format(
    format: String,
    vararg args: Any?,
): String = formatMessage(format, args)

fun String.format(vararg args: Any?): String = formatMessage(this, args)

class Collator private constructor(
    private val locale: Locale,
) : Comparator<String> {
    var strength: Int = TERTIARY

    fun setStrength(value: Int) {
        strength = value
    }

    override fun compare(
        a: String,
        b: String,
    ): Int = platformCompare(a, b, strength <= SECONDARY)

    companion object {
        const val PRIMARY = 0
        const val SECONDARY = 1
        const val TERTIARY = 2
        const val IDENTICAL = 3

        fun getInstance(): Collator = Collator(Locale.getDefault())

        fun getInstance(locale: Locale): Collator = Collator(locale)
    }
}

internal expect fun platformDefaultLocale(): Locale

internal expect fun platformAvailableLocales(): Array<Locale>

internal expect fun displayLanguageOf(
    locale: Locale,
    inLocale: Locale? = null,
): String

internal expect fun displayNameOf(
    locale: Locale,
    inLocale: Locale? = null,
): String

internal expect fun displayCountryOf(locale: Locale): String

/** Confronto alfabetico secondo la lingua del telefono; [ignoreCaseAndAccents] come Collator.PRIMARY. */
internal expect fun platformCompare(
    a: String,
    b: String,
    ignoreCaseAndAccents: Boolean,
): Int
