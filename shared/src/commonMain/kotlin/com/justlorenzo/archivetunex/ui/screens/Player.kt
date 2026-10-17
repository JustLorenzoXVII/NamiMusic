@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.justlorenzo.archivetunex.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import archivetunex.shared.generated.resources.Res
import archivetunex.shared.generated.resources.expand_more
import archivetunex.shared.generated.resources.favorite
import archivetunex.shared.generated.resources.favorite_border
import archivetunex.shared.generated.resources.lyrics
import archivetunex.shared.generated.resources.more_vert
import archivetunex.shared.generated.resources.pause
import archivetunex.shared.generated.resources.play
import archivetunex.shared.generated.resources.queue_music
import archivetunex.shared.generated.resources.repeat
import archivetunex.shared.generated.resources.repeat_one
import archivetunex.shared.generated.resources.shuffle
import archivetunex.shared.generated.resources.skip_next
import archivetunex.shared.generated.resources.skip_previous
import com.justlorenzo.archivetunex.ui.LocalActions
import com.justlorenzo.archivetunex.ui.LocalAtxStyle
import com.justlorenzo.archivetunex.ui.LocalNavigator
import com.justlorenzo.archivetunex.ui.MiniArtwork
import com.justlorenzo.archivetunex.ui.MiniButtons
import com.justlorenzo.archivetunex.ui.MiniLayout
import com.justlorenzo.archivetunex.ui.MiniPlayerStyle
import com.justlorenzo.archivetunex.ui.MiniProgress
import com.justlorenzo.archivetunex.ui.PlayButtonShape
import com.justlorenzo.archivetunex.ui.PlayerBackground
import com.justlorenzo.archivetunex.ui.PlayerLayout
import com.justlorenzo.archivetunex.ui.Route
import com.justlorenzo.archivetunex.ui.SliderStyleChoice
import com.justlorenzo.archivetunex.ui.components.Artwork
import com.justlorenzo.archivetunex.ui.components.AtxIcon
import com.justlorenzo.archivetunex.ui.components.SongMenuSheet
import com.justlorenzo.archivetunex.ui.components.SongRow
import com.justlorenzo.archivetunex.ui.components.animatedArtworkColor
import com.justlorenzo.archivetunex.ui.components.readableOn
import com.justlorenzo.archivetunex.ui.components.rememberArtworkColors
import com.justlorenzo.archivetunex.ui.components.tint
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import moe.rukamori.archivetune.compat.media3.C
import moe.rukamori.archivetune.compat.media3.Player
import moe.rukamori.archivetune.extensions.metadata
import moe.rukamori.archivetune.playback.PlayerConnection
import org.jetbrains.compose.resources.DrawableResource
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import androidx.compose.ui.graphics.lerp as lerpColor

// region Utilità

/**
 * Posizione e durata del brano. Sono stati separati: li leggono solo barra, tempi e cassetta,
 * così il resto del player non si ridisegna a ogni aggiornamento.
 */
private class ProgressState {
    val position = mutableLongStateOf(0L)
    val duration = mutableLongStateOf(0L)

    fun fraction(): Float {
        val d = duration.longValue
        return if (d > 0) (position.longValue.toFloat() / d).coerceIn(0f, 1f) else 0f
    }
}

@Composable
private fun rememberProgressState(connection: PlayerConnection): ProgressState {
    val state = remember { ProgressState() }
    LaunchedEffect(connection) {
        while (true) {
            state.position.longValue = connection.player.currentPosition.coerceAtLeast(0L)
            state.duration.longValue = connection.player.duration.takeIf { it != C.TIME_UNSET && it > 0 } ?: 0L
            delay(200)
        }
    }
    return state
}

private fun formatTime(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}

/** Rotazione continua (vinile del mini player, Play che gira). */
@Composable
private fun rememberSpin(durationMs: Int): Float {
    val rotation by rememberInfiniteTransition(label = "spin").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(durationMs, easing = LinearEasing), RepeatMode.Restart),
        label = "rotation",
    )
    return rotation
}

/** Disco in vinile con i solchi e la copertina come etichetta al centro (mini player). */
@Composable
private fun VinylDisc(
    artworkUrl: String?,
    rotation: Float,
    modifier: Modifier = Modifier,
    labelFraction: Float = 0.42f,
) {
    BoxWithConstraints(modifier.rotate(rotation).clip(CircleShape).background(Color(0xFF111114)), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val maxRadius = size.minDimension / 2f
            var r = maxRadius * 0.96f
            while (r > maxRadius * (labelFraction + 0.04f)) {
                drawCircle(Color.White.copy(alpha = 0.07f), radius = r, style = Stroke(width = 1.2f))
                r -= maxRadius * 0.035f
            }
        }
        Artwork(artworkUrl, Modifier.size(maxWidth * labelFraction), CircleShape, sizePx = 400)
        Box(Modifier.size(maxWidth * 0.05f).clip(CircleShape).background(Color(0xFF111114)))
    }
}

/** Swipe orizzontale per cambiare brano: restituisce lo spostamento e il modificatore da applicare. */
private class TrackSwipe(
    val offset: State<Float>,
    val modifier: Modifier,
)

@Composable
private fun rememberTrackSwipe(
    threshold: Dp = 90.dp,
    onNext: (fromOffset: Float) -> Boolean = { false },
    onSwipe: (forward: Boolean) -> Unit,
): TrackSwipe {
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val thresholdPx = with(LocalDensity.current) { threshold.toPx() }
    val offset = remember { mutableFloatStateOf(0f) }
    var job by remember { mutableStateOf<Job?>(null) }
    val state =
        rememberDraggableState { delta ->
            // Oltre la soglia diventa più "duro", per far capire che il brano cambierà
            offset.floatValue += delta * if (abs(offset.floatValue) > thresholdPx) 0.4f else 1f
        }
    val modifier =
        Modifier.draggable(
            state = state,
            orientation = Orientation.Horizontal,
            onDragStarted = { job?.cancel() },
            onDragStopped = { velocity ->
                val forward = offset.floatValue < -thresholdPx || velocity < -1200f
                val back = offset.floatValue > thresholdPx || velocity > 1200f
                if (forward) {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    // Pila: la carta vola via da sola, qui si riparte da zero
                    if (onNext(offset.floatValue)) {
                        offset.floatValue = 0f
                        onSwipe(true)
                        return@draggable
                    }
                    onSwipe(true)
                } else if (back) {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onSwipe(false)
                }
                job =
                    scope.launch {
                        animate(offset.floatValue, 0f, animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow)) { v, _ ->
                            offset.floatValue = v
                        }
                    }
            },
        )
    return TrackSwipe(offset, modifier)
}

// endregion

// region Mini player

private data class MiniGeometry(
    val shape: Shape,
    val outerPadding: Dp,
    val height: Dp,
    val artSize: Dp,
)

