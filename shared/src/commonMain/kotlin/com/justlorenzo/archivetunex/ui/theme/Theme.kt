@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

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

// Tema iOS (Liquid Glass): bianco o nero come Apple Music, grigi di sistema e l'accento dell'icona
// (rosa del fiore nel tema scuro, prugna dello sfondo nel tema chiaro, che sul bianco si legge).
val IconPink = Color(0xFFFFB5CF)
val IconPlum = Color(0xFF3E1B42)
val IconPlumLight = Color(0xFF6B2F63)

private val IosDarkColors =
    darkColorScheme(
        primary = IconPink,
        onPrimary = IconPlum,
        primaryContainer = Color(0xFF3D2433),
        onPrimaryContainer = Color(0xFFFFD9E6),
        inversePrimary = IconPlumLight,
        secondary = IconPink,
        onSecondary = IconPlum,
        secondaryContainer = Color(0xFF2C2C2E),
        onSecondaryContainer = Color.White,
        tertiary = IconPink,
        onTertiary = IconPlum,
        tertiaryContainer = Color(0xFF3D2433),
        onTertiaryContainer = Color(0xFFFFD9E6),
        background = Color.Black,
        onBackground = Color.White,
        surface = Color.Black,
        onSurface = Color.White,
        surfaceVariant = Color(0xFF2C2C2E),
        onSurfaceVariant = Color(0xFF98989F),
        surfaceTint = Color.Black,
        outline = Color(0xFF48484A),
        outlineVariant = Color(0xFF38383A),
        surfaceBright = Color(0xFF3A3A3C),
        surfaceDim = Color.Black,
        surfaceContainerLowest = Color.Black,
        surfaceContainerLow = Color(0xFF121214),
        surfaceContainer = Color(0xFF1C1C1E),
        surfaceContainerHigh = Color(0xFF2C2C2E),
        surfaceContainerHighest = Color(0xFF3A3A3C),
        error = Color(0xFFFF453A),
        onError = Color.White,
    )

private val IosLightColors =
    lightColorScheme(
        primary = IconPlumLight,
        onPrimary = Color.White,
        primaryContainer = Color(0xFFFCE4EE),
        onPrimaryContainer = IconPlum,
        inversePrimary = IconPink,
        secondary = IconPlumLight,
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFE5E5EA),
        onSecondaryContainer = Color.Black,
        tertiary = IconPlumLight,
        onTertiary = Color.White,
        tertiaryContainer = Color(0xFFFCE4EE),
        onTertiaryContainer = IconPlum,
        background = Color.White,
        onBackground = Color.Black,
        surface = Color.White,
        onSurface = Color.Black,
        surfaceVariant = Color(0xFFE5E5EA),
        onSurfaceVariant = Color(0xFF8A8A8E),
        surfaceTint = Color.White,
        outline = Color(0xFFC6C6C8),
        outlineVariant = Color(0xFFD1D1D6),
        surfaceBright = Color.White,
        surfaceDim = Color(0xFFE5E5EA),
        surfaceContainerLowest = Color.White,
        surfaceContainerLow = Color(0xFFF7F7FA),
        surfaceContainer = Color(0xFFF2F2F7),
        surfaceContainerHigh = Color(0xFFE9E9EE),
        surfaceContainerHighest = Color(0xFFE5E5EA),
        error = Color(0xFFFF3B30),
        onError = Color.White,
    )

// Forme del tema iOS: angoli continui moderati come nelle app di Apple
private val IosShapes =
    Shapes(
        extraSmall = RoundedCornerShape(6.dp),
        small = RoundedCornerShape(8.dp),
        medium = RoundedCornerShape(10.dp),
        large = RoundedCornerShape(14.dp),
        extraLarge = RoundedCornerShape(26.dp),
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
    iosStyle: Boolean = false,
    content: @Composable () -> Unit,
) {
    if (iosStyle) {
        // Tema iOS: colori fissi di sistema (niente tavolozza dalla copertina) e font di sistema
        MaterialExpressiveTheme(
            colorScheme = if (darkTheme) IosDarkColors else IosLightColors,
            typography = iosTypography(),
            shapes = IosShapes,
            motionScheme = MotionScheme.standard(),
        ) {
            // Niente onde di Material al tocco: le righe si velano di grigio come su iPhone
            androidx.compose.runtime.CompositionLocalProvider(
                androidx.compose.material3.LocalRippleConfiguration provides null,
                androidx.compose.foundation.LocalIndication provides com.justlorenzo.archivetunex.ui.ios.IosHighlight,
                content = content,
            )
        }
        return
    }
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
