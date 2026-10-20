@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.justlorenzo.archivetunex.ui.ios

import androidx.compose.material3.SegmentedButton
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import archivetunex.shared.generated.resources.Res
import archivetunex.shared.generated.resources.check
import archivetunex.shared.generated.resources.close
import archivetunex.shared.generated.resources.search
import com.justlorenzo.archivetunex.ui.LocalAtxStyle
import com.justlorenzo.archivetunex.ui.components.AtxIcon
import com.justlorenzo.archivetunex.ui.tr
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.DrawableResource
import androidx.compose.ui.Modifier.Node as ModifierNode

// Componenti in stile iOS per il tema iOS (Liquid Glass). Ognuno, col tema spento, disegna il
// componente Material 3 di sempre: le schermate usano questi e non cambiano altro.

/** Il tema iOS è acceso. */
val iosStyle: Boolean
    @Composable get() = LocalAtxStyle.current.liquidGlass

/**
 * Le forme Expressive (biscotto, fiore…) non esistono su iOS: col tema iOS diventano [ios]
 * (di solito un cerchio).
 */
@Composable
fun iosShape(
    expressive: androidx.compose.ui.graphics.Shape,
    ios: androidx.compose.ui.graphics.Shape = androidx.compose.foundation.shape.CircleShape,
): androidx.compose.ui.graphics.Shape = if (iosStyle) ios else expressive

/** Tema scuro (dal colore di sfondo). */
private val isDark: Boolean
    @Composable get() = MaterialTheme.colorScheme.background.luminance() < 0.5f

/** I grigi di riempimento di iOS (tertiarySystemFill). */
val iosFill: Color
    @Composable get() = if (isDark) Color(0x3D767680) else Color(0x1F767680)

/** Il verde degli interruttori di iOS. */
val iosGreen: Color
    @Composable get() = if (isDark) Color(0xFF30D158) else Color(0xFF34C759)

/** Il rosso delle azioni distruttive di iOS. */
val iosRed: Color
    @Composable get() = if (isDark) Color(0xFFFF453A) else Color(0xFFFF3B30)

/** Peso dei titoli: Black/ExtraBold in Material, Bold come nelle app di Apple. */
val heavyWeight: FontWeight
    @Composable get() = if (iosStyle) FontWeight.Bold else FontWeight.Black

val extraBoldWeight: FontWeight
    @Composable get() = if (iosStyle) FontWeight.Bold else FontWeight.ExtraBold

// region Tocco

/** Al tocco iOS non fa onde: la riga o il pulsante si velano di grigio finché il dito è giù. */
object IosHighlight : IndicationNodeFactory {
    override fun create(interactionSource: InteractionSource): DelegatableNode = IosHighlightNode(interactionSource)

    override fun equals(other: Any?): Boolean = other === this

    override fun hashCode(): Int = 7
}

private class IosHighlightNode(
    private val source: InteractionSource,
) : ModifierNode(),
    DrawModifierNode {
    private var pressed = false

    override fun onAttach() {
        coroutineScope.launch {
            var count = 0
            source.interactions.collect { interaction ->
                when (interaction) {
                    is PressInteraction.Press -> count++
                    is PressInteraction.Release -> count--
                    is PressInteraction.Cancel -> count--
                }
                val now = count > 0
                if (now != pressed) {
                    pressed = now
                    invalidateDraw()
                }
            }
        }
    }

    override fun ContentDrawScope.draw() {
        drawContent()
        if (pressed) drawRect(Color(0x33808080))
    }
}

// endregion

// region Interruttore

