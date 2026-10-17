package com.justlorenzo.archivetunex.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
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

@Composable
fun ArchiveTuneXTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    accent: Color = AccentRed,
    content: @Composable () -> Unit,
) {
    val base = if (darkTheme) DarkColors else LightColors
    // L'accento scelto (o quello della copertina) sostituisce il rosso di Fuji
    val scheme =
        if (accent == AccentRed) {
            base
        } else {
            val onAccent = if (accent.luminance() > 0.55f) Color(0xFF111114) else Color.White
            val container = accent.copy(alpha = if (darkTheme) 0.28f else 0.18f).compositeOver(base.background)
            val onContainer = if (darkTheme) accent.copy(alpha = 0.35f).compositeOver(Color.White) else accent.copy(alpha = 0.6f).compositeOver(Color.Black)
            base.copy(
                primary = accent,
                onPrimary = onAccent,
                primaryContainer = container,
                onPrimaryContainer = onContainer,
                secondary = accent,
                onSecondary = onAccent,
                secondaryContainer = container,
                onSecondaryContainer = onContainer,
                tertiary = accent,
                onTertiary = onAccent,
            )
        }
    MaterialExpressiveTheme(
        colorScheme = scheme,
        typography = AtxTypography,
        shapes = AtxShapes,
        motionScheme = MotionScheme.expressive(),
        content = content,
    )
}