/** Mini player: forma, colori, copertina, pulsanti e avanzamento a scelta. Scorri per cambiare brano. */
@Composable
fun MiniPlayer(modifier: Modifier = Modifier) {
    val actions = LocalActions.current
    val navigator = LocalNavigator.current
    val connection = actions.player
    val metadata by connection.mediaMetadata.collectAsState()
    val isPlaying by connection.isPlaying.collectAsState()
    val song = metadata ?: return
    val progressState = rememberProgressState(connection)
    val style = LocalAtxStyle.current
    val colors = rememberArtworkColors(song.thumbnailUrl)
    val accent = animatedArtworkColor(colors)
    val swipe =
        rememberTrackSwipe { forward ->
            if (forward) connection.seekToNext() else connection.player.seekToPreviousMediaItem()
        }

    val surface = MaterialTheme.colorScheme.surfaceContainerHigh
    val background: Brush =
        when (style.miniPlayer) {
            MiniPlayerStyle.Tinted -> Brush.linearGradient(listOf(accent.tint(surface, 0.3f), accent.tint(surface, 0.3f)))
            MiniPlayerStyle.Gradient ->
                Brush.horizontalGradient(listOf(accent.tint(surface, 0.7f), (colors?.gradient?.getOrNull(1) ?: accent).tint(surface, 0.35f)))
            MiniPlayerStyle.Accent -> Brush.linearGradient(listOf(accent, accent))
            MiniPlayerStyle.Neutral -> Brush.linearGradient(listOf(surface, surface))
        }
    val content =
        when (style.miniPlayer) {
            MiniPlayerStyle.Accent -> accent.readableOn()
            MiniPlayerStyle.Gradient -> accent.tint(surface, 0.7f).readableOn()
            else -> MaterialTheme.colorScheme.onSurface
        }
    val progressColor = if (style.miniPlayer == MiniPlayerStyle.Accent || style.miniPlayer == MiniPlayerStyle.Gradient) content else accent

    val geometry =
        when (style.miniLayout) {
            MiniLayout.Floating -> MiniGeometry(RoundedCornerShape(26.dp), 0.dp, 68.dp, 48.dp)
            MiniLayout.Pill -> MiniGeometry(RoundedCornerShape(50), 0.dp, 58.dp, 42.dp)
            MiniLayout.Large -> MiniGeometry(RoundedCornerShape(30.dp), 0.dp, 92.dp, 72.dp)
        }
    val wave = style.miniProgress == MiniProgress.Wave
    val spin = rememberSpin(8000)

    Box(
        modifier
            .fillMaxWidth()
            .padding(horizontal = geometry.outerPadding)
            .shadow(14.dp, geometry.shape)
            .clip(geometry.shape)
            .background(background)
            .then(swipe.modifier)
            .clickable { navigator.playerExpanded = true },
    ) {
        // Avanzamento "riempimento": lo sfondo si colora da sinistra
        if (style.miniProgress == MiniProgress.Fill) {
            Box(
                Modifier
                    .height(geometry.height + if (wave) 12.dp else 0.dp)
                    .fillMaxWidth()
                    .drawBehind { drawRect(progressColor.copy(alpha = 0.2f), size = Size(size.width * progressState.fraction(), size.height)) },
            )
        }
        Column {
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(geometry.height)
                    .graphicsLayer {
                        translationX = swipe.offset.value
                        alpha = 1f - (abs(swipe.offset.value) / 600f).coerceAtMost(0.6f)
                    }.padding(start = ((geometry.height - geometry.artSize) / 2 - 4.dp).coerceAtLeast(0.dp), end = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(Modifier.size(geometry.artSize + 8.dp), contentAlignment = Alignment.Center) {
                    if (style.miniArtwork == MiniArtwork.Vinyl) {
                        VinylDisc(song.thumbnailUrl, if (isPlaying) spin else 0f, Modifier.size(geometry.artSize))
                    } else {
                        val artShape: Shape =
                            when (style.miniArtwork) {
                                MiniArtwork.Circle -> CircleShape
                                MiniArtwork.Cookie -> MaterialShapes.Cookie9Sided.toShape()
                                else -> if (style.miniLayout == MiniLayout.Pill) CircleShape else RoundedCornerShape(14.dp)
                            }
                        Artwork(song.thumbnailUrl, Modifier.size(geometry.artSize), artShape, sizePx = 226)
                    }
                    when (style.miniProgress) {
                        MiniProgress.Ring ->
                            CircularProgressIndicator(
                                progress = { progressState.fraction() },
                                modifier = Modifier.size(geometry.artSize + 6.dp),
                                color = progressColor,
                                strokeWidth = 3.dp,
                                trackColor = Color.Transparent,
                            )
                        // Cornice: l'avanzamento corre lungo il bordo della copertina (Fuji)
                        MiniProgress.Frame ->
                            ProgressFrame(
                                progress = { progressState.fraction() },
                                color = progressColor,
                                track = content.copy(alpha = 0.15f),
                                radius = if (style.miniLayout == MiniLayout.Pill) geometry.artSize else 17.dp,
                                modifier = Modifier.size(geometry.artSize + 8.dp),
                            )
                        else -> Unit
                    }
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        song.title,
                        style = if (style.miniLayout == MiniLayout.Large) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = content,
                        maxLines = 1,
                        // I titoli lunghi scorrono invece di essere tagliati
                        modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE, initialDelayMillis = 2000),
                    )
                    if (style.miniShowArtist) {
                        Text(
                            song.artists.joinToString { it.name },
                            style = MaterialTheme.typography.bodyMedium,
                            color = content.copy(alpha = 0.7f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                if (style.miniButtons == MiniButtons.All) {
                    IconButton(onClick = connection::seekToPrevious) {
                        AtxIcon(Res.drawable.skip_previous, "Brano precedente", size = 26.dp, tint = content)
                    }
                }
                IconButton(onClick = actions::togglePlayPause) {
                    AtxIcon(if (isPlaying) Res.drawable.pause else Res.drawable.play, if (isPlaying) "Pausa" else "Riproduci", size = 30.dp, tint = content)
                }
                if (style.miniButtons != MiniButtons.PlayOnly) {
                    IconButton(onClick = connection::seekToNext) {
                        AtxIcon(Res.drawable.skip_next, "Brano successivo", size = 28.dp, tint = content)
                    }
                }
            }
            // Onda: piccola onda sotto le informazioni del brano (Fuji)
            if (wave) {
                LinearWavyProgressIndicator(
                    progress = { progressState.fraction() },
                    color = progressColor,
                    trackColor = content.copy(alpha = 0.15f),
                    amplitude = { if (isPlaying) 1f else 0f },
                    modifier = Modifier.padding(start = 18.dp, end = 18.dp, bottom = 8.dp).fillMaxWidth().height(6.dp),
                )
            }
        }
        if (style.miniProgress == MiniProgress.Line) {
            Box(
                Modifier
                    .align(Alignment.BottomStart)
                    .padding(horizontal = if (style.miniLayout == MiniLayout.Pill) 30.dp else 22.dp)
                    .fillMaxWidth()
                    .height(3.dp)
                    .drawBehind {
                        drawRoundRect(progressColor, size = Size(size.width * progressState.fraction(), size.height), cornerRadius = CornerRadius(4f, 4f))
                    },
            )
        }
    }
}

/** Bordo arrotondato che si "chiude" man mano che il brano va avanti. */
@Composable
private fun ProgressFrame(
    progress: () -> Float,
    color: Color,
    track: Color,
    radius: Dp,
    modifier: Modifier,
) {
    val path = remember { Path() }
    val piece = remember { Path() }
    val measure = remember { PathMeasure() }
    Canvas(modifier) {
        val stroke = 3.dp.toPx()
        val r = radius.toPx().coerceAtMost(size.minDimension / 2f)
        path.reset()
        path.addRoundRect(RoundRect(stroke / 2, stroke / 2, size.width - stroke / 2, size.height - stroke / 2, CornerRadius(r, r)))
        drawPath(path, track, style = Stroke(stroke))
        measure.setPath(path, false)
        piece.reset()
        measure.getSegment(0f, measure.length * progress().coerceIn(0f, 1f), piece, true)
        drawPath(piece, color, style = Stroke(stroke, cap = StrokeCap.Round))
    }
}

// endregion

// region Player a schermo intero (stili di Fuji)

/** Posizione e dimensione della copertina nel player. */
private data class ArtFrame(
    val x: Dp,
    val y: Dp,
    val width: Dp,
    val height: Dp,
    val radius: Dp,
)

private fun lerpFrame(
    a: ArtFrame,
    b: ArtFrame,
    t: Float,
) = ArtFrame(lerp(a.x, b.x, t), lerp(a.y, b.y, t), lerp(a.width, b.width, t), lerp(a.height, b.height, t), lerp(a.radius, b.radius, t))

@Composable
fun PlayerScreen() {
    val actions = LocalActions.current
    val navigator = LocalNavigator.current
    val connection = actions.player
    val metadata by connection.mediaMetadata.collectAsState()
    val currentSong by connection.currentSong.collectAsState(null)
    val isPlaying by connection.isPlaying.collectAsState()
    val shuffle by connection.shuffleModeEnabled.collectAsState()
    val repeatMode by connection.repeatMode.collectAsState()
    val windows by connection.queueWindows.collectAsState()
    val currentIndex by connection.currentWindowIndex.collectAsState()
    val queueTitle by connection.queueTitle.collectAsState()
    val progress = rememberProgressState(connection)
    val song = metadata ?: return
    val style = LocalAtxStyle.current
    val layout = style.playerLayout
    // Il Poster ha il suo sfondo fisso (colore della copertina con il bagliore)
    val playerBackground = if (layout == PlayerLayout.Poster) PlayerBackground.Glow else style.playerBackground
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val haptics = LocalHapticFeedback.current
    var showMenu by remember { mutableStateOf(false) }
    fun close() {
        navigator.lyricsVisible = false
        navigator.playerExpanded = false
    }

    // Titolo senza "(feat. ...)", "- Remastered" ecc.: le aggiunte diventano etichette
    val (cleanTitle, tags) = remember(song.title) { splitTitle(song.title) }
    val liked = currentSong?.song?.liked == true

    // 0 = copertina grande, 1 = testi (la copertina diventa piccola in alto)
    val showLyrics = navigator.lyricsVisible
    val lyricsSpring by animateFloatAsState(
        if (showLyrics) 1f else 0f,
        spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow),
        label = "lyrics",
    )
    val tLyrics = lyricsSpring.coerceIn(0f, 1f)

    // ================= Colori =================
    val artColors = rememberArtworkColors(song.thumbnailUrl)
    val artworkAccent = animatedArtworkColor(artColors)
    val themeBackground = MaterialTheme.colorScheme.background
    val dark = themeBackground.luminance() < 0.5f
    val seed = if (style.coloredPlayerControls) artworkAccent else MaterialTheme.colorScheme.primary
    val baseColors = remember(seed, dark, layout) { playerColors(seed, dark, poster = layout == PlayerLayout.Poster) }
    val pc =
        if (playerBackground == PlayerBackground.Plain) {
            baseColors.copy(background = themeBackground, text = MaterialTheme.colorScheme.onBackground)
        } else {
            baseColors
        }

    // ================= Animazioni =================
    val breath by rememberInfiniteTransition(label = "alive").animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "breath",
    )
    // 1 quando suona, 0 in pausa
    val life by animateFloatAsState(if (isPlaying) 1f else 0f, tween(700), label = "life")
    // Vinile: quanto esce da dietro la copertina (rimbalza un po')
    val discOut by animateFloatAsState(
        if (isPlaying) 1f else 0.12f,
        spring(Spring.DampingRatioLowBouncy, Spring.StiffnessLow),
        label = "discOut",
    )
    val artScale by animateFloatAsState(
        if (isPlaying || !style.shrinkArtworkOnPause) 1f else 0.86f,
        spring(dampingRatio = 0.55f, stiffness = 300f),
        label = "artScale",
    )
    // Il disco / le bobine girano solo mentre la musica suona
    var spinAngle by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(isPlaying, layout) {
        if (!isPlaying || (layout != PlayerLayout.Vinyl && layout != PlayerLayout.Cassette)) return@LaunchedEffect
        val speed = if (layout == PlayerLayout.Cassette) 180f else 72f
        var last = withFrameNanos { it }
        while (true) {
            withFrameNanos { now ->
                spinAngle = (spinAngle + (now - last) / 1_000_000_000f * speed) % 360f
                last = now
            }
        }
    }

    // ================= Swipe sulla copertina =================
    // Pila: la copertina vecchia vola via mentre quella nuova è già davanti
    var ghostUrl by remember { mutableStateOf<String?>(null) }
    var ghostX by remember { mutableFloatStateOf(0f) }
    val screenWidthPx = with(density) { 420.dp.toPx() }
    val swipe =
        rememberTrackSwipe(
            onNext = { from ->
                if (layout != PlayerLayout.Stack) return@rememberTrackSwipe false
                ghostUrl = song.thumbnailUrl
                ghostX = from
                scope.launch {
                    animate(from, -screenWidthPx * 1.4f, animationSpec = tween(380, easing = FastOutLinearInEasing)) { v, _ -> ghostX = v }
                    ghostUrl = null
                }
                true
            },
        ) { forward -> if (forward) connection.seekToNext() else connection.player.seekToPreviousMediaItem() }
    val swipeX = swipe.offset.value

    // ================= Trascina giù per chiudere =================
    var dragY by remember { mutableFloatStateOf(0f) }
    var dragJob by remember { mutableStateOf<Job?>(null) }
    val closeThreshold = with(density) { 140.dp.toPx() }
    val dragState = rememberDraggableState { delta -> dragY = (dragY + delta).coerceAtLeast(0f) }

    // Prossimi brani per la Pila
    val upcoming =
        remember(windows, currentIndex, repeatMode) {
            val list = windows.mapNotNull { it.mediaItem.metadata?.thumbnailUrl }
            val after = list.drop(currentIndex + 1).toMutableList()
            if (repeatMode == Player.REPEAT_MODE_ALL) after += list
            after.take(2)
        }
    val nextTitle =
        remember(windows, currentIndex, repeatMode) {
            (windows.getOrNull(currentIndex + 1) ?: if (repeatMode == Player.REPEAT_MODE_ALL) windows.firstOrNull() else null)
                ?.mediaItem
                ?.metadata
                ?.title
        }

    val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    // Dove inizia il blocco del titolo (misurato): la copertina si centra nello spazio sopra
    var textTop by remember { mutableStateOf<Dp?>(null) }
    var rootY by remember { mutableFloatStateOf(0f) }

    // Surface: blocca i tocchi verso le schermate sotto il player
    Surface(
        Modifier
            .fillMaxSize()
            .graphicsLayer { translationY = dragY }
            .draggable(
                state = dragState,
                orientation = Orientation.Vertical,
                onDragStarted = { dragJob?.cancel() },
                onDragStopped = { velocity ->
                    if (dragY > closeThreshold || velocity > 1500f) {
                        close()
                    } else {
                        dragJob = scope.launch { animate(dragY, 0f, animationSpec = spring(dampingRatio = 0.8f)) { v, _ -> dragY = v } }
                    }
                },
            ),
        color = pc.background,
        shape = RoundedCornerShape(if (dragY > 0f) 28.dp else 0.dp),
    ) {
        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .onGloballyPositioned { rootY = it.positionInRoot().y },
        ) {
            val screenHeight = maxHeight
            val width = maxWidth

            // ================= Sfondi =================
            when (playerBackground) {
                PlayerBackground.Glow ->
                    Box(
                        Modifier.fillMaxSize().drawBehind {
                            // Bagliore colorato in basso che pulsa mentre suona (Fuji)
                            val intensity = 0.15f + 0.2f * life * breath
                            drawRect(
                                Brush.radialGradient(
                                    colors = listOf(pc.accent.copy(alpha = intensity), Color.Transparent),
                                    center = Offset(size.width / 2f, size.height * 0.95f),
                                    radius = size.width * 1.1f,
                                ),
                            )
                        },
                    )
                PlayerBackground.Gradient ->
                    Box(
                        Modifier.fillMaxSize().background(
                            Brush.verticalGradient(
                                listOf(
                                    pc.accent.tint(pc.background, 0.35f),
                                    (artColors?.gradient?.getOrNull(1) ?: pc.accent).tint(pc.background, 0.18f),
                                    pc.background,
                                ),
                            ),
                        ),
                    )
                PlayerBackground.Blur -> {
                    Artwork(song.thumbnailUrl, Modifier.fillMaxSize().blur(70.dp), RoundedCornerShape(0.dp), sizePx = 300)
                    Box(Modifier.fillMaxSize().background(pc.background.copy(alpha = 0.62f)))
                }
                PlayerBackground.Plain -> Unit
            }

            // ================= Dove va la copertina =================
            val zoneStart = statusTop + 64.dp
            val zoneEnd = (textTop ?: (screenHeight - 340.dp)) - 20.dp
            val zone = (zoneEnd - zoneStart).coerceAtLeast(120.dp)

            fun centerY(h: Dp) = zoneStart + ((zone - h) / 2).coerceAtLeast(0.dp)
            val full =
                when (layout) {
                    PlayerLayout.Stack -> {
                        // La copertina davanti è centrata; le carte dietro spuntano un po' in alto a destra
                        val side = minOf(width * 0.68f, zone - 40.dp)
                        ArtFrame((width - side) / 2, centerY(side) + 10.dp, side, side, 28.dp)
                    }
                    PlayerLayout.Cassette -> {
                        // La copertina è l'etichetta della cassetta
                        val w = minOf(width - 48.dp, zone / 0.62f)
                        val h = w * 0.62f
                        ArtFrame((width - w) / 2 + 14.dp, centerY(h) + 14.dp, w - 28.dp, h * 0.56f, 8.dp)
                    }
                    // Poster: copertina a tutta larghezza in alto, che sfuma nel colore
                    PlayerLayout.Poster -> ArtFrame(0.dp, 0.dp, width, screenHeight * 0.74f, 0.dp)
                    PlayerLayout.Card -> {
                        val side = minOf(width - 64.dp, zone - 8.dp)
                        ArtFrame((width - side) / 2, centerY(side), side, side, style.corners.radius.coerceAtLeast(16.dp))
                    }
                    PlayerLayout.Vinyl -> {
                        // Copertina + disco che esce a destra: il gruppo intero resta centrato
                        val side = minOf(width * 0.6f, zone - 16.dp)
                        val group = maxOf(side, side * (0.94f + 0.45f * discOut))
                        ArtFrame((width - group) / 2, centerY(side), side, side, 16.dp)
                    }
                }
            // Con i testi aperti la copertina si rimpicciolisce in alto a sinistra
            val small = ArtFrame(24.dp, statusTop + 64.dp, 56.dp, 56.dp, 12.dp)
            val art = lerpFrame(full, small, tLyrics)
            val styleVisible = 1f - tLyrics

            // Vinile: il disco sta dietro la copertina e scivola fuori verso destra
            if (layout == PlayerLayout.Vinyl && tLyrics < 1f) {
                val discSide = art.height * 0.94f
                PlayerDisc(
                    artworkUrl = song.thumbnailUrl,
                    holeColor = pc.background,
                    modifier =
                        Modifier
                            .offset(x = art.x + art.width * 0.45f * discOut, y = art.y + (art.height - discSide) / 2)
                            .size(discSide)
                            .graphicsLayer {
                                alpha = styleVisible
                                rotationZ = spinAngle
                            },
                )
            }

            // Pila: le copertine dei prossimi brani dietro, un po' storte
            if (layout == PlayerLayout.Stack && tLyrics < 1f) {
                val thresholdPx = with(density) { 90.dp.toPx() }
                val advance = (-swipeX / (thresholdPx * 1.5f)).coerceIn(0f, 1f)
                upcoming.indices.reversed().forEach { i ->
                    val level = (i + 1) - advance
                    StackCard(
                        url = upcoming[i],
                        light = 1f - 0.3f * level,
                        shape = RoundedCornerShape(art.radius),
                        modifier =
                            Modifier
                                .offset(x = art.x, y = art.y)
                                .size(art.width, art.height)
                                .graphicsLayer {
                                    alpha = styleVisible
                                    translationX = 17.dp.toPx() * level
                                    translationY = -11.dp.toPx() * level
                                    rotationZ = 4.5f * level
                                    val s = 1f - 0.07f * level
                                    scaleX = s
                                    scaleY = s
                                },
                    )
                }
            }

            // Cassetta: il corpo con le bobine, la copertina fa da etichetta
            if (layout == PlayerLayout.Cassette && tLyrics < 1f) {
                val bodyWidth = art.width + 28.dp
                Cassette(
                    progress = { progress.fraction() },
                    angle = { spinAngle },
                    body = pc.cassette,
                    modifier =
                        Modifier
                            .offset(x = art.x - 14.dp, y = art.y - 14.dp)
                            .size(bodyWidth, bodyWidth * 0.62f)
                            .graphicsLayer {
                                alpha = styleVisible
                                translationX = swipeX
                            },
                )
            }

            // ================= Copertina =================
            val saturation = 1f - 0.4f * (1f - life)
            val artShape = RoundedCornerShape(art.radius.coerceAtLeast(0.dp))
            val shadow =
                when (layout) {
                    PlayerLayout.Cassette, PlayerLayout.Poster -> 0.dp
                    else -> if (tLyrics > 0f) 0.dp else 24.dp
                }
            val tilt =
                when (layout) {
                    PlayerLayout.Stack -> 0.05f
                    PlayerLayout.Cassette -> 0f
                    else -> 0.02f
                }
            val poster = layout == PlayerLayout.Poster
            Box(
                Modifier
                    .offset(x = art.x, y = art.y)
                    .graphicsLayer {
                        // Segue il dito e si inclina un po' durante lo swipe
                        translationX = swipeX
                        rotationZ = swipeX * tilt
                        val s = if (poster || layout == PlayerLayout.Cassette) 1f else (artScale + (1f - artScale) * tLyrics)
                        scaleX = s
                        scaleY = s
                    }.size(art.width, art.height)
                    .shadow(shadow, artShape, ambientColor = pc.accent, spotColor = pc.accent)
                    .clip(artShape)
                    .then(
                        // Poster: la copertina diventa trasparente verso il basso, niente riga di stacco
                        if (poster) {
                            Modifier
                                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                                .drawWithContent {
                                    drawContent()
                                    drawRect(
                                        Brush.verticalGradient(
                                            0f to Color.Black,
                                            0.45f to Color.Black,
                                            0.8f to Color.Black.copy(alpha = 1f - 0.75f * styleVisible),
                                            1f to Color.Black.copy(alpha = 1f - styleVisible),
                                        ),
                                        blendMode = BlendMode.DstIn,
                                    )
                                }
                        } else {
                            Modifier
                        },
                    ).then(swipe.modifier)
                    .clickable(enabled = showLyrics) { navigator.lyricsVisible = false },
            ) {
                Artwork(
                    song.thumbnailUrl,
                    Modifier.fillMaxSize(),
                    RoundedCornerShape(0.dp),
                    sizePx = 1080,
                    colorFilter = if (saturation < 0.999f) ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(saturation) }) else null,
                )
                // Poster: la copertina sfuma nel colore verso il basso (e un po' in alto, per la barra di stato)
                if (poster) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .graphicsLayer { alpha = styleVisible }
                            .background(
                                Brush.verticalGradient(
                                    0f to pc.background.copy(alpha = 0.55f),
                                    0.16f to Color.Transparent,
                                    0.5f to Color.Transparent,
                                    1f to pc.background.copy(alpha = 0.6f),
                                ),
                            ),
                    )
                }
            }

            // Pila: la copertina vecchia che vola via a sinistra
            ghostUrl?.let { url ->
                StackCard(
                    url = url,
                    light = 1f,
                    shape = artShape,
                    modifier =
                        Modifier
                            .offset(x = art.x, y = art.y)
                            .size(art.width, art.height)
                            .graphicsLayer {
                                translationX = ghostX
                                rotationZ = ghostX * 0.05f
                            },
                )
            }

            // ================= Contenuto =================
            Column(
                Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
                    .padding(horizontal = 24.dp),
            ) {
                // Barra in alto
                Row(
                    Modifier.fillMaxWidth().padding(top = 8.dp).height(48.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = ::close) {
                        AtxIcon(Res.drawable.expand_more, "Chiudi player", size = 32.dp, tint = pc.text)
                    }
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(Modifier.width(36.dp).height(4.dp).clip(CircleShape).background(pc.text.copy(alpha = 0.5f)))
                        Spacer(Modifier.height(6.dp))
                        Text(
                            queueTitle?.uppercase() ?: "IN RIPRODUZIONE",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = pc.text.copy(alpha = 0.8f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    IconButton(onClick = { navigator.lyricsVisible = !showLyrics }) {
                        AtxIcon(Res.drawable.lyrics, "Testo", tint = if (showLyrics) pc.accent else pc.text)
                    }
                    IconButton(onClick = { showMenu = true }) {
                        AtxIcon(Res.drawable.more_vert, "Altre opzioni", tint = pc.text)
                    }
                }

                if (showLyrics) {
                    // Copertina piccola (disegnata sopra) + titolo e artista accanto
                    Row(
                        Modifier.fillMaxWidth().padding(top = 8.dp).height(56.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Spacer(Modifier.width(68.dp))
                        Column(Modifier.weight(1f)) {
                            TitleWithBadge(song.title, song.explicit, MaterialTheme.typography.titleMedium, pc, maxLines = 1)
                            Text(
                                song.artists.joinToString { it.name },
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = pc.accent,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        LikeButton(liked, pc) { connection.toggleLike() }
                    }
                    // I testi compaiono a metà animazione, così la copertina si muove leggera
                    Box(
                        Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .graphicsLayer { alpha = ((tLyrics - 0.4f) / 0.6f).coerceIn(0f, 1f) },
                    ) {
                        if (tLyrics > 0.4f) PlayerLyrics(textColor = pc.text, modifier = Modifier.fillMaxSize())
                    }
                } else {
                    if (poster) {
                        // Poster: il titolo gigante riempie lo spazio sopra la sfumatura
                        PosterTitle(
                            title = cleanTitle,
                            tags = tags,
                            pc = pc,
                            pulse = { 1f + 0.025f * life * breath },
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                        )
                        Spacer(Modifier.height(12.dp))
                    } else {
                        // Titolo, barra e controlli restano uniti in basso; la copertina si centra nello spazio sopra
                        Spacer(Modifier.weight(1f))
                        // Segnaposto invisibile: misura dove inizia il titolo
                        Spacer(
                            Modifier.onGloballyPositioned { c ->
                                val y = with(density) { (c.positionInRoot().y - rootY).toDp() }
                                if (textTop == null || abs((textTop!! - y).value) > 1f) textTop = y
                            },
                        )
                        if (tags.isNotEmpty()) {
                            TagRow(tags, background = pc.accent.copy(alpha = 0.18f), text = pc.accent)
                            Spacer(Modifier.height(6.dp))
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            if (!poster) TitleWithBadge(cleanTitle, song.explicit, MaterialTheme.typography.headlineMedium, pc, maxLines = 2)
                            Text(
                                song.artists.joinToString { it.name },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = pc.accent,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier =
                                    Modifier.clickable(enabled = song.artists.firstOrNull()?.id != null) {
                                        song.artists.firstOrNull()?.id?.let {
                                            close()
                                            navigator.open(Route.Artist(it))
                                        }
                                    },
                            )
                            song.album?.let { album ->
                                Text(
                                    album.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = pc.text.copy(alpha = 0.6f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier =
                                        Modifier.clickable {
                                            close()
                                            navigator.open(Route.Album(album.id))
                                        },
                                )
                            }
                        }
                        LikeButton(liked, pc) { connection.toggleLike() }
                    }
                }

                Spacer(Modifier.height(16.dp))
                ProgressBar(
                    slider = style.slider,
                    progress = progress,
                    isPlaying = isPlaying,
                    color = pc.accent,
                    onSeek = { connection.player.seekTo(it) },
                )
                Times(progress, pc.text.copy(alpha = 0.7f), style.showRemainingTime)

                Spacer(Modifier.height(20.dp))
                if (style.playButton == PlayButtonShape.Group) {
                    ConnectedControls(
                        isPlaying = isPlaying,
                        accent = pc.accent,
                        onAccent = pc.onAccent,
                        onPress = { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove) },
                        onPrevious = connection::seekToPrevious,
                        onPlay = actions::togglePlayPause,
                        onNext = connection::seekToNext,
                    )
                } else {
                    ClassicControls(style.playButton, isPlaying, pc, onPrevious = connection::seekToPrevious, onPlay = actions::togglePlayPause, onNext = connection::seekToNext)
                }

                Spacer(Modifier.height(16.dp))
                // Casuale, coda e ripeti come pillole con il nome
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    PlayerPill(Res.drawable.shuffle, "Casuale", shuffle, pc) {
                        connection.player.shuffleModeEnabled = !shuffle
                    }
                    PlayerPill(
                        Res.drawable.queue_music,
                        nextTitle?.let { "Dopo: $it" } ?: "Coda",
                        false,
                        pc,
                        Modifier.weight(1f),
                    ) { navigator.queueVisible = true }
                    PlayerPill(
                        if (repeatMode == Player.REPEAT_MODE_ONE) Res.drawable.repeat_one else Res.drawable.repeat,
                        if (repeatMode == Player.REPEAT_MODE_ONE) "Ripeti 1" else "Ripeti",
                        repeatMode != Player.REPEAT_MODE_OFF,
                        pc,
                    ) {
                        connection.player.repeatMode =
                            when (repeatMode) {
                                Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                                Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                                else -> Player.REPEAT_MODE_OFF
                            }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }

    if (showMenu) SongMenuSheet(song, onDismiss = { showMenu = false })
}

@Composable
private fun TitleWithBadge(
    title: String,
    explicit: Boolean,
    textStyle: TextStyle,
    pc: PlayerColors,
    maxLines: Int,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            title,
            style = textStyle,
            fontWeight = FontWeight.Black,
            color = pc.text,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
        if (explicit) {
            Spacer(Modifier.width(8.dp))
            ExplicitBadge(if (maxLines > 1) 20.dp else 14.dp, pc.text.copy(alpha = 0.85f), pc.background)
        }
    }
}

/** Simbolo "E" per i brani con contenuto esplicito. */
@Composable
private fun ExplicitBadge(
    size: Dp,
    background: Color,
    text: Color,
) {
    Box(Modifier.size(size).clip(RoundedCornerShape(size * 0.25f)).background(background), contentAlignment = Alignment.Center) {
        Text(
            "E",
            color = text,
            style = TextStyle(fontSize = (size.value * 0.62f).sp, lineHeight = (size.value * 0.62f).sp, fontWeight = FontWeight.Black),
        )
    }
}

@Composable
private fun LikeButton(
    liked: Boolean,
    pc: PlayerColors,
    onClick: () -> Unit,
) {
    IconButton(onClick = onClick) {
        AtxIcon(
            if (liked) Res.drawable.favorite else Res.drawable.favorite_border,
            if (liked) "Togli dai preferiti" else "Aggiungi ai preferiti",
            size = 28.dp,
            tint = if (liked) pc.accent else pc.text,
        )
    }
}

@Composable
private fun Times(
    progress: ProgressState,
    color: Color,
    remaining: Boolean,
) {
    val position = progress.position.longValue
    val duration = progress.duration.longValue
    Row(Modifier.fillMaxWidth()) {
        Text(formatTime(position), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = color)
        Spacer(Modifier.weight(1f))
        Text(
            when {
                duration <= 0 -> "--:--"
                remaining -> "-" + formatTime(duration - position)
                else -> formatTime(duration)
            },
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = color,
        )
    }
}

/** Barra di avanzamento (3 stili) con uno slider invisibile sopra per trascinare. */
@Composable
private fun ProgressBar(
    slider: SliderStyleChoice,
    progress: ProgressState,
    isPlaying: Boolean,
    color: Color,
    onSeek: (Long) -> Unit,
) {
    var dragging by remember { mutableStateOf<Float?>(null) }
    val current = dragging ?: progress.fraction()
    Box(Modifier.fillMaxWidth().height(40.dp), contentAlignment = Alignment.Center) {
        when (slider) {
            SliderStyleChoice.Wavy ->
                LinearWavyProgressIndicator(
                    progress = { dragging ?: progress.fraction() },
                    color = color,
                    trackColor = color.copy(alpha = 0.25f),
                    amplitude = { if (isPlaying) 1f else 0f },
                    modifier = Modifier.fillMaxWidth(),
                )
            SliderStyleChoice.Classic ->
                LinearProgressIndicator(
                    progress = { dragging ?: progress.fraction() },
                    color = color,
                    trackColor = color.copy(alpha = 0.25f),
                    modifier = Modifier.fillMaxWidth(),
                )
            SliderStyleChoice.Thick -> {
                // Barra spessa che si ingrossa mentre la trascini
                val height by animateDpAsState(if (dragging != null) 16.dp else 10.dp, spring(Spring.DampingRatioMediumBouncy), label = "barHeight")
                Box(Modifier.fillMaxWidth().height(height).clip(CircleShape).background(color.copy(alpha = 0.25f))) {
                    Box(Modifier.fillMaxWidth(current).fillMaxHeight().clip(CircleShape).background(color))
                }
            }
        }
        Slider(
            value = current,
            onValueChange = { dragging = it },
            onValueChangeFinished = {
                dragging?.let { onSeek((it * progress.duration.longValue).toLong()) }
                dragging = null
            },
            colors =
                SliderDefaults.colors(
                    thumbColor = if (dragging != null && slider != SliderStyleChoice.Thick) color else Color.Transparent,
                    activeTrackColor = Color.Transparent,
                    inactiveTrackColor = Color.Transparent,
                    activeTickColor = Color.Transparent,
                    inactiveTickColor = Color.Transparent,
                ),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/**
 * Precedente / play / successivo "incollati" in un unico gruppo (Expressive):
 * quando ne premi uno si allarga e gli altri si stringono.
 */
@Composable
private fun ConnectedControls(
    isPlaying: Boolean,
    accent: Color,
    onAccent: Color,
    onPress: () -> Unit,
    onPrevious: () -> Unit,
    onPlay: () -> Unit,
    onNext: () -> Unit,
) {
    val prevSource = remember { MutableInteractionSource() }
    val playSource = remember { MutableInteractionSource() }
    val nextSource = remember { MutableInteractionSource() }
    val prevPressed by prevSource.collectIsPressedAsState()
    val playPressed by playSource.collectIsPressedAsState()
    val nextPressed by nextSource.collectIsPressedAsState()
    LaunchedEffect(prevPressed, playPressed, nextPressed) {
        if (prevPressed || playPressed || nextPressed) onPress()
    }

    val bounce = spring<Float>(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium)
    val anyPressed = prevPressed || playPressed || nextPressed
    val prevWeight by animateFloatAsState(
        when {
            prevPressed -> 1.4f
            anyPressed -> 0.8f
            else -> 1f
        },
        bounce,
        label = "prevWeight",
    )
    val playWeight by animateFloatAsState(
        when {
            playPressed -> 1.8f
            anyPressed -> 1.3f
            else -> 1.55f
        },
        bounce,
        label = "playWeight",
    )
    val nextWeight by animateFloatAsState(
        when {
            nextPressed -> 1.4f
            anyPressed -> 0.8f
            else -> 1f
        },
        bounce,
        label = "nextWeight",
    )
    val bounceDp = spring<Dp>(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium)
    // Gli angoli interni si arrotondano quando premi il tasto
    val prevInner by animateDpAsState(if (prevPressed) 30.dp else 10.dp, bounceDp, label = "prevInner")
    val nextInner by animateDpAsState(if (nextPressed) 30.dp else 10.dp, bounceDp, label = "nextInner")
    val playRadius by animateDpAsState(if (isPlaying) 28.dp else 44.dp, spring(Spring.DampingRatioMediumBouncy), label = "playRadius")
    val sideColor = accent.copy(alpha = 0.16f)

    Row(
        Modifier.fillMaxWidth().height(88.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GroupPiece(
            Res.drawable.skip_previous,
            "Brano precedente",
            RoundedCornerShape(topStart = 38.dp, bottomStart = 38.dp, topEnd = prevInner, bottomEnd = prevInner),
            sideColor,
            accent,
            36.dp,
            prevSource,
            onPrevious,
            Modifier.weight(prevWeight.coerceAtLeast(0.3f)).height(76.dp),
        )
        GroupPiece(
            if (isPlaying) Res.drawable.pause else Res.drawable.play,
            if (isPlaying) "Pausa" else "Riproduci",
            RoundedCornerShape(playRadius.coerceAtLeast(0.dp)),
            accent,
            onAccent,
            48.dp,
            playSource,
            onPlay,
            Modifier.weight(playWeight.coerceAtLeast(0.3f)).fillMaxHeight(),
        )
        GroupPiece(
            Res.drawable.skip_next,
            "Brano successivo",
            RoundedCornerShape(topStart = nextInner, bottomStart = nextInner, topEnd = 38.dp, bottomEnd = 38.dp),
            sideColor,
            accent,
            36.dp,
            nextSource,
            onNext,
            Modifier.weight(nextWeight.coerceAtLeast(0.3f)).height(76.dp),
        )
    }
}

@Composable
private fun GroupPiece(
    icon: DrawableResource,
    description: String,
    shape: Shape,
    color: Color,
    iconColor: Color,
    iconSize: Dp,
    source: MutableInteractionSource,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    Surface(onClick = onClick, interactionSource = source, shape = shape, color = color, modifier = modifier) {
        Box(contentAlignment = Alignment.Center) {
            AtxIcon(icon, description, size = iconSize, tint = iconColor)
        }
    }
}

/** Comandi classici: precedente, Play con la forma scelta, successivo. */
@Composable
private fun ClassicControls(
    shape: PlayButtonShape,
    isPlaying: Boolean,
    pc: PlayerColors,
    onPrevious: () -> Unit,
    onPlay: () -> Unit,
    onNext: () -> Unit,
) {
    val rotation = rememberSpin(12000)
    val spin = shape == PlayButtonShape.CookieSpin && isPlaying
    val playShape: Shape =
        when (shape) {
            PlayButtonShape.CookieSpin, PlayButtonShape.Cookie -> MaterialShapes.Cookie9Sided.toShape()
            PlayButtonShape.Squircle -> RoundedCornerShape(30.dp)
            else -> CircleShape
        }
    Row(
        Modifier.fillMaxWidth().height(88.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onPrevious, modifier = Modifier.size(64.dp)) {
            AtxIcon(Res.drawable.skip_previous, "Brano precedente", size = 40.dp, tint = pc.text)
        }
        Box(
            Modifier
                .size(88.dp)
                .rotate(if (spin) rotation else 0f)
                .clip(playShape)
                .background(pc.accent)
                .clickable(onClick = onPlay),
            contentAlignment = Alignment.Center,
        ) {
            AtxIcon(
                if (isPlaying) Res.drawable.pause else Res.drawable.play,
                if (isPlaying) "Pausa" else "Riproduci",
                size = 40.dp,
                tint = pc.onAccent,
                modifier = Modifier.rotate(if (spin) -rotation else 0f),
            )
        }
        IconButton(onClick = onNext, modifier = Modifier.size(64.dp)) {
            AtxIcon(Res.drawable.skip_next, "Brano successivo", size = 40.dp, tint = pc.text)
        }
    }
}

/** Pillola con icona e nome (Casuale / Coda / Ripeti). */
@Composable
private fun PlayerPill(
    icon: DrawableResource,
    text: String,
    active: Boolean,
    pc: PlayerColors,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val background by animateColorAsState(if (active) pc.accent.copy(alpha = 0.28f) else pc.text.copy(alpha = 0.08f), label = "pillBg")
    val content by animateColorAsState(if (active) pc.accent else pc.text.copy(alpha = 0.85f), label = "pillContent")
    Row(
        modifier
            .clip(CircleShape)
            .background(background)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        AtxIcon(icon, null, size = 18.dp, tint = content)
        Spacer(Modifier.width(6.dp))
        Text(text, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = content, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

// region Vinile, Pila e Cassetta

@Composable
private fun PlayerDisc(
    artworkUrl: String?,
    holeColor: Color,
    modifier: Modifier,
) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val r = size.minDimension / 2f
            drawCircle(Color(0xFF111111), radius = r)
            // Solchi
            for (i in 0..7) {
                drawCircle(Color.White.copy(alpha = 0.05f + 0.01f * (i % 2)), radius = r * (0.42f + 0.07f * i), style = Stroke(width = 1.dp.toPx()))
            }
            // Riflesso
            drawCircle(Brush.linearGradient(listOf(Color.White.copy(alpha = 0.10f), Color.Transparent, Color.White.copy(alpha = 0.06f))), radius = r)
        }
        // Etichetta centrale con la copertina
        Artwork(artworkUrl, Modifier.fillMaxSize(0.36f), CircleShape, sizePx = 400)
        Box(Modifier.size(8.dp).clip(CircleShape).background(holeColor))
    }
}

@Composable
private fun StackCard(
    url: String?,
    light: Float,
    shape: Shape,
    modifier: Modifier,
) {
    val l = light.coerceIn(0f, 1f)
    Box(modifier.shadow(16.dp, shape).clip(shape).background(Color.DarkGray)) {
        Artwork(
            url,
            Modifier.fillMaxSize(),
            RoundedCornerShape(0.dp),
            sizePx = 600,
            colorFilter =
                if (l < 1f) {
                    ColorFilter.colorMatrix(
                        ColorMatrix(
                            floatArrayOf(
                                l, 0f, 0f, 0f, 0f,
                                0f, l, 0f, 0f, 0f,
                                0f, 0f, l, 0f, 0f,
                                0f, 0f, 0f, 1f, 0f,
                            ),
                        ),
                    )
                } else {
                    null
                },
        )
    }
}

@Composable
private fun Cassette(
    progress: () -> Float,
    angle: () -> Float,
    body: Color,
    modifier: Modifier,
) {
    // Tutti i colori della cassetta nascono dal colore della copertina
    val dark = lerpColor(body, Color.Black, 0.78f)
    val tape = lerpColor(body, Color.Black, 0.55f)
    val reel = lerpColor(body, Color.White, 0.85f)
    val screws = lerpColor(body, Color.Black, 0.35f)
    Canvas(modifier.shadow(22.dp, RoundedCornerShape(16.dp))) {
        val r = 16.dp.toPx()
        drawRoundRect(body, cornerRadius = CornerRadius(r, r))
        drawRoundRect(Color.Black.copy(alpha = 0.08f), cornerRadius = CornerRadius(r, r), style = Stroke(width = 3.dp.toPx()))
        // Finestrella con le bobine
        val fw = size.width * 0.56f
        val fh = size.height * 0.24f
        val fx = (size.width - fw) / 2
        val fy = size.height * 0.67f
        val window = Path().apply { addRoundRect(RoundRect(fx, fy, fx + fw, fy + fh, CornerRadius(fh / 2, fh / 2))) }
        drawRoundRect(dark, Offset(fx, fy), Size(fw, fh), CornerRadius(fh / 2, fh / 2))
        val cy = fy + fh / 2
        val cxLeft = fx + fh / 2 + 4.dp.toPx()
        val cxRight = fx + fw - fh / 2 - 4.dp.toPx()
        clipPath(window) {
            // Il nastro passa da sinistra a destra man mano che il brano va avanti
            val maxR = fh * 0.62f
            val minR = fh * 0.40f
            val p = progress()
            drawCircle(tape, radius = maxR - (maxR - minR) * p, center = Offset(cxLeft, cy))
            drawCircle(tape, radius = minR + (maxR - minR) * p, center = Offset(cxRight, cy))
        }
        // Bobine che girano
        listOf(cxLeft, cxRight).forEach { cx ->
            val rb = fh * 0.34f
            drawCircle(reel, radius = rb, center = Offset(cx, cy))
            drawCircle(dark, radius = rb * 0.42f, center = Offset(cx, cy))
            rotate(angle(), pivot = Offset(cx, cy)) {
                repeat(3) { k ->
                    rotate(k * 120f, pivot = Offset(cx, cy)) {
                        drawRoundRect(
                            dark,
                            topLeft = Offset(cx - rb * 0.11f, cy - rb * 0.95f),
                            size = Size(rb * 0.22f, rb * 0.42f),
                            cornerRadius = CornerRadius(rb * 0.1f, rb * 0.1f),
                        )
                    }
                }
            }
        }
        // Viti
        val v = 3.5.dp.toPx()
        val m = 10.dp.toPx()
        listOf(Offset(m, size.height - m), Offset(size.width - m, size.height - m), Offset(size.width / 2, size.height - m * 0.8f))
            .forEach { drawCircle(screws, radius = v, center = it) }
    }
}

// endregion

// region Titolo pulito e Poster

// Parole che indicano un'aggiunta al titolo (parole intere, non pezzi: "Alive" non è "live")
private val addedWords =
    Regex(
        """\b(feat|ft|featuring|with|remaster|remastered|version|versione|live|edit|remix|mix|deluxe|acoustic|demo|mono|stereo|bonus|radio|extended|instrumental|sped up|slowed|reprise|from|anniversary|unplugged|session|sessions|prod|explicit|clean|soundtrack|motion picture|ost|cover|official|video|audio|lyrics|visualizer)\b""",
        RegexOption.IGNORE_CASE,
    )

private fun isAddition(text: String) = addedWords.containsMatchIn(text)

/** "Piove (feat. Sfera Ebbasta) - Remastered 2011" → "Piove" + ["FEAT. SFERA EBBASTA", "REMASTERED 2011"] */
private fun splitTitle(title: String): Pair<String, List<String>> {
    val tags = mutableListOf<String>()
    var rest =
        Regex("""\s*[(\[]([^)\]]+)[)\]]""").replace(title) { m ->
            val inside = m.groupValues[1].trim()
            if (isAddition(inside)) {
                tags += inside.uppercase()
                ""
            } else {
                m.value
            }
        }
    Regex("""\s+[-–—]\s+(.+)$""").find(rest)?.let { m ->
        val after = m.groupValues[1].trim()
        if (isAddition(after)) {
            tags += after.uppercase()
            rest = rest.substring(0, m.range.first)
        }
    }
    rest = rest.trim()
    return if (rest.isEmpty()) title to emptyList() else rest to tags
}

@Composable
private fun TagRow(
    tags: List<String>,
    background: Color,
    text: Color,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        tags.take(2).forEach { tag ->
            Text(
                tag,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false).clip(CircleShape).background(background).padding(horizontal = 10.dp, vertical = 4.dp),
            )
        }
    }
}

// Come impaginare il titolo: tutto grande, oppure prime parole grandi e il resto più piccolo sotto
private data class PosterLayout(
    val big: String,
    val small: String?,
    val bigSize: Float,
    val smallSize: Float,
    val maxLines: Int,
)

private const val MAX_POSTER = 96f
private const val MIN_POSTER = 56f // sotto questa grandezza non sembra più un poster

@Composable
private fun PosterTitle(
    title: String,
    tags: List<String>,
    pc: PlayerColors,
    pulse: () -> Float,
    modifier: Modifier,
) {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val words = remember(title) { title.uppercase().split(" ").filter { it.isNotBlank() } }

    fun posterStyle(size: Float) =
        TextStyle(
            fontSize = size.sp,
            lineHeight = (size * 0.9f).sp,
            fontWeight = FontWeight.Black,
            letterSpacing = (-0.03).em,
            lineBreak = LineBreak.Heading,
            hyphens = Hyphens.None,
        )

    BoxWithConstraints(modifier, contentAlignment = Alignment.BottomStart) {
        val widthPx = constraints.maxWidth
        val tagSpace = if (tags.isNotEmpty()) with(density) { 36.dp.toPx() } else 0f
        val heightPx = (constraints.maxHeight - tagSpace).coerceAtLeast(1f)

        val plan =
            remember(words, widthPx, heightPx) {
                fun measure(
                    text: String,
                    size: Float,
                ) = measurer.measure(text, style = posterStyle(size), constraints = Constraints(maxWidth = widthPx))
                val sizes = generateSequence(MAX_POSTER) { it - 4f }.takeWhile { it >= MIN_POSTER }.toList()
                val all = words.joinToString(" ")
                // 1) Tutto il titolo grande, massimo 3 righe
                for (s in sizes) {
                    val m = measure(all, s)
                    if (m.lineCount <= 3 && m.size.height <= heightPx) return@remember PosterLayout(all, null, s, 0f, 3)
                }
                // 2) Prime parole grandi (massimo 2 righe), il resto sotto a circa metà grandezza
                if (words.size > 1) {
                    for (s in sizes) {
                        for (k in words.size - 1 downTo 1) {
                            val big = words.take(k).joinToString(" ")
                            val small = words.drop(k).joinToString(" ")
                            val mb = measure(big, s)
                            if (mb.lineCount > 2) continue
                            val smallSize = (s * 0.45f).coerceAtLeast(26f)
                            val ms = measure(small, smallSize)
                            val gap = with(density) { 6.dp.toPx() }
                            val maxSmall = with(density) { (smallSize * 0.9f * 3).sp.toPx() }
                            if (mb.size.height + gap + min(ms.size.height.toFloat(), maxSmall) <= heightPx) {
                                return@remember PosterLayout(big, small, s, smallSize, 2)
                            }
                        }
                    }
                }
                // 3) Caso estremo: grandezza minima, massimo 4 righe con "…"
                PosterLayout(all, null, MIN_POSTER, 0f, 4)
            }

        Column(
            Modifier.fillMaxWidth().graphicsLayer {
                val s = pulse()
                scaleX = s
                scaleY = s
                transformOrigin = TransformOrigin(0f, 1f)
            },
        ) {
            if (tags.isNotEmpty()) {
                TagRow(tags, background = pc.accent, text = pc.onAccent)
                Spacer(Modifier.height(10.dp))
            }
            if (plan.small == null) {
                // Tutto grande: seconda metà delle parole nel secondo colore
                val parts = plan.big.split(" ")
                val half = ((parts.size + 1) / 2).coerceAtLeast(1)
                val text =
                    buildAnnotatedString {
                        append(parts.take(half).joinToString(" "))
                        if (parts.size > half) {
                            append(" ")
                            withStyle(SpanStyle(color = pc.posterSecond)) { append(parts.drop(half).joinToString(" ")) }
                        }
                    }
                Text(text, style = posterStyle(plan.bigSize), color = pc.text, maxLines = plan.maxLines, overflow = TextOverflow.Ellipsis)
            } else {
                Text(plan.big, style = posterStyle(plan.bigSize), color = pc.text, maxLines = plan.maxLines)
                Spacer(Modifier.height(6.dp))
                Text(plan.small, style = posterStyle(plan.smallSize), color = pc.posterSecond, maxLines = 3, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

// endregion

// region Colori del player

private data class PlayerColors(
    val background: Color,
    val text: Color,
    val accent: Color,
    val onAccent: Color,
    val posterSecond: Color,
    val cassette: Color,
)

/** Stessa tinta del colore di partenza, con luminosità e saturazione scelte da noi. */
private fun tone(
    seed: Color,
    lightness: Float,
    maxSaturation: Float,
): Color {
    val r = seed.red
    val g = seed.green
    val b = seed.blue
    val maxC = max(r, max(g, b))
    val minC = min(r, min(g, b))
    val l = (maxC + minC) / 2f
    var h = 0f
    var s = 0f
    if (maxC != minC) {
        val d = maxC - minC
        s = if (l > 0.5f) d / (2f - maxC - minC) else d / (maxC + minC)
        h =
            when (maxC) {
                r -> (g - b) / d + (if (g < b) 6f else 0f)
                g -> (b - r) / d + 2f
                else -> (r - g) / d + 4f
            } * 60f
    }
    return Color.hsl(h.coerceIn(0f, 359.99f), s.coerceAtMost(maxSaturation).coerceIn(0f, 1f), lightness)
}

private fun contrastOn(c: Color) = if (c.luminance() > 0.45f) Color(0xFF141216) else Color.White

/**
 * Tutto nasce da un solo colore (copertina o app): sfondo, testi e tasti sono sue sfumature.
 * Tema scuro: sfondo scuro colorato e testi chiari. Tema chiaro: sfondo pastello e testi scuri.
 */
private fun playerColors(
    seed: Color,
    dark: Boolean,
    poster: Boolean,
): PlayerColors {
    val background: Color
    val accent: Color
    val text: Color
    if (dark) {
        background = if (poster) tone(seed, 0.30f, 0.75f) else tone(seed, 0.17f, 0.55f)
        accent = if (poster) tone(seed, 0.82f, 0.9f) else tone(seed, 0.68f, 0.9f)
        text = Color.White
    } else {
        background = if (poster) tone(seed, 0.74f, 0.8f) else tone(seed, 0.90f, 0.7f)
        accent = if (poster) tone(seed, 0.26f, 0.85f) else tone(seed, 0.42f, 0.85f)
        text = tone(seed, 0.12f, 0.35f)
    }
    return PlayerColors(
        background = background,
        text = text,
        accent = accent,
        onAccent = contrastOn(accent),
        // Seconda metà del titolo nel Poster: più colorata, per staccare dal testo
        posterSecond = if (dark) tone(seed, 0.66f, 0.95f) else tone(seed, 0.34f, 0.9f),
        cassette = if (dark) tone(seed, 0.52f, 0.7f) else tone(seed, 0.66f, 0.7f),
    )
}

// endregion

// endregion

/** Coda di riproduzione: tocca un brano per saltarci. */
@Composable
fun QueueSheet() {
    val actions = LocalActions.current
    val navigator = LocalNavigator.current
    val connection = actions.player
    val windows by connection.queueWindows.collectAsState()
    val currentIndex by connection.currentWindowIndex.collectAsState()
    val sheetState = rememberModalBottomSheetState()
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = currentIndex.coerceAtLeast(0))

    ModalBottomSheet(onDismissRequest = { navigator.queueVisible = false }, sheetState = sheetState) {
        Text(
            "In coda",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(start = 20.dp, bottom = 8.dp),
        )
        LazyColumn(state = listState, modifier = Modifier.fillMaxWidth()) {
            itemsIndexed(windows) { index, window ->
                val item = window.mediaItem.metadata ?: return@itemsIndexed
                SongRow(
                    songId = item.id,
                    title = item.title,
                    subtitle = item.artists.joinToString { it.name },
                    artworkUrl = item.thumbnailUrl,
                    isCurrent = index == currentIndex,
                    onClick = {
                        connection.player.seekToDefaultPosition(window.firstPeriodIndex)
                        connection.player.prepare()
                        connection.player.playWhenReady = true
                    },
                )
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}
