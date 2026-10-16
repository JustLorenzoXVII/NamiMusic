package moe.rukamori.archivetune.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily

/** iPhone non ha i colori di sistema di Material You. */
@Composable
actual fun systemDynamicColorScheme(darkTheme: Boolean): ColorScheme? = null

actual object CustomFontLoader {
    actual suspend fun loadFontFamily(uriString: String): FontFamily? = null
}
