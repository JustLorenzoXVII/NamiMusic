package moe.rukamori.archivetune.compat

import kotlinx.coroutines.runBlocking
import moe.rukamori.archivetune.lyrics.translation.StandardLyricsTranslationRepository

/**
 * Sostituto della libreria me.bush.translator (solo JVM): stessa API usata dall'app,
 * le traduzioni passano dallo stesso servizio di StandardLyricsTranslationRepository.
 */
class Language(
    language: String,
    strict: Boolean = false,
) {
    val code: String
    val name: String

    init {
        val key = language.trim()
        val match =
            ALL.entries.firstOrNull { it.key.equals(key, ignoreCase = true) }
                ?: ALL.entries.firstOrNull { it.value.equals(key, ignoreCase = true) }
                ?: ALL.entries.firstOrNull { it.key.replace("_", " ").equals(key, ignoreCase = true) }
        if (match == null && strict) throw IllegalArgumentException("Unsupported language: $language")
        name = match?.key ?: key.uppercase()
        code = match?.value ?: key.lowercase()
    }

    override fun toString(): String = name

    override fun equals(other: Any?): Boolean = other is Language && other.code == code

    override fun hashCode(): Int = code.hashCode()

    companion object {
        fun valueOf(name: String): Language = Language(name, strict = true)

        val AUTO = Language("AUTO")
        val ENGLISH = Language("ENGLISH")

        val ALL: Map<String, String> =
            linkedMapOf(
                "AUTO" to "auto", "AFRIKAANS" to "af", "ALBANIAN" to "sq", "AMHARIC" to "am", "ARABIC" to "ar",
                "ARMENIAN" to "hy", "AZERBAIJANI" to "az", "BASQUE" to "eu", "BELARUSIAN" to "be", "BENGALI" to "bn",
                "BOSNIAN" to "bs", "BULGARIAN" to "bg", "CATALAN" to "ca", "CEBUANO" to "ceb", "CHICHEWA" to "ny",
                "CHINESE_SIMPLIFIED" to "zh-CN", "CHINESE_TRADITIONAL" to "zh-TW", "CORSICAN" to "co", "CROATIAN" to "hr",
                "CZECH" to "cs", "DANISH" to "da", "DUTCH" to "nl", "ENGLISH" to "en", "ESPERANTO" to "eo",
                "ESTONIAN" to "et", "FILIPINO" to "tl", "FINNISH" to "fi", "FRENCH" to "fr", "FRISIAN" to "fy",
                "GALICIAN" to "gl", "GEORGIAN" to "ka", "GERMAN" to "de", "GREEK" to "el", "GUJARATI" to "gu",
                "HAITIAN_CREOLE" to "ht", "HAUSA" to "ha", "HAWAIIAN" to "haw", "HEBREW" to "iw", "HINDI" to "hi",
                "HMONG" to "hmn", "HUNGARIAN" to "hu", "ICELANDIC" to "is", "IGBO" to "ig", "INDONESIAN" to "id",
                "IRISH" to "ga", "ITALIAN" to "it", "JAPANESE" to "ja", "JAVANESE" to "jw", "KANNADA" to "kn",
                "KAZAKH" to "kk", "KHMER" to "km", "KINYARWANDA" to "rw", "KOREAN" to "ko", "KURDISH" to "ku",
                "KYRGYZ" to "ky", "LAO" to "lo", "LATIN" to "la", "LATVIAN" to "lv", "LITHUANIAN" to "lt",
                "LUXEMBOURGISH" to "lb", "MACEDONIAN" to "mk", "MALAGASY" to "mg", "MALAY" to "ms", "MALAYALAM" to "ml",
                "MALTESE" to "mt", "MAORI" to "mi", "MARATHI" to "mr", "MONGOLIAN" to "mn", "MYANMAR" to "my",
                "NEPALI" to "ne", "NORWEGIAN" to "no", "ODIA" to "or", "PASHTO" to "ps", "PERSIAN" to "fa",
                "POLISH" to "pl", "PORTUGUESE" to "pt", "PUNJABI" to "pa", "ROMANIAN" to "ro", "RUSSIAN" to "ru",
                "SAMOAN" to "sm", "SCOTS_GAELIC" to "gd", "SERBIAN" to "sr", "SESOTHO" to "st", "SHONA" to "sn",
                "SINDHI" to "sd", "SINHALA" to "si", "SLOVAK" to "sk", "SLOVENIAN" to "sl", "SOMALI" to "so",
                "SPANISH" to "es", "SUNDANESE" to "su", "SWAHILI" to "sw", "SWEDISH" to "sv", "TAJIK" to "tg",
                "TAMIL" to "ta", "TATAR" to "tt", "TELUGU" to "te", "THAI" to "th", "TURKISH" to "tr",
                "TURKMEN" to "tk", "UKRAINIAN" to "uk", "URDU" to "ur", "UYGHUR" to "ug", "UZBEK" to "uz",
                "VIETNAMESE" to "vi", "WELSH" to "cy", "XHOSA" to "xh", "YIDDISH" to "yi", "YORUBA" to "yo",
                "ZULU" to "zu",
            )
    }
}

class Translation(
    val translatedText: String,
    val sourceText: String,
    val targetLanguage: Language,
)

class Translator {
    private val repository by lazy { StandardLyricsTranslationRepository() }

    suspend fun translate(
        text: String,
        target: Language,
        source: Language = Language.AUTO,
    ): Translation = Translation(repository.translate(text, target.code), text, target)

    fun translateBlocking(
        text: String,
        target: Language,
        source: Language = Language.AUTO,
    ): Translation = runBlocking { translate(text, target, source) }
}