@Composable
fun AtxSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    if (!iosStyle) {
        Switch(checked = checked, onCheckedChange = onCheckedChange, modifier = modifier, enabled = enabled)
        return
    }
    val track by animateColorAsState(if (checked) iosGreen else if (isDark) Color(0xFF39393D) else Color(0xFFE9E9EB), tween(220), label = "switchTrack")
    val offset by animateDpAsState(if (checked) 22.dp else 2.dp, spring(dampingRatio = 0.75f, stiffness = 600f), label = "switchThumb")
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val thumbWidth by animateDpAsState(if (pressed) 33.dp else 27.dp, spring(stiffness = 700f), label = "switchStretch")
    Box(
        modifier
            .size(51.dp, 31.dp)
            .graphicsLayer { alpha = if (enabled) 1f else 0.5f }
            .clip(CircleShape)
            .background(track)
            .clickable(interactionSource = source, indication = null, enabled = enabled && onCheckedChange != null) { onCheckedChange?.invoke(!checked) },
    ) {
        Box(
            Modifier
                .offset(x = if (checked) offset - (thumbWidth - 27.dp) else offset, y = 2.dp)
                .size(thumbWidth, 27.dp)
                .shadow(3.dp, CircleShape)
                .background(Color.White, CircleShape),
        )
    }
}

// endregion

// region Slider

/** Slider di iOS: barra sottile e pallino bianco con ombra. */
@Composable
fun AtxSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    onValueChangeFinished: (() -> Unit)? = null,
    activeColor: Color = MaterialTheme.colorScheme.primary,
) {
    if (!iosStyle) {
        androidx.compose.material3.Slider(
            value = value,
            onValueChange = onValueChange,
            modifier = modifier,
            valueRange = valueRange,
            steps = steps,
            onValueChangeFinished = onValueChangeFinished,
        )
        return
    }
    val currentChange by rememberUpdatedState(onValueChange)
    val currentFinished by rememberUpdatedState(onValueChangeFinished)
    val span = (valueRange.endInclusive - valueRange.start).takeIf { it > 0f } ?: 1f
    val fraction = ((value - valueRange.start) / span).coerceIn(0f, 1f)
    BoxWithConstraints(modifier.fillMaxWidth().height(36.dp)) {
        val density = LocalDensity.current
        val trackWidth = maxWidth
        val widthPx = with(density) { trackWidth.toPx() }
        val thumbPx = with(density) { 28.dp.toPx() }
        fun valueAt(x: Float): Float {
            var f = ((x - thumbPx / 2f) / (widthPx - thumbPx)).coerceIn(0f, 1f)
            if (steps > 0) {
                val parts = steps + 1
                f = kotlin.math.round(f * parts) / parts
            }
            return valueRange.start + f * span
        }
        Box(
            Modifier
                .fillMaxSize()
                .pointerInput(widthPx, steps, valueRange) {
                    detectTapGestures { pos ->
                        currentChange(valueAt(pos.x))
                        currentFinished?.invoke()
                    }
                }.pointerInput(widthPx, steps, valueRange) {
                    detectHorizontalDragGestures(
                        onDragEnd = { currentFinished?.invoke() },
                        onDragCancel = { currentFinished?.invoke() },
                    ) { change, _ -> currentChange(valueAt(change.position.x)) }
                },
            contentAlignment = Alignment.CenterStart,
        ) {
            Box(Modifier.padding(horizontal = 14.dp).fillMaxWidth().height(4.dp).clip(CircleShape).background(iosFill))
            Box(
                Modifier
                    .padding(start = 14.dp)
                    .width((trackWidth - 28.dp) * fraction)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(activeColor),
            )
            Box(
                Modifier
                    .offset(x = (trackWidth - 28.dp) * fraction)
                    .size(28.dp)
                    .shadow(4.dp, CircleShape)
                    .background(Color.White, CircleShape),
            )
        }
    }
}

// endregion

// region Controllo segmentato

