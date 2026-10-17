package moe.rukamori.archivetune.compat.squiggles

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderColors
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin

/**
 * Slider "ondulato" usato dal player di ArchiveTune.
 *
 * L'originale usa la libreria me.saket.squigglyslider, che esiste solo per Android:
 * questa è una versione multipiattaforma con la stessa API (SquigglySlider + SquigglySlider.SquigglesSpec),
 * così il codice del player resta identico.
 */
object SquigglySlider {
    @Immutable
    data class SquigglesSpec(
        val strokeWidth: Dp = 4.dp,
        val wavelength: Dp = 16.dp,
        val amplitude: Dp = 2.dp,
        /** Durata di un'onda completa che scorre, in millisecondi. */
        val periodMillis: Int = 1_000,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SquigglySlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    onValueChangeFinished: (() -> Unit)? = null,
    colors: SliderColors = SliderDefaults.colors(),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    squigglesSpec: SquigglySlider.SquigglesSpec = SquigglySlider.SquigglesSpec(),
) {
    val transition = rememberInfiniteTransition(label = "squiggles")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = squigglesSpec.periodMillis.coerceAtLeast(1), easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
        label = "squigglesPhase",
    )

    Slider(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
        valueRange = valueRange,
        steps = steps,
        onValueChangeFinished = onValueChangeFinished,
        colors = colors,
        interactionSource = interactionSource,
        thumb = {
            SliderDefaults.Thumb(
                interactionSource = interactionSource,
                colors = colors,
                enabled = enabled,
            )
        },
        track = { sliderState ->
            val range = sliderState.valueRange
            val span = (range.endInclusive - range.start).takeIf { it > 0f } ?: 1f
            val fraction = ((sliderState.value - range.start) / span).coerceIn(0f, 1f)
            val activeColor = if (enabled) colors.activeTrackColor else colors.disabledActiveTrackColor
            val inactiveColor = if (enabled) colors.inactiveTrackColor else colors.disabledInactiveTrackColor

            Canvas(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(squigglesSpec.strokeWidth + squigglesSpec.amplitude * 2),
            ) {
                val strokePx = squigglesSpec.strokeWidth.toPx()
                val amplitudePx = squigglesSpec.amplitude.toPx()
                val wavelengthPx = squigglesSpec.wavelength.toPx().coerceAtLeast(1f)
                val centerY = size.height / 2f
                val activeEnd = size.width * fraction

                // Parte già ascoltata: onda che scorre
                if (activeEnd > 0f) {
                    val path = Path()
                    val step = 1.5f
                    var x = 0f
                    path.moveTo(0f, centerY + waveY(0f, wavelengthPx, amplitudePx, phase))
                    while (x < activeEnd) {
                        x = (x + step).coerceAtMost(activeEnd)
                        path.lineTo(x, centerY + waveY(x, wavelengthPx, amplitudePx, phase))
                    }
                    drawPath(
                        path = path,
                        color = activeColor,
                        style = Stroke(width = strokePx, cap = StrokeCap.Round),
                    )
                }

                // Parte rimanente: linea dritta
                if (activeEnd < size.width) {
                    drawLine(
                        color = inactiveColor,
                        start = Offset(activeEnd, centerY),
                        end = Offset(size.width, centerY),
                        strokeWidth = strokePx,
                        cap = StrokeCap.Round,
                    )
                }
            }
        },
    )
}

private fun waveY(
    x: Float,
    wavelength: Float,
    amplitude: Float,
    phase: Float,
): Float = amplitude * sin(2f * PI.toFloat() * (x / wavelength - phase))
