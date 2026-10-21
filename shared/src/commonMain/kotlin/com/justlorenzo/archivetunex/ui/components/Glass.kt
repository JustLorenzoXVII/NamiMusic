@file:OptIn(dev.chrisbanes.haze.ExperimentalHazeApi::class)

package com.justlorenzo.archivetunex.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.justlorenzo.archivetunex.ui.LocalAtxStyle
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.glass.GlassStyle
import dev.chrisbanes.haze.glass.hazeGlass
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import org.jetbrains.compose.resources.DrawableResource

// Vetro del tema iOS disegnato in Compose (con Haze): pulsanti che galleggiano sopra le copertine,
// menu e fogli. La barra in basso invece è nativa (GlassChrome).

/** Il contenuto che il vetro di questa pagina riflette (null se il tema iOS è spento). */
val LocalGlassState = staticCompositionLocalOf<HazeState?> { null }

/** Il tema iOS con il vetro è acceso. */
val glassEnabled: Boolean
    @Composable get() = LocalAtxStyle.current.liquidGlass

/**
 * Una pagina con vetro: dentro, [glassSource] segna il contenuto che si vede attraverso il vetro
 * e [glassSurface] disegna il vetro sopra di lui.
 */
@Composable
fun GlassScope(content: @Composable () -> Unit) {
    if (!glassEnabled) {
        content()
        return
    }
    val state = rememberHazeState()
    CompositionLocalProvider(LocalGlassState provides state) { content() }
}

/** Il contenuto della pagina che il vetro riflette. */
@Composable
fun Modifier.glassSource(): Modifier {
    val state = LocalGlassState.current ?: return this
    return this.hazeSource(state)
}

/** Vetro (con la forma [shape]); senza tema iOS, o senza contenuto da riflettere, [fallback] pieno. */
@Composable
fun Modifier.glassSurface(
    shape: RoundedCornerShape,
    fallback: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    state: HazeState? = LocalGlassState.current,
    dark: Boolean = MaterialTheme.colorScheme.background.luminance() < 0.5f,
): Modifier {
    if (state == null || !glassEnabled) return this.clip(shape).background(fallback)
    val style =
        remember(shape, dark) {
            // Vetro "clear" (quello dei controlli di iOS sopra foto e video): trasparente, con la
            // rifrazione forte sul bordo e i riflessi; solo un filo di bianco per staccare dallo sfondo
            GlassStyle.clear.then {
                shape(shape)
                tint(Color.White.copy(alpha = if (dark) 0.08f else 0.22f))
                specularIntensity(0.75f)
            }
        }
    return this.hazeGlass(input = HazeInput.Sources(state), style = style)
}

/** Pulsante tondo di vetro con un'icona (indietro, menu, cestino…). */
@Composable
fun GlassIconButton(
    icon: DrawableResource,
    label: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    iconSize: Dp = 22.dp,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    state: HazeState? = LocalGlassState.current,
    dark: Boolean = MaterialTheme.colorScheme.background.luminance() < 0.5f,
) {
    Box(
        modifier
            .size(size)
            .glassSurface(CircleShape, state = state, dark = dark)
            .clip(CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) { AtxIcon(icon, label, size = iconSize, tint = tint) }
}

/**
 * Fogli aperti in questo momento. Col tema iOS la barra di vetro nativa sta sopra a tutto,
 * quindi mentre c'è un foglio aperto si nasconde.
 */
object OpenSheets {
    var count by mutableIntStateOf(0)
        private set

    internal fun opened() {
        count++
    }

    internal fun closed() {
        count = (count - 1).coerceAtLeast(0)
    }
}

/** Foglio dal basso dell'app: col tema iOS ha gli angoli grandi e il grigio di sistema, come su iPhone. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AtxModalSheet(
    onDismissRequest: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(),
    content: @Composable ColumnScope.() -> Unit,
) {
    DisposableEffect(Unit) {
        OpenSheets.opened()
        onDispose { OpenSheets.closed() }
    }
    if (glassEnabled) {
        // iOS 26: foglio staccato dai bordi, angoli grandi e bordo di vetro
        val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
        val shape = RoundedCornerShape(38.dp)
        ModalBottomSheet(
            onDismissRequest = onDismissRequest,
            sheetState = sheetState,
            modifier =
                Modifier
                    .padding(horizontal = 8.dp)
                    .border(
                        0.8.dp,
                        androidx.compose.ui.graphics.Brush.verticalGradient(
                            listOf(Color.White.copy(alpha = if (dark) 0.35f else 0.9f), Color.White.copy(alpha = 0.04f)),
                        ),
                        shape,
                    ),
            shape = shape,
            containerColor = if (dark) Color(0xF51C1C1E) else Color(0xF7F7F7FA),
            tonalElevation = 0.dp,
            content = content,
        )
    } else {
        ModalBottomSheet(onDismissRequest = onDismissRequest, sheetState = sheetState, content = content)
    }
}

/** Un contenitore che usa tutto lo spazio della pagina per il vetro (contenuto + pulsanti sopra). */
@Composable
fun GlassPage(
    modifier: Modifier = Modifier,
    background: @Composable BoxScope.() -> Unit = {},
    overlay: @Composable BoxScope.() -> Unit,
    content: @Composable BoxScope.() -> Unit,
) {
    GlassScope {
        Box(modifier) {
            Box(Modifier.fillMaxSize().glassSource()) {
                background()
                content()
            }
            overlay()
        }
    }
}