/** Controllo segmentato: la scelta attiva è una pillola che scivola (come in iOS 26). */
@Composable
fun AtxSegmented(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!iosStyle) {
        androidx.compose.material3.SingleChoiceSegmentedButtonRow(modifier.fillMaxWidth()) {
            options.forEachIndexed { index, label ->
                SegmentedButton(
                    selected = index == selectedIndex,
                    onClick = { onSelect(index) },
                    shape = androidx.compose.material3.SegmentedButtonDefaults.itemShape(index, options.size),
                ) { Text(label) }
            }
        }
        return
    }
    val dark = isDark
    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .height(36.dp)
            .clip(CircleShape)
            .background(iosFill)
            .padding(2.dp),
    ) {
        val segment = maxWidth / options.size.coerceAtLeast(1)
        val x by animateDpAsState(segment * selectedIndex, spring(dampingRatio = 0.8f, stiffness = 500f), label = "segment")
        Box(
            Modifier
                .offset(x = x)
                .width(segment)
                .fillMaxSize()
                .shadow(if (dark) 0.dp else 2.dp, CircleShape)
                .background(if (dark) Color(0xFF636366) else Color.White, CircleShape),
        )
        Row(Modifier.fillMaxSize()) {
            options.forEachIndexed { index, label ->
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .clip(CircleShape)
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onSelect(index) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        label,
                        fontSize = 13.sp,
                        fontWeight = if (index == selectedIndex) FontWeight.SemiBold else FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

// endregion

// region Chip

/** Chip dei filtri: capsula grigia, piena del colore d'accento quando è scelta. */
@Composable
fun AtxFilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: (@Composable () -> Unit)? = null,
) {
    if (!iosStyle) {
        FilterChip(
            selected = selected,
            onClick = onClick,
            label = label,
            modifier = modifier,
            leadingIcon = leadingIcon,
            shape = RoundedCornerShape(20.dp),
            colors =
                androidx.compose.material3.FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                ),
        )
        return
    }
    val background by animateColorAsState(if (selected) MaterialTheme.colorScheme.primary else iosFill, tween(180), label = "chip")
    val content by animateColorAsState(if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface, tween(180), label = "chipText")
    Row(
        modifier
            .height(34.dp)
            .clip(CircleShape)
            .background(background)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        CompositionLocalProvider(LocalContentColor provides content) {
            ProvideTextStyle(TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Medium, color = content)) {
                leadingIcon?.invoke()
                label()
            }
        }
    }
}

// endregion

// region Caricamento

/** La rotellina di caricamento di iOS: 8 raggi che si accendono in giro. */
@Composable
fun IosSpinner(
    modifier: Modifier = Modifier,
    size: Dp = 22.dp,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    val turn by rememberInfiniteTransition(label = "spinner").animateFloat(
        initialValue = 0f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(tween(800, easing = LinearEasing)),
        label = "spinnerStep",
    )
    val step = turn.toInt()
    Box(modifier.size(size)) {
        androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
            val r = this.size.minDimension / 2f
            val w = r * 0.26f
            for (i in 0 until 8) {
                val alpha = 0.25f + 0.75f * (((i - step) % 8 + 8) % 8).let { 1f - it / 8f }
                val angle = SpinnerMath.toRadiansCompat(i * 45.0 - 90.0)
                val inner = r * 0.45f
                val outer = r - w / 2f
                drawLine(
                    color = color.copy(alpha = alpha),
                    start = center + androidx.compose.ui.geometry.Offset((kotlin.math.cos(angle) * inner).toFloat(), (kotlin.math.sin(angle) * inner).toFloat()),
                    end = center + androidx.compose.ui.geometry.Offset((kotlin.math.cos(angle) * outer).toFloat(), (kotlin.math.sin(angle) * outer).toFloat()),
                    strokeWidth = w,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round,
                )
            }
        }
    }
}

private object SpinnerMath {
    fun toRadiansCompat(degrees: Double): Double = degrees / 180.0 * kotlin.math.PI
}

