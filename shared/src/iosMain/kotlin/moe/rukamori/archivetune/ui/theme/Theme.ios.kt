package moe.rukamori.archivetune.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily

/** iPhone non ha i colori di sistema di Material You. */
@Composable
actual fun systemDynamicColorScheme(darkTheme: Boolean): ColorScheme? = null

actual object CustomFontLoader {
    actual val supportedMimeTypes: Array<String> = arrayOf("font/ttf", "application/x-font-ttf", "application/octet-stream")

    actual fun displayName(
        context: moe.rukamori.archivetune.compat.Context,
        uri: moe.rukamori.archivetune.compat.Uri,
    ): String = uri.toString().substringAfterLast('/').substringBefore('?')

    actual fun isSupportedTtf(
        context: moe.rukamori.archivetune.compat.Context,
        uri: moe.rukamori.archivetune.compat.Uri,
    ): Boolean = displayName(context, uri).endsWith(".ttf", ignoreCase = true)

    actual suspend fun loadFontFamily(uriString: String): FontFamily? = null
}
