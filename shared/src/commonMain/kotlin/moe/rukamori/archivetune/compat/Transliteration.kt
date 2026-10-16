package moe.rukamori.archivetune.compat

/** Una parola giapponese con la lettura in katakana (come i token di Kuromoji). */
data class JapaneseToken(
    val surface: String,
    val reading: String?,
)

/** Traslitterazione ICU (es. "Any-Latin; Latin-ASCII"): android.icu su Android, NSString su iPhone. */
expect fun platformTransliterate(
    transform: String,
    text: String,
): String

/** Divide il testo giapponese in parole con la lettura: Kuromoji su Android, CFStringTokenizer su iPhone. */
expect fun platformTokenizeJapanese(text: String): List<JapaneseToken>