/** Indicatore di caricamento: rotellina di iOS o indicatore Expressive. */
@Composable
fun AtxLoading(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    color: Color? = null,
    contained: Boolean = false,
) {
    if (iosStyle) {
        IosSpinner(modifier, size = (size * 0.5f).coerceIn(18.dp, 32.dp), color = color ?: MaterialTheme.colorScheme.onSurfaceVariant)
    } else if (contained) {
        ContainedLoadingIndicator(modifier)
    } else if (color != null) {
        LoadingIndicator(modifier.size(size), color = color)
    } else {
        LoadingIndicator(modifier.size(size))
    }
}

/** Trascina in giù per aggiornare: rotellina di iOS sopra la lista, oppure l'indicatore Expressive. */
@Composable
fun AtxPullToRefreshBox(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    state: PullToRefreshState = rememberPullToRefreshState(),
    content: @Composable BoxScope.() -> Unit,
) {
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier,
        state = state,
        indicator = {
            if (iosStyle) {
                val shown = if (isRefreshing) 1f else state.distanceFraction.coerceIn(0f, 1f)
                IosSpinner(
                    Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 64.dp)
                        .graphicsLayer {
                            alpha = shown
                            scaleX = 0.6f + 0.4f * shown
                            scaleY = 0.6f + 0.4f * shown
                        },
                    size = 26.dp,
                )
            } else {
                PullToRefreshDefaults.LoadingIndicator(
                    state = state,
                    isRefreshing = isRefreshing,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = 48.dp),
                )
            }
        },
        content = content,
    )
}

// endregion

// region Pulsanti

/** Ruolo dei pulsanti dentro un avviso di iOS. */
private enum class AlertRole { None, Confirm, Cancel }

private val LocalAlertRole = staticCompositionLocalOf { AlertRole.None }

/** Pulsante di solo testo: in iOS testo nel colore d'accento (nelle finestre di avviso, capsule). */
@Composable
fun AtxTextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    destructive: Boolean = false,
    content: @Composable RowScope.() -> Unit,
) {
    if (!iosStyle) {
        TextButton(onClick = onClick, modifier = modifier, enabled = enabled, content = content)
        return
    }
    val role = LocalAlertRole.current
    val accent = MaterialTheme.colorScheme.primary
    val textColor =
        when {
            destructive -> iosRed
            role == AlertRole.Confirm -> MaterialTheme.colorScheme.onPrimary
            role == AlertRole.Cancel -> MaterialTheme.colorScheme.onSurface
            else -> accent
        }
    val background =
        when (role) {
            AlertRole.Confirm -> if (destructive) iosFill else accent
            AlertRole.Cancel -> iosFill
            AlertRole.None -> Color.Transparent
        }
    Row(
        modifier
            .then(if (role != AlertRole.None) Modifier.fillMaxWidth().height(48.dp) else Modifier.height(40.dp))
            .graphicsLayer { alpha = if (enabled) 1f else 0.4f }
            .clip(CircleShape)
            .background(background)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CompositionLocalProvider(LocalContentColor provides textColor) {
            ProvideTextStyle(TextStyle(fontSize = 17.sp, fontWeight = if (role == AlertRole.None) FontWeight.Normal else FontWeight.SemiBold, color = textColor)) {
                content()
            }
        }
    }
}

/** Pulsante principale: capsula piena del colore d'accento. */
@Composable
fun AtxButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tonal: Boolean = false,
    highlighted: Boolean = false,
    content: @Composable RowScope.() -> Unit,
) {
    if (!iosStyle) {
        if (tonal) {
            androidx.compose.material3.FilledTonalButton(
                onClick = onClick,
                modifier = modifier,
                enabled = enabled,
                colors =
                    if (highlighted) {
                        androidx.compose.material3.ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        )
                    } else {
                        androidx.compose.material3.ButtonDefaults.filledTonalButtonColors()
                    },
                content = content,
            )
        } else {
            androidx.compose.material3.Button(onClick = onClick, modifier = modifier, enabled = enabled, content = content)
        }
        return
    }
    val background = if (tonal && !highlighted) iosFill else MaterialTheme.colorScheme.primary
    val text = if (tonal && !highlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onPrimary
    Row(
        modifier
            .height(50.dp)
            .graphicsLayer { alpha = if (enabled) 1f else 0.4f }
            .clip(RoundedCornerShape(14.dp))
            .background(background)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CompositionLocalProvider(LocalContentColor provides text) {
            ProvideTextStyle(TextStyle(fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = text)) { content() }
        }
    }
}

