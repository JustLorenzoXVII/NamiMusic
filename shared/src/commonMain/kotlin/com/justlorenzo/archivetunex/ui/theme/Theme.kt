package com.justlorenzo.archivetunex.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamicColorScheme
import moe.rukamori.archivetune.ui.theme.systemDynamicColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp

private val LightColors =
    lightColorScheme(
        primary = AccentRed,
        onPrimary = Color.White,
        primaryContainer = LightPrimaryContainer,
        onPrimaryContainer = LightOnPrimaryContainer,
        secondary = AccentRed,
        onSecondary = Color.White,
        secondaryContainer = LightPrimaryContainer,
        onSecondaryContainer = LightOnPrimaryContainer,
        tertiary = AccentRed,
        onTertiary = Color.White,
        background = LightBackground,
        onBackground = LightOnBackground,
        surface = LightBackground,
        onSurface = LightOnBackground,
        surfaceVariant = LightSurfaceVariant,
        onSurfaceVariant = LightOnSurfaceVariant,
        outline = LightOutline,
        outlineVariant = LightOutlineVariant,
        surfaceContainerLowest = LightBackground,
        surfaceContainerLow = LightContainerLow,
        surfaceContainer = LightContainer,
        surfaceContainerHigh = LightContainerHigh,
        surfaceContainerHighest = LightContainerHighest,
    )

private val DarkColors =
    darkColorScheme(
        primary = AccentRed,
        onPrimary = Color.White,
        primaryContainer = DarkPrimaryContainer,
        onPrimaryContainer = DarkOnPrimaryContainer,
        secondary = AccentRed,
        onSecondary = Color.White,
        secondaryContainer = DarkPrimaryContainer,
        onSecondaryContainer = DarkOnPrimaryContainer,
        tertiary = AccentRed,
        onTertiary = Color.White,
        background = DarkBackground,
        onBackground = DarkOnBackground,
        surface = DarkBackground,
        onSurface = DarkOnBackground,
        surfaceVariant = DarkSurfaceVariant,
        onSurfaceVariant = DarkOnSurfaceVariant,
        outline = DarkOutline,
        outlineVariant = DarkOutlineVariant,
        surfaceContainerLowest = DarkBackground,
        surfaceContainerLow = DarkContainerLow,
        surfaceContainer = DarkContainer,
        surfaceContainerHigh = DarkContainerHigh,
        surfaceContainerHighest = DarkContainerHighest,
    )

// Forme più arrotondate, in stile Expressive (come Fuji Music)
private val AtxShapes =
    Shapes(
        extraSmall = RoundedCornerShape(8.dp),
        small = RoundedCornerShape(12.dp),
        medium = RoundedCornerShape(16.dp),
        large = RoundedCornerShape(24.dp),
        extraLarge = RoundedCornerShape(32.dp),
    )

/**
 * Tema di ArchiveTuneX. I colori (sfondi compresi) vengono da:
 * - i colori di sistema (Material You, Android 12+; su iPhone non esistono e si usa [seed]),
 * - oppure una tavolozza Material costruita da [seed] (copertina o colore scelto).
 */
@Composable
fun ArchiveTuneXTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    seed: Color = AccentRed,
    paletteStyle: PaletteStyle = PaletteStyle.TonalSpot,
    useSystemColors: Boolean = false,
    pureBlack: Boolean = false,
    content: @Composable () -> Unit,
) {
    val system = if (useSystemColors) systemDynamicColorScheme(darkTheme) else null
    val generated =
        remember(seed, darkTheme, paletteStyle) {
            dynamicColorScheme(seedColor = seed, isDark = darkTheme, contrastLevel = 0.0, style = paletteStyle)
        }
    var scheme = system ?: generated
    if (darkTheme && pureBlack) {
        scheme =
            scheme.copy(
                background = Color.Black,
                surface = Color.Black,
                surfaceContainerLowest = Color.Black,
                surfaceContainerLow = scheme.surfaceContainerLowest,
            )
    }
    MaterialExpressiveTheme(
        colorScheme = scheme,
        typography = atxTypography(),
        shapes = AtxShapes,
        motionScheme = MotionScheme.expressive(),
        content = content,
    )
}
