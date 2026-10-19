package com.justlorenzo.archivetunex.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.intl.Locale

/** Lingua dell'interfaccia di Nami. */
enum class AppLanguage(
    val label: String,
) {
    System("Sistema"),
    Italian("Italiano"),
    English("English"),
}

/**
 * Traduzioni dell'app. I testi sono scritti in italiano nel codice: [tr] li cerca nel dizionario
 * della lingua scelta e, se manca, lascia l'italiano. Cambiando lingua l'app si ridisegna da sola
 * (la lingua è uno "stato" di Compose letto da ogni testo).
 */
object I18n {
    var language by mutableStateOf(AppLanguage.System)

    /** La lingua effettiva: con "Sistema" segue quella del telefono (inglese se non è l'italiano). */
    val resolved: AppLanguage
        get() =
            when (language) {
                AppLanguage.System -> if (Locale.current.language == "it") AppLanguage.Italian else AppLanguage.English
                else -> language
            }
}

/** Traduce un testo dell'interfaccia (scritto in italiano). */
fun tr(text: String): String =
    when (I18n.resolved) {
        AppLanguage.English -> EnglishStrings[text] ?: text
        else -> text
    }

/** Traduce un testo con dei valori dentro: "{0} brani" → "12 brani" / "12 songs". */
fun trf(
    template: String,
    vararg args: Any?,
): String {
    var out = tr(template)
    args.forEachIndexed { i, value -> out = out.replace("{$i}", value.toString()) }
    return out
}