// endregion

// region Avvisi

/**
 * Finestra di avviso. Col tema iOS è quella di iOS 26: angoli molto arrotondati, titolo e testo
 * centrati e i pulsanti come capsule (quello di conferma pieno del colore d'accento).
 */
@Composable
fun AtxAlertDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: (@Composable () -> Unit)? = null,
    title: (@Composable () -> Unit)? = null,
    text: (@Composable () -> Unit)? = null,
) {
    if (!iosStyle) {
        AlertDialog(
            onDismissRequest = onDismissRequest,
            confirmButton = confirmButton,
            modifier = modifier,
            dismissButton = dismissButton,
            title = title,
            text = text,
        )
        return
    }
    val dark = isDark
    Dialog(onDismissRequest = onDismissRequest) {
        Column(
            modifier
                .widthIn(max = 320.dp)
                .fillMaxWidth()
                .shadow(24.dp, RoundedCornerShape(34.dp))
                .clip(RoundedCornerShape(34.dp))
                .background(if (dark) Color(0xFF2C2C2E) else Color(0xFFF7F7F9))
                .padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (title != null) {
                ProvideTextStyle(TextStyle(fontSize = 17.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurface)) { title() }
            }
            if (text != null) {
                ProvideTextStyle(TextStyle(fontSize = 15.sp, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurface)) {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { text() }
                }
            }
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (dismissButton != null) {
                    Box(Modifier.weight(1f)) {
                        CompositionLocalProvider(LocalAlertRole provides AlertRole.Cancel) { dismissButton() }
                    }
                }
                Box(Modifier.weight(1f)) {
                    CompositionLocalProvider(LocalAlertRole provides AlertRole.Confirm) { confirmButton() }
                }
            }
        }
    }
}

/** Scelta in un elenco: in iOS una spunta d'accento a destra, in Material il pallino. */
@Composable
fun AtxRadio(
    selected: Boolean,
    onClick: (() -> Unit)?,
) {
    if (!iosStyle) {
        androidx.compose.material3.RadioButton(selected = selected, onClick = onClick)
        return
    }
    Box(Modifier.size(28.dp), contentAlignment = Alignment.Center) {
        if (selected) AtxIcon(Res.drawable.check, null, size = 20.dp, tint = MaterialTheme.colorScheme.primary)
    }
}

// endregion

// region Campi di testo

/** Campo di ricerca: in iOS capsula grigia con lente e crocetta per cancellare. */
@Composable
fun AtxSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    onSearch: (() -> Unit)? = null,
) {
    if (!iosStyle) {
        TextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder) },
            leadingIcon = { AtxIcon(Res.drawable.search, null, size = 22.dp, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
            trailingIcon =
                if (value.isNotEmpty()) {
                    {
                        androidx.compose.material3.IconButton(onClick = { onValueChange("") }) {
                            AtxIcon(Res.drawable.close, tr("Cancella"), size = 20.dp, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else {
                    null
                },
            singleLine = true,
            shape = RoundedCornerShape(28.dp),
            colors =
                TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                ),
            keyboardOptions = KeyboardOptions(imeAction = if (onSearch != null) androidx.compose.ui.text.input.ImeAction.Search else androidx.compose.ui.text.input.ImeAction.Default),
            keyboardActions = KeyboardActions(onSearch = { onSearch?.invoke() }),
            modifier = modifier,
        )
        return
    }
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = TextStyle(fontSize = 17.sp, color = MaterialTheme.colorScheme.onSurface),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        keyboardOptions = KeyboardOptions(imeAction = if (onSearch != null) androidx.compose.ui.text.input.ImeAction.Search else androidx.compose.ui.text.input.ImeAction.Default),
        keyboardActions = KeyboardActions(onSearch = { onSearch?.invoke() }),
        modifier = modifier,
        decorationBox = { field ->
            Row(
                Modifier.fillMaxWidth().height(40.dp).clip(CircleShape).background(iosFill).padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                AtxIcon(Res.drawable.search, null, size = 18.dp, tint = muted)
                Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    if (value.isEmpty()) Text(placeholder, fontSize = 17.sp, color = muted, maxLines = 1)
                    field()
                }
                if (value.isNotEmpty()) {
                    Box(
                        Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(muted.copy(alpha = 0.6f))
                            .clickable { onValueChange("") },
                        contentAlignment = Alignment.Center,
                    ) { AtxIcon(Res.drawable.close, tr("Cancella"), size = 11.dp, tint = MaterialTheme.colorScheme.background) }
                }
            }
        },
    )
}

