/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.rukamori.archivetune.ui.theme

import androidx.compose.ui.text.font.FontFamily

/**
 * Carica un font .ttf scelto dall'utente.
 * Android: implementazione originale (file copiato nella cartella privata dell'app).
 * iPhone: non ancora supportato, si usa il font predefinito.
 */
expect object CustomFontLoader {
    val supportedMimeTypes: Array<String>

    fun displayName(
        context: moe.rukamori.archivetune.compat.Context,
        uri: moe.rukamori.archivetune.compat.Uri,
    ): String

    fun isSupportedTtf(
        context: moe.rukamori.archivetune.compat.Context,
        uri: moe.rukamori.archivetune.compat.Uri,
    ): Boolean

    suspend fun loadFontFamily(uriString: String): FontFamily?
}