/** Campo di testo semplice (nei dialoghi): in iOS rettangolo grigio arrotondato senza bordo. */
@Composable
fun AtxTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    singleLine: Boolean = true,
) {
    if (!iosStyle) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = label?.let { { Text(it) } },
            singleLine = singleLine,
            modifier = modifier,
        )
        return
    }
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = singleLine,
        textStyle = TextStyle(fontSize = 17.sp, color = MaterialTheme.colorScheme.onSurface),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        modifier = modifier,
        decorationBox = { field ->
            Box(
                Modifier.fillMaxWidth().height(44.dp).clip(RoundedCornerShape(12.dp)).background(iosFill).padding(horizontal = 12.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (value.isEmpty() && label != null) Text(label, fontSize = 17.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                field()
            }
        },
    )
}

// endregion

// region Menu

/** Menu a comparsa: in iOS un riquadro arrotondato traslucido con le voci e l'icona a destra. */
@Composable
fun AtxDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    if (!iosStyle) {
        DropdownMenu(expanded = expanded, onDismissRequest = onDismissRequest, modifier = modifier, shape = RoundedCornerShape(20.dp), content = content)
        return
    }
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier.widthIn(min = 250.dp),
        shape = RoundedCornerShape(26.dp),
        containerColor = if (isDark) Color(0xF22C2C2E) else Color(0xF7F9F9FB),
        tonalElevation = 0.dp,
        shadowElevation = 24.dp,
        content = content,
    )
}

/** Voce di menu: in iOS testo a sinistra e icona a destra, rossa se distruttiva. */
@Composable
fun AtxMenuItem(
    text: String,
    icon: DrawableResource?,
    onClick: () -> Unit,
    danger: Boolean = false,
    selected: Boolean = false,
) {
    val color = if (danger) (if (iosStyle) iosRed else MaterialTheme.colorScheme.error) else MaterialTheme.colorScheme.onSurface
    if (!iosStyle) {
        DropdownMenuItem(
            text = { Text(text, color = color, fontWeight = FontWeight.SemiBold) },
            leadingIcon = icon?.let { { AtxIcon(it, null, size = 20.dp, tint = color) } },
            trailingIcon = if (selected) ({ AtxIcon(Res.drawable.check, null, size = 20.dp, tint = MaterialTheme.colorScheme.primary) }) else null,
            onClick = onClick,
        )
        return
    }
    Row(
        Modifier
            .fillMaxWidth()
            .height(46.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.width(18.dp)) {
            if (selected) AtxIcon(Res.drawable.check, null, size = 16.dp, tint = color)
        }
        Text(text, color = color, fontSize = 17.sp, modifier = Modifier.weight(1f), maxLines = 1)
        if (icon != null) AtxIcon(icon, null, size = 20.dp, tint = color)
    }
}

// endregion
