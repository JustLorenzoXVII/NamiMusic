@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.justlorenzo.archivetunex.ui.screens

import com.justlorenzo.archivetunex.ui.tr
import com.justlorenzo.archivetunex.ui.trf
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.wrapContentSize
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
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
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
import androidx.compose.ui.layout.boundsInRoot
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
import archivetunex.shared.generated.resources.all_inclusive
import archivetunex.shared.generated.resources.airplay
import archivetunex.shared.generated.resources.cast
import archivetunex.shared.generated.resources.drag_handle
import archivetunex.shared.generated.resources.close
import archivetunex.shared.generated.resources.clear_all
import androidx.compose.ui.input.pointer.pointerInput
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
import com.justlorenzo.archivetunex.ui.components.scrollingText
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
fun MiniPlayer(
    modifier: Modifier = Modifier,
    interactive: Boolean = true,
) {
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

    if (style.miniLayout == MiniLayout.Joined) {
        JoinedMini(modifier, interactive)
        return
    }
    val geometry =
        when (style.miniLayout) {
            MiniLayout.Joined, MiniLayout.Floating -> MiniGeometry(RoundedCornerShape(26.dp), 0.dp, 68.dp, 48.dp)
            MiniLayout.Pill -> MiniGeometry(RoundedCornerShape(50), 0.dp, 58.dp, 42.dp)
            MiniLayout.Large -> MiniGeometry(RoundedCornerShape(30.dp), 0.dp, 92.dp, 72.dp)
        }
    val wave = style.miniProgress == MiniProgress.Wave
    val spin = rememberSpin(8000)

    Box(
        modifier
            .fillMaxWidth()
            .padding(horizontal = geometry.outerPadding)
            .then(if (interactive) Modifier.shadow(14.dp, geometry.shape) else Modifier)
            .clip(geometry.shape)
            .background(background)
            .then(if (interactive) swipe.modifier.dragToOpenPlayer().clickable { navigator.playerExpanded = true } else Modifier),
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
                    if (!interactive) {
                        // Durante l'apertura la copertina la disegna il player (vola verso il suo posto)
                    } else if (style.miniArtwork == MiniArtwork.Vinyl) {
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
                        modifier =
                            Modifier.scrollingText(),
                    )
                    if (style.miniShowArtist) {
                        Text(
                            song.artists.joinToString { it.name },
                            style = MaterialTheme.typography.bodyMedium,
                            color = content.copy(alpha = 0.7f),
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.scrollingText(),
                        )
                    }
                }
                if (style.miniButtons == MiniButtons.All) {
                    IconButton(onClick = connection::seekToPrevious) {
                        AtxIcon(Res.drawable.skip_previous, tr("Brano precedente"), size = 26.dp, tint = content)
                    }
                }
                IconButton(onClick = actions::togglePlayPause) {
                    AtxIcon(if (isPlaying) Res.drawable.pause else Res.drawable.play, if (isPlaying) tr("Pausa") else tr("Riproduci"), size = 30.dp, tint = content)
                }
                if (style.miniButtons != MiniButtons.PlayOnly) {
                    IconButton(onClick = connection::seekToNext) {
                        AtxIcon(Res.drawable.skip_next, tr("Brano successivo"), size = 28.dp, tint = content)
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
fun PlayerScreen(underArtwork: @Composable () -> Unit = {}) {
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
    // Tema iOS: il player prende sempre i colori della copertina, su sfondo scuro sfocato (come Apple Music)
    val playerBackground = if (style.liquidGlass) PlayerBackground.Blur else style.playerBackground
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
    val dark = style.liquidGlass || themeBackground.luminance() < 0.5f
    val seed = if (style.liquidGlass || style.coloredPlayerControls) artworkAccent else MaterialTheme.colorScheme.primary
    val baseColors = remember(seed, dark, layout) { playerColors(seed, dark, poster = false) }
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
        if (!isPlaying || (layout != PlayerLayout.Vinyl && layout != PlayerLayout.Cassette && layout != PlayerLayout.Wave)) return@LaunchedEffect
        val speed =
            when (layout) {
                PlayerLayout.Cassette -> 180f
                PlayerLayout.Wave -> 90f
                else -> 72f
            }
        var last = withFrameNanos { it }
        while (true) {
            withFrameNanos { now ->
                spinAngle = (spinAngle + (now - last) / 1_000_000_000f * speed) % 360f
                last = now
            }
        }
    }

    // ================= Swipe sulla copertina =================
    val swipe =
        rememberTrackSwipe { forward -> if (forward) connection.seekToNext() else connection.player.seekToPreviousMediaItem() }
    val swipeX = swipe.offset.value

    // Mosaico: la copertina toccata vola dal suo riquadro al posto di quella grande
    var flyer by remember { mutableStateOf<Pair<ArtFrame, String>?>(null) }
    val flyProgress = remember { androidx.compose.animation.core.Animatable(0f) }

    // Prossimi brani per il Mosaico: copertina e posizione in coda (toccandoli si salta lì)
    val upcoming =
        remember(windows, currentIndex) {
            windows
                .mapIndexedNotNull { index, window -> window.mediaItem.metadata?.thumbnailUrl?.let { index to it } }
                .filter { it.first > currentIndex }
                .take(4)
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
    // La misura resta nel PlayerTransition: non si perde chiudendo il player, così l'apertura
    // successiva va già dritta al punto giusto
    val playerTransition = LocalPlayerTransition.current
    val textTop = playerTransition.textTop
    // Coordinate del contenuto *prima* dello spostamento animato: la misura non dipende dall'apertura
    val measureCoords = remember { arrayOfNulls<androidx.compose.ui.layout.LayoutCoordinates>(2) }
    val density0 = LocalDensity.current

    /** Calcola la posizione del titolo appena sono pronti sia il contenuto [0] sia il segnaposto [1]. */
    fun measureTextTop() {
        val parent = measureCoords[0]?.takeIf { it.isAttached } ?: return
        val title = measureCoords[1]?.takeIf { it.isAttached } ?: return
        val y = with(density0) { parent.localPositionOf(title, Offset.Zero).y.toDp() }
        val current = playerTransition.textTop
        if (current == null || abs((current - y).value) > 1f) playerTransition.textTop = y
    }

    // Surface: blocca i tocchi verso le schermate sotto il player
    // 0 = chiuso nel mini player, 1 = aperto (lo guida l'animazione o il dito)
    val open = LocalPlayerTransition.current.progress.value.coerceIn(0f, 1f)

    Surface(Modifier.fillMaxSize(), color = pc.background) {
        BoxWithConstraints(
            Modifier
                .fillMaxSize(),
        ) {
            val screenHeight = maxHeight
            val width = maxWidth

            // ================= Sfondi =================
            Box(Modifier.fillMaxSize().graphicsLayer { alpha = open }) {
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
            }

            // Durante l'apertura/chiusura: il mini player (titolo e tasti) sfuma qui, SOTTO la copertina,
            // così la copertina che vola resta sempre visibile
            underArtwork()

            // ================= Dove va la copertina =================
            val zoneStart = statusTop + 64.dp
            // Se cambia (es. compaiono i tag) la copertina ci scivola, senza scatti
            val measuredTextTop by animateDpAsState(
                textTop ?: (screenHeight - 340.dp),
                spring(dampingRatio = 0.9f, stiffness = Spring.StiffnessMediumLow),
                label = "textTop",
            )
            val zoneEnd = measuredTextTop - 20.dp
            val zone = (zoneEnd - zoneStart).coerceAtLeast(120.dp)

            fun centerY(h: Dp) = zoneStart + ((zone - h) / 2).coerceAtLeast(0.dp)
            val full =
                when (layout) {
                    PlayerLayout.Mosaic -> {
                        // Copertina grande a sinistra; a destra e sotto le copertine dei prossimi brani
                        val grid = minOf(width - 48.dp, zone - 8.dp)
                        val big = grid * 0.68f
                        val total = big + 8.dp + (grid - big - 8.dp)
                        ArtFrame((width - grid) / 2, centerY(total), big, big, 22.dp)
                    }
                    PlayerLayout.Ticket -> {
                        // Biglietto: la copertina è la parte alta, sotto la parte da strappare
                        val w = minOf(width - 64.dp, zone / 1.3f)
                        val h = w * 1.3f
                        ArtFrame((width - w) / 2 + 14.dp, centerY(h) + 14.dp, w - 28.dp, w - 28.dp, 8.dp)
                    }
                    // Onda: copertina a tutta larghezza in alto che finisce con un bordo ondulato
                    PlayerLayout.Wave -> ArtFrame(0.dp, 0.dp, width, measuredTextTop - 24.dp, 0.dp)
                    PlayerLayout.Cassette -> {
                        // La copertina è l'etichetta della cassetta
                        val w = minOf(width - 48.dp, zone / 0.62f)
                        val h = w * 0.62f
                        ArtFrame((width - w) / 2 + 14.dp, centerY(h) + 14.dp, w - 28.dp, h * 0.56f, 8.dp)
                    }
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
            // La copertina parte dal mini player e vola al suo posto mentre il player si apre
            val art = lerpFrame(miniArtFrame(style), lerpFrame(full, small, tLyrics), open)
            val styleVisible = (1f - tLyrics) * ((open - 0.3f) / 0.7f).coerceIn(0f, 1f)

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

            // Mosaico: le copertine dei prossimi brani a destra e sotto quella grande (toccale per saltarci)
            if (layout == PlayerLayout.Mosaic && tLyrics < 1f) {
                val gap = 8.dp
                val grid = art.width / 0.68f
                val side = grid - art.width - gap
                val tiles =
                    listOf(
                        ArtFrame(art.x + art.width + gap, art.y, side, (art.height - gap) / 2, 16.dp),
                        ArtFrame(art.x + art.width + gap, art.y + (art.height + gap) / 2, side, (art.height - gap) / 2, 16.dp),
                        ArtFrame(art.x, art.y + art.height + gap, (grid - gap) / 2, side, 16.dp),
                        ArtFrame(art.x + (grid + gap) / 2, art.y + art.height + gap, (grid - gap) / 2, side, 16.dp),
                    )
                // In pausa tutto il mosaico si rimpicciolisce insieme, verso il suo centro
                val groupScale = artScale + (1f - artScale) * tLyrics
                val centerX = art.x + grid / 2
                val centerY = art.y + grid / 2
                tiles.forEachIndexed { i, tile ->
                    val next = upcoming.getOrNull(i)
                    Box(
                        Modifier
                            .offset(tile.x, tile.y)
                            .size(tile.width, tile.height)
                            .graphicsLayer {
                                // Compaiono una dopo l'altra mentre il player si apre
                                alpha = (styleVisible * 1.4f - i * 0.12f).coerceIn(0f, 1f) * if (next == null) 0.35f else 0.9f
                                scaleX = groupScale
                                scaleY = groupScale
                                translationX = ((tile.x + tile.width / 2) - centerX).toPx() * (groupScale - 1f)
                                translationY = ((tile.y + tile.height / 2) - centerY).toPx() * (groupScale - 1f)
                            }.clip(RoundedCornerShape(tile.radius))
                            .background(pc.text.copy(alpha = 0.08f))
                            .clickable(enabled = next != null) {
                                next?.let { (index, url) ->
                                    // La copertina vola al posto di quella grande mentre parte il brano
                                    flyer = tile to url
                                    scope.launch {
                                        flyProgress.snapTo(0f)
                                        flyProgress.animateTo(1f, spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow))
                                        flyer = null
                                    }
                                    connection.player.seekToDefaultPosition(index)
                                    connection.player.playWhenReady = true
                                }
                            },
                    ) {
                        if (next != null) Artwork(next.second, Modifier.fillMaxSize(), RoundedCornerShape(0.dp), sizePx = 400)
                    }
                }
            }

            // Biglietto: il cartoncino del concerto, con la parte da strappare e il brano stampato
            if (layout == PlayerLayout.Ticket && tLyrics < 1f) {
                val ticketW = art.width + 28.dp
                TicketBody(
                    album = song.album?.title,
                    durationMs = progress.duration.longValue,
                    holeColor = pc.background,
                    artSide = art.width,
                    modifier =
                        Modifier
                            .offset(x = art.x - 14.dp, y = art.y - 14.dp)
                            .size(ticketW, ticketW * 1.3f)
                            .graphicsLayer {
                                alpha = styleVisible
                                translationX = swipeX
                            },
                )
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
                    PlayerLayout.Cassette, PlayerLayout.Ticket, PlayerLayout.Wave -> 0.dp
                    // L'ombra cresce insieme all'apertura (prima compariva di colpo a metà volo)
                    else -> 24.dp * (((open - 0.4f) / 0.6f).coerceIn(0f, 1f) * (1f - tLyrics))
                }
            val tilt =
                when (layout) {
                    PlayerLayout.Cassette, PlayerLayout.Ticket, PlayerLayout.Wave -> 0f
                    else -> 0.02f
                }
            val poster = layout == PlayerLayout.Wave
            Box(
                Modifier
                    .offset(x = art.x, y = art.y)
                    .graphicsLayer {
                        // Segue il dito e si inclina un po' durante lo swipe
                        translationX = swipeX
                        rotationZ = swipeX * tilt
                        val s = if (poster || layout == PlayerLayout.Cassette || layout == PlayerLayout.Vinyl || layout == PlayerLayout.Ticket) 1f else (artScale + (1f - artScale) * tLyrics)
                        scaleX = s
                        scaleY = s
                        // Mentre la copertina toccata vola qui, quella sotto resta nascosta
                        alpha = if (flyer != null) 0f else 1f
                        if (layout == PlayerLayout.Mosaic) {
                            // Si rimpicciolisce verso il centro del mosaico, insieme alle altre copertine
                            val grid = art.width / 0.68f
                            translationX += (art.width / 2 - grid / 2).toPx() * (s - 1f)
                            translationY = (art.height / 2 - grid / 2).toPx() * (s - 1f)
                        }
                    }.size(art.width, art.height)
                    .shadow(shadow, artShape, ambientColor = pc.accent, spotColor = pc.accent)
                    .then(
                        if (poster) {
                            // Onda: il bordo in basso ondeggia mentre suona (l'ampiezza cresce con l'apertura)
                            Modifier.graphicsLayer {
                                clip = true
                                shape = WaveBottomShape(amplitude = 12.dp.toPx() * styleVisible, phase = spinAngle / 360f * 2f * kotlin.math.PI.toFloat())
                            }
                        } else {
                            Modifier.clip(artShape)
                        },
                    ).then(swipe.modifier)
                    .clickable(enabled = showLyrics) { navigator.lyricsVisible = false },
            ) {
                Artwork(
                    song.thumbnailUrl,
                    Modifier.fillMaxSize(),
                    RoundedCornerShape(0.dp),
                    sizePx = 1080,
                    previewSizePx = 226,
                    colorFilter = if (saturation < 0.999f) ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(saturation) }) else null,
                )
                // Onda: un velo in alto per leggere la barra di stato e i tasti
                if (poster) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .graphicsLayer { alpha = styleVisible }
                            .background(
                                Brush.verticalGradient(
                                    0f to pc.background.copy(alpha = 0.75f),
                                    0.2f to Color.Transparent,
                                ),
                            ),
                    )
                }
            }

            // Mosaico: la copertina toccata che vola al posto grande
            flyer?.let { (from, url) ->
                val frame = lerpFrame(from, art, flyProgress.value)
                Artwork(
                    url,
                    Modifier
                        .offset(frame.x, frame.y)
                        .size(frame.width, frame.height)
                        .shadow(18.dp * flyProgress.value, RoundedCornerShape(frame.radius)),
                    RoundedCornerShape(frame.radius),
                    sizePx = 1080,
                )
            }

            // ================= Contenuto =================
            Column(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        // Titolo e comandi salgono e compaiono mentre il player si apre (come Fuji)
                        alpha = ((open - 0.35f) / 0.65f).coerceIn(0f, 1f)
                        translationY = (1f - open) * 80.dp.toPx()
                    }.onGloballyPositioned {
                        measureCoords[0] = it
                        measureTextTop()
                    }
                    .systemBarsPadding()
                    .padding(horizontal = 24.dp),
            ) {
                // Barra in alto
                Row(
                    Modifier.fillMaxWidth().padding(top = 8.dp).height(48.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Nell'Onda la barra sta sopra la copertina: icone e titolo su "bolle" leggibili
                    val chip = if (poster && !showLyrics) pc.background.copy(alpha = 0.55f) else Color.Transparent
                    TopBarButton(chip, ::close) {
                        AtxIcon(Res.drawable.expand_more, tr("Chiudi player"), size = 30.dp, tint = pc.text)
                    }
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Text(
                            tr("IN RIPRODUZIONE"),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = pc.text.copy(alpha = 0.9f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier =
                                Modifier
                                    .padding(horizontal = 8.dp)
                                    .clip(CircleShape)
                                    .background(chip)
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                        )
                    }
                    TopBarButton(chip, { showMenu = true }) {
                        AtxIcon(Res.drawable.more_vert, tr("Altre opzioni"), size = 22.dp, tint = pc.text)
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
                                softWrap = false,
                                modifier = Modifier.scrollingText(),
                            )
                        }
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
                    run {
                        // Titolo, barra e controlli restano uniti in basso; la copertina si centra nello spazio sopra
                        Spacer(Modifier.weight(1f))
                        // Segnaposto invisibile: misura dove inizia il titolo
                        Spacer(
                            Modifier.onGloballyPositioned { c ->
                                // Posizione rispetto al contenuto non spostato: uguale in ogni istante dell'animazione
                                measureCoords[1] = c
                                measureTextTop()
                            },
                        )
                        if (tags.isNotEmpty()) {
                            TagRow(tags, background = pc.accent.copy(alpha = 0.18f), text = pc.accent)
                            Spacer(Modifier.height(6.dp))
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            TitleWithBadge(cleanTitle, song.explicit, MaterialTheme.typography.headlineMedium, pc, maxLines = 1)
                            Text(
                                song.artists.joinToString { it.name },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = pc.accent,
                                maxLines = 1,
                                softWrap = false,
                                modifier =
                                    Modifier.scrollingText().clickable(enabled = song.artists.firstOrNull()?.id != null) {
                                        song.artists.firstOrNull()?.id?.let {
                                            close()
                                            navigator.open(Route.Artist(it))
                                        }
                                    },
                            )
                        }
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

                Spacer(Modifier.height(18.dp))
                // Testi, Trasmetti, Preferiti e Coda (come Apple Music): icone semplici distanziate
                val isAndroid = remember { com.justlorenzo.archivetunex.getPlatform().name.startsWith("Android") }
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BottomAction(Res.drawable.lyrics, tr("Testo"), showLyrics, pc) { navigator.lyricsVisible = !showLyrics }
                    BottomAction(if (isAndroid) Res.drawable.cast else Res.drawable.airplay, tr("Trasmetti"), false, pc) {
                        moe.rukamori.archivetune.utils.oem.SystemMediaControlResolver.openMediaOutputSwitcher(moe.rukamori.archivetune.compat.AppContext)
                    }
                    BottomAction(
                        if (liked) Res.drawable.favorite else Res.drawable.favorite_border,
                        if (liked) tr("Togli dai preferiti") else tr("Aggiungi ai preferiti"),
                        liked,
                        pc,
                    ) { connection.toggleLike() }
                    BottomAction(Res.drawable.queue_music, tr("Coda"), false, pc) { navigator.queueVisible = true }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }

    if (showMenu) SongMenuSheet(song, onDismiss = { showMenu = false })
}

/** Pulsante della coda (Casuale, Ripeti, Automatica): pieno quando è attivo. */
@Composable
private fun QueueToggle(
    icon: DrawableResource,
    label: String,
    active: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val container by animateColorAsState(
        if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest,
        label = "queueToggle",
    )
    val content = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    Row(
        modifier
            .height(44.dp)
            .clip(CircleShape)
            .background(container)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AtxIcon(icon, null, size = 18.dp, tint = content)
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = content, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Icona della fila in basso (Testi, Trasmetti, Preferiti, Coda): colorata quando è attiva. */
@Composable
private fun BottomAction(
    icon: DrawableResource,
    description: String,
    active: Boolean,
    pc: PlayerColors,
    onClick: () -> Unit,
) {
    val tint by animateColorAsState(if (active) pc.accent else pc.text.copy(alpha = 0.7f), label = "bottomActionTint")
    IconButton(onClick = onClick, modifier = Modifier.size(52.dp)) {
        AtxIcon(icon, description, size = 26.dp, tint = tint)
    }
}

/** Pulsante tondo della barra in alto, con uno sfondo semitrasparente quando serve. */
@Composable
private fun TopBarButton(
    background: Color,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Box(
        Modifier.size(42.dp).clip(CircleShape).background(background).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { content() }
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
        // Sempre una riga sola, stessa grandezza per tutti i titoli: se è lungo scorre
        Text(
            title,
            style = textStyle,
            fontWeight = FontWeight.Black,
            color = pc.text,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.weight(1f, fill = false).scrollingText(),
        )
        if (explicit) {
            Spacer(Modifier.width(8.dp))
            ExplicitBadge(if (textStyle.fontSize.value > 20f) 20.dp else 14.dp, pc.text.copy(alpha = 0.85f), pc.background)
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
            tr("Brano precedente"),
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
            if (isPlaying) tr("Pausa") else tr("Riproduci"),
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
            tr("Brano successivo"),
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
            AtxIcon(Res.drawable.skip_previous, tr("Brano precedente"), size = 40.dp, tint = pc.text)
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
                if (isPlaying) tr("Pausa") else tr("Riproduci"),
                size = 40.dp,
                tint = pc.onAccent,
                modifier = Modifier.rotate(if (spin) -rotation else 0f),
            )
        }
        IconButton(onClick = onNext, modifier = Modifier.size(64.dp)) {
            AtxIcon(Res.drawable.skip_next, tr("Brano successivo"), size = 40.dp, tint = pc.text)
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

// region Vinile, Mosaico, Biglietto, Onda e Cassetta

/** Rettangolo con il bordo in basso ondulato: la fase fa scorrere l'onda. */
private class WaveBottomShape(
    private val amplitude: Float,
    private val phase: Float,
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: androidx.compose.ui.unit.LayoutDirection,
        density: androidx.compose.ui.unit.Density,
    ): androidx.compose.ui.graphics.Outline {
        val path = Path()
        val base = size.height - amplitude
        path.moveTo(0f, 0f)
        path.lineTo(size.width, 0f)
        path.lineTo(size.width, base)
        val steps = 48
        for (i in steps downTo 0) {
            val x = size.width * i / steps
            val y = base + amplitude * kotlin.math.sin(x / size.width * 2f * kotlin.math.PI.toFloat() * 2.5f + phase)
            path.lineTo(x, y)
        }
        path.close()
        return androidx.compose.ui.graphics.Outline.Generic(path)
    }
}

/** Il cartoncino del biglietto: fori laterali, linea tratteggiata e il brano stampato sulla parte da strappare. */
@Composable
private fun TicketBody(
    album: String?,
    durationMs: Long,
    holeColor: Color,
    artSide: Dp,
    modifier: Modifier,
) {
    val paper = Color(0xFFFBF3E9)
    val ink = Color(0xFF2B1230)
    Box(modifier.shadow(20.dp, RoundedCornerShape(16.dp)).clip(RoundedCornerShape(16.dp)).background(paper)) {
        // Dove si strappa: due mezzi fori ai lati e la linea tratteggiata
        Canvas(Modifier.fillMaxSize()) {
            val y = 14.dp.toPx() + artSide.toPx() + 14.dp.toPx()
            val r = 10.dp.toPx()
            drawCircle(holeColor, radius = r, center = Offset(0f, y))
            drawCircle(holeColor, radius = r, center = Offset(size.width, y))
            var x = r + 6.dp.toPx()
            while (x < size.width - r - 6.dp.toPx()) {
                drawCircle(holeColor, radius = 2.5.dp.toPx(), center = Offset(x, y))
                x += 12.dp.toPx()
            }
        }
        // Sulla parte da strappare: album, durata e un codice a barre (titolo e artista sono già sotto)
        Row(
            Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(start = 18.dp, end = 18.dp, bottom = 14.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    (album ?: tr("Singolo")).uppercase(),
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = ink,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.scrollingText(),
                )
                Text(
                    (if (durationMs > 0) formatTime(durationMs) + " · " else "") + "ADMIT ONE · NAMI",
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = ink.copy(alpha = 0.6f),
                    maxLines = 1,
                    softWrap = false,
                )
            }
            Spacer(Modifier.width(10.dp))
            // Codice a barre (sempre uguale per lo stesso album)
            val seed = remember(album) { (album ?: "nami").fold(17) { acc, c -> acc * 31 + c.code } }
            Canvas(Modifier.width(54.dp).height(30.dp)) {
                var x = 0f
                var n = seed
                while (x < size.width) {
                    n = n * 1103515245 + 12345
                    val w = (1 + ((n ushr 16) and 3)) * 1.2.dp.toPx()
                    drawRect(ink, topLeft = Offset(x, 0f), size = Size(w, size.height))
                    x += w + (1 + ((n ushr 20) and 1)) * 1.6.dp.toPx()
                }
            }
        }
    }
}

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

// region Titolo pulito

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

// region Barra unita (stile Fuji)

/** Una scheda della barra di navigazione. */
class BarTab(
    val tab: com.justlorenzo.archivetunex.ui.Tab,
    val label: String,
    val icon: DrawableResource,
)

/** Colori della barra unita: le stesse sfumature del player, prese dalla copertina. */
@Composable
private fun rememberBarColors(): PlayerColors {
    val connection = LocalActions.current.player
    val metadata by connection.mediaMetadata.collectAsState()
    val style = LocalAtxStyle.current
    val artColors = rememberArtworkColors(metadata?.thumbnailUrl)
    val accent = animatedArtworkColor(artColors)
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val seed = if (style.coloredPlayerControls && metadata != null) accent else MaterialTheme.colorScheme.primary
    return remember(seed, dark) { playerColors(seed, dark, poster = false) }
}

/**
 * Mini player e barra di navigazione in un unico blocco (come Fuji Music):
 * angoli grandi in alto, stesso colore, schede con l'indicatore a pillola.
 */
@Composable
fun JoinedBottomBar(
    tabs: List<BarTab>,
    hideMini: Boolean,
    onMiniBounds: (androidx.compose.ui.geometry.Rect) -> Unit,
    modifier: Modifier = Modifier,
) {
    val navigator = LocalNavigator.current
    val connection = LocalActions.current.player
    val metadata by connection.mediaMetadata.collectAsState()
    val pc = rememberBarColors()
    val background by animateColorAsState(pc.background, tween(600), label = "barBackground")
    Column(
        modifier
            .fillMaxWidth()
            .shadow(18.dp, RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
            .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
            .background(background),
    ) {
        if (metadata != null) {
            Box(
                Modifier
                    .onGloballyPositioned { onMiniBounds(it.boundsInRoot()) }
                    .graphicsLayer { alpha = if (hideMini) 0f else 1f },
            ) { JoinedMini(Modifier, interactive = true) }
        }
        // Schede fatte a mano: stessa larghezza per tutte, icona sopra e testo sotto, altezza fissa.
        // (La NavigationBar di sistema su iPhone metteva icona e testo affiancati e la quarta scheda usciva dallo schermo.)
        Row(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(72.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            tabs.forEach { item ->
                val selected = navigator.tab == item.tab
                val indicator by animateColorAsState(
                    if (selected) pc.accent.copy(alpha = 0.22f) else Color.Transparent,
                    tween(250),
                    label = "tabIndicator",
                )
                Column(
                    Modifier
                        .weight(1f)
                        // Niente alone su tutta la scheda al tocco: si accende solo la pillola dietro l'icona
                        .clickable(
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                            indication = null,
                        ) { navigator.selectTab(item.tab) }
                        .padding(vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Box(
                        Modifier.size(width = 60.dp, height = 32.dp).clip(RoundedCornerShape(16.dp)).background(indicator),
                        contentAlignment = Alignment.Center,
                    ) {
                        AtxIcon(item.icon, item.label, size = 24.dp, tint = if (selected) pc.accent else pc.text.copy(alpha = 0.7f))
                    }
                    Text(
                        item.label,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        color = if (selected) pc.text else pc.text.copy(alpha = 0.7f),
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

/** Il mini player della barra unita: copertina, titolo, artista con il tempo, Play quadrato e avanti. */
@Composable
private fun JoinedMini(
    modifier: Modifier,
    interactive: Boolean,
) {
    val actions = LocalActions.current
    val navigator = LocalNavigator.current
    val connection = actions.player
    val metadata by connection.mediaMetadata.collectAsState()
    val isPlaying by connection.isPlaying.collectAsState()
    val song = metadata ?: return
    val style = LocalAtxStyle.current
    val progress = rememberProgressState(connection)
    val pc = rememberBarColors()
    val swipe =
        rememberTrackSwipe { forward ->
            if (forward) connection.seekToNext() else connection.player.seekToPreviousMediaItem()
        }
    val spin = rememberSpin(8000)

    Column(
        modifier
            .fillMaxWidth()
            .then(if (interactive) Modifier else Modifier.background(pc.background))
            .drawBehind {
                // Riempimento: un colore appena più chiaro avanza da sinistra a destra
                if (style.miniProgress == MiniProgress.Fill) {
                    drawRect(lerpColor(pc.background, pc.text, 0.09f), size = Size(size.width * progress.fraction(), size.height))
                }
            }.then(if (interactive) swipe.modifier.dragToOpenPlayer().clickable { navigator.playerExpanded = true } else Modifier),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(72.dp)
                .graphicsLayer {
                    translationX = swipe.offset.value
                    alpha = 1f - (abs(swipe.offset.value) / 600f).coerceAtMost(0.6f)
                }.padding(start = 8.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(56.dp), contentAlignment = Alignment.Center) {
                if (!interactive) {
                    // Durante l'apertura la copertina la disegna il player (vola verso il suo posto)
                } else if (style.miniArtwork == MiniArtwork.Vinyl) {
                    VinylDisc(song.thumbnailUrl, if (isPlaying) spin else 0f, Modifier.size(48.dp))
                } else {
                    val shape: Shape =
                        when (style.miniArtwork) {
                            MiniArtwork.Circle -> CircleShape
                            MiniArtwork.Cookie -> MaterialShapes.Cookie9Sided.toShape()
                            else -> RoundedCornerShape(12.dp)
                        }
                    Artwork(song.thumbnailUrl, Modifier.size(48.dp), shape, sizePx = 226)
                }
                when (style.miniProgress) {
                    MiniProgress.Frame -> ProgressFrame({ progress.fraction() }, pc.accent, pc.text.copy(alpha = 0.15f), 15.dp, Modifier.size(56.dp))
                    MiniProgress.Ring ->
                        CircularProgressIndicator(
                            progress = { progress.fraction() },
                            modifier = Modifier.size(56.dp),
                            color = pc.accent,
                            strokeWidth = 3.dp,
                            trackColor = Color.Transparent,
                        )
                    else -> Unit
                }
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        song.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = pc.text,
                        maxLines = 1,
                        softWrap = false,
                        modifier =
                            Modifier.scrollingText().weight(1f, fill = false),
                    )
                    if (song.explicit) {
                        Spacer(Modifier.width(6.dp))
                        ExplicitBadge(14.dp, pc.text.copy(alpha = 0.7f), pc.background)
                    }
                }
                if (style.miniShowArtist) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            song.artists.joinToString { it.name },
                            style = MaterialTheme.typography.bodySmall,
                            color = pc.text.copy(alpha = 0.7f),
                            maxLines = 1,
                            softWrap = false,
                            modifier =
                                Modifier.scrollingText().weight(1f, fill = false),
                        )
                        MiniTime(progress, pc.text.copy(alpha = 0.55f))
                    }
                }
            }
            if (style.miniButtons == MiniButtons.All) {
                IconButton(onClick = connection::seekToPrevious) {
                    AtxIcon(Res.drawable.skip_previous, tr("Brano precedente"), tint = pc.accent)
                }
            }
            Box(
                Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(pc.accent)
                    .clickable(onClick = actions::togglePlayPause),
                contentAlignment = Alignment.Center,
            ) {
                AtxIcon(if (isPlaying) Res.drawable.pause else Res.drawable.play, if (isPlaying) tr("Pausa") else tr("Riproduci"), tint = pc.onAccent)
            }
            if (style.miniButtons != MiniButtons.PlayOnly) {
                IconButton(onClick = connection::seekToNext) {
                    AtxIcon(Res.drawable.skip_next, tr("Brano successivo"), tint = pc.accent)
                }
            }
        }
        when (style.miniProgress) {
            // Onda: piccola onda tra le informazioni del brano e la barra
            MiniProgress.Wave ->
                LinearWavyProgressIndicator(
                    progress = { progress.fraction() },
                    color = pc.accent,
                    trackColor = pc.text.copy(alpha = 0.15f),
                    amplitude = { if (isPlaying) 1f else 0f },
                    modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 6.dp).fillMaxWidth().height(8.dp),
                )
            MiniProgress.Line ->
                Box(
                    Modifier.padding(horizontal = 16.dp).padding(bottom = 6.dp).fillMaxWidth().height(3.dp).drawBehind {
                        drawRoundRect(pc.text.copy(alpha = 0.15f), cornerRadius = CornerRadius(4f, 4f))
                        drawRoundRect(pc.accent, size = Size(size.width * progress.fraction(), size.height), cornerRadius = CornerRadius(4f, 4f))
                    },
                )
            else -> Unit
        }
    }
}

/** " · 1:00 / 3:41" accanto all'artista: si aggiorna da solo senza ridisegnare il resto. */
@Composable
private fun MiniTime(
    progress: ProgressState,
    color: Color,
) {
    val duration = progress.duration.longValue
    if (duration <= 0) return
    Text(
        "  ·  ${formatTime(progress.position.longValue)} / ${formatTime(duration)}",
        style = MaterialTheme.typography.bodySmall,
        fontWeight = FontWeight.Medium,
        color = color,
        maxLines = 1,
        softWrap = false,
    )
}

// endregion

/**
 * Stato condiviso dell'apertura del player: 0 = chiuso nel mini player, 1 = aperto.
 * Lo muovono l'animazione (tocco, freccia, indietro) e il dito (trascinando su o giù).
 */
@androidx.compose.runtime.Stable
class PlayerTransition {
    val progress = androidx.compose.animation.core.Animatable(0f)
    var dragging by mutableStateOf(false)
    var heightPx by mutableFloatStateOf(2000f)

    /** Dove inizia il titolo nel player (misurato una volta, poi riusato): serve a centrare la copertina. */
    var textTop by mutableStateOf<Dp?>(null)
    val spec = spring<Float>(dampingRatio = 0.86f, stiffness = Spring.StiffnessMediumLow)

    /** Il dito si è staccato: decide se aprire o chiudere e ci arriva con la stessa velocità del dito. */
    suspend fun settle(
        velocity: Float,
        navigator: com.justlorenzo.archivetunex.ui.AtxNavigator,
    ) {
        dragging = false
        val open =
            when {
                velocity < -900f -> true
                velocity > 900f -> false
                else -> progress.value > 0.5f
            }
        if (!open) navigator.lyricsVisible = false
        navigator.playerExpanded = open
        progress.animateTo(if (open) 1f else 0f, spec, initialVelocity = -velocity / heightPx)
    }
}

val LocalPlayerTransition = androidx.compose.runtime.staticCompositionLocalOf { PlayerTransition() }

/** Trascina in su il mini player per aprire il player: il player segue il dito. */
@Composable
fun Modifier.dragToOpenPlayer(): Modifier {
    val transition = LocalPlayerTransition.current
    val navigator = LocalNavigator.current
    val scope = rememberCoroutineScope()
    val state =
        rememberDraggableState { delta ->
            scope.launch { transition.progress.snapTo((transition.progress.value - delta / transition.heightPx).coerceIn(0f, 1f)) }
        }
    return draggable(
        state = state,
        orientation = Orientation.Vertical,
        onDragStarted = {
            transition.dragging = true
            transition.progress.stop()
        },
        onDragStopped = { velocity -> scope.launch { transition.settle(velocity, navigator) } },
    )
}

/** Dove sta la copertina dentro il mini player, per farla partire (e tornare) esattamente da lì. */
private fun miniArtFrame(style: com.justlorenzo.archivetunex.ui.AtxStyle): ArtFrame =
    when (style.miniLayout) {
        MiniLayout.Joined -> ArtFrame(12.dp, 12.dp, 48.dp, 48.dp, 12.dp)
        MiniLayout.Floating -> ArtFrame(10.dp, 10.dp, 48.dp, 48.dp, 14.dp)
        MiniLayout.Pill -> ArtFrame(8.dp, 8.dp, 42.dp, 42.dp, 21.dp)
        MiniLayout.Large -> ArtFrame(10.dp, 10.dp, 72.dp, 72.dp, 14.dp)
    }.let { if (style.miniArtwork == MiniArtwork.Circle || style.miniArtwork == MiniArtwork.Vinyl) it.copy(radius = it.width / 2) else it }

/**
 * Il player che si apre dal mini player e si richiude dentro di lui (come Fuji): il riquadro si allarga,
 * la copertina vola al suo posto, titolo e comandi salgono e compaiono. Tutto segue anche il dito.
 *
 * [miniBounds] è la posizione del mini player sullo schermo, [miniRadius] il raggio dei suoi angoli.
 */
@Composable
fun PlayerMorph(
    miniBounds: androidx.compose.ui.geometry.Rect?,
    miniRadius: Dp,
    onProgress: (Float) -> Unit,
    topCornersOnly: Boolean = false,
) {
    val navigator = LocalNavigator.current
    val transition = LocalPlayerTransition.current
    val progress = transition.progress
    val scope = rememberCoroutineScope()

    // Tocco, freccia o tasto indietro: anima verso aperto o chiuso (se non lo sta già facendo il dito)
    LaunchedEffect(navigator.playerExpanded) {
        val target = if (navigator.playerExpanded) 1f else 0f
        if (transition.dragging || (progress.isRunning && progress.targetValue == target)) return@LaunchedEffect
        progress.animateTo(target, transition.spec)
        // Fine animazione: esattamente chiuso o aperto (la molla può fermarsi a un soffio dal valore)
        progress.snapTo(target)
    }
    val p = progress.value.coerceIn(0f, 1f)
    androidx.compose.runtime.SideEffect { onProgress(p) }
    // Rimasto aperto di un soffio senza che nessuno lo stia muovendo: lo si chiude del tutto
    LaunchedEffect(p <= 0.01f && !navigator.playerExpanded && !transition.dragging) {
        if (p > 0f && p <= 0.01f && !navigator.playerExpanded && !transition.dragging && !progress.isRunning) progress.snapTo(0f)
    }
    if (p <= 0.01f && !navigator.playerExpanded) return

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val fullW = maxWidth
        val fullH = maxHeight
        transition.heightPx = with(density) { fullH.toPx() }.coerceAtLeast(1f)
        // Se il mini player non è ancora stato misurato si parte dal fondo dello schermo
        val start =
            miniBounds?.let {
                with(density) { ArtFrame(it.left.toDp(), it.top.toDp(), it.width.toDp(), it.height.toDp(), miniRadius) }
            } ?: ArtFrame(12.dp, fullH - 150.dp, fullW - 24.dp, 68.dp, miniRadius)
        val frame = lerpFrame(start, ArtFrame(0.dp, 0.dp, fullW, fullH, 0.dp), p)

        val dragState =
            rememberDraggableState { delta ->
                scope.launch { progress.snapTo((progress.value - delta / transition.heightPx).coerceIn(0f, 1f)) }
            }

        Box(
            Modifier
                .offset(frame.x, frame.y)
                .size(frame.width, frame.height)
                .shadow(if (p < 1f) 18.dp else 0.dp, morphShape(frame.radius, topCornersOnly, p))
                .clip(morphShape(frame.radius, topCornersOnly, p))
                .draggable(
                    state = dragState,
                    orientation = Orientation.Vertical,
                    onDragStarted = {
                        transition.dragging = true
                        progress.stop()
                    },
                    onDragStopped = { velocity -> scope.launch { transition.settle(velocity, navigator) } },
                ),
        ) {
            // Il player è sempre disegnato a schermo intero, ancorato in alto: il riquadro lo "scopre".
            // Copertina, titolo e comandi si muovono da soli in base all'apertura.
            Box(
                Modifier
                    .wrapContentSize(Alignment.TopStart, unbounded = true)
                    .requiredSize(fullW, fullH),
            ) {
                PlayerScreen(
                    underArtwork = {
                        // Le scritte e i tasti del mini player sfumano subito (la copertina vola via da sola)
                        if (p < 0.4f) {
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .height(start.height)
                                    .graphicsLayer { alpha = (1f - p / 0.3f).coerceIn(0f, 1f) },
                            ) { MiniPlayer(Modifier.fillMaxSize(), interactive = false) }
                        }
                    },
                )
            }
        }
    }
}

private fun morphShape(
    radius: Dp,
    topOnly: Boolean,
    p: Float,
) = if (topOnly) {
    RoundedCornerShape(topStart = radius, topEnd = radius)
} else {
    RoundedCornerShape(radius)
}

/**
 * Coda di riproduzione: tocca un brano per saltarci, trascina la maniglia per riordinare,
 * la X per toglierlo, "Svuota" per lasciare solo il brano in riproduzione.
 */
@Composable
fun QueueSheet() {
    val actions = LocalActions.current
    val navigator = LocalNavigator.current
    val connection = actions.player
    val windows by connection.queueWindows.collectAsState()
    val currentIndex by connection.currentWindowIndex.collectAsState()
    val sheetState = rememberModalBottomSheetState()
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = currentIndex.coerceAtLeast(0))
    val density = LocalDensity.current
    val haptics = LocalHapticFeedback.current
    val rowHeight = 68.dp
    val rowPx = with(density) { rowHeight.toPx() }

    // Trascinamento: quale brano, dove si trova il dito e su quale posizione cadrebbe
    var dragFrom by remember { mutableStateOf<Int?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    val dragTarget =
        dragFrom?.let { from -> (from + kotlin.math.round(dragOffset / rowPx).toInt()).coerceIn(0, (windows.size - 1).coerceAtLeast(0)) }

    ModalBottomSheet(onDismissRequest = { navigator.queueVisible = false }, sheetState = sheetState) {
        Row(
            Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(tr("In coda"), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                Text(
                    trf("{0} brani", windows.size),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            // Svuota: resta solo il brano in riproduzione
            if (windows.size > 1) {
                Row(
                    Modifier
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                        .clickable {
                            val player = connection.player
                            val current = player.currentMediaItemIndex
                            if (current + 1 < player.mediaItemCount) player.removeMediaItems(current + 1, player.mediaItemCount)
                            if (current > 0) player.removeMediaItems(0, current)
                        }.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    AtxIcon(Res.drawable.clear_all, null, size = 18.dp, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                    Text(tr("Svuota"), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                }
            }
        }
        // Casuale, Ripeti e Riproduzione automatica (alla fine continua con brani simili)
        val shuffle by connection.shuffleModeEnabled.collectAsState()
        val repeatMode by connection.repeatMode.collectAsState()
        var autoplay by moe.rukamori.archivetune.utils.rememberPreference(moe.rukamori.archivetune.constants.AutoLoadMoreKey, true)
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            QueueToggle(Res.drawable.shuffle, tr("Casuale"), shuffle, Modifier.weight(1f)) {
                connection.player.shuffleModeEnabled = !shuffle
            }
            QueueToggle(
                if (repeatMode == Player.REPEAT_MODE_ONE) Res.drawable.repeat_one else Res.drawable.repeat,
                if (repeatMode == Player.REPEAT_MODE_ONE) tr("Ripeti 1") else tr("Ripeti"),
                repeatMode != Player.REPEAT_MODE_OFF,
                Modifier.weight(1f),
            ) {
                connection.player.repeatMode =
                    when (repeatMode) {
                        Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                        Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                        else -> Player.REPEAT_MODE_OFF
                    }
            }
            QueueToggle(Res.drawable.all_inclusive, tr("Automatica"), autoplay, Modifier.weight(1f)) { autoplay = !autoplay }
        }
        if (autoplay) {
            Text(
                tr("Alla fine della coda continua con brani simili."),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 8.dp),
            )
        }
        LazyColumn(state = listState, modifier = Modifier.fillMaxWidth()) {
            itemsIndexed(windows, key = { index, window -> "${window.uid}-$index" }) { index, window ->
                val item = window.mediaItem.metadata ?: return@itemsIndexed
                val isDragged = dragFrom == index
                // Gli altri brani si spostano per fare posto a quello trascinato
                val shift =
                    when {
                        isDragged || dragFrom == null || dragTarget == null -> 0f
                        dragFrom!! < dragTarget && index in (dragFrom!! + 1)..dragTarget -> -rowPx
                        dragFrom!! > dragTarget && index in dragTarget until dragFrom!! -> rowPx
                        else -> 0f
                    }
                val animatedShift by animateFloatAsState(shift, spring(stiffness = Spring.StiffnessMediumLow), label = "queueShift")
                val isCurrent = index == currentIndex
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(rowHeight)
                        .graphicsLayer {
                            translationY = if (isDragged) dragOffset else animatedShift
                            shadowElevation = if (isDragged) 12.dp.toPx() else 0f
                        }.background(
                            when {
                                isDragged -> MaterialTheme.colorScheme.surfaceContainerHighest
                                isCurrent -> MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                else -> Color.Transparent
                            },
                        ).clickable {
                            connection.player.seekToDefaultPosition(window.firstPeriodIndex)
                            connection.player.prepare()
                            connection.player.playWhenReady = true
                        }.padding(start = 20.dp, end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Artwork(item.thumbnailUrl, Modifier.size(48.dp), RoundedCornerShape(12.dp), sizePx = 160)
                    Column(Modifier.weight(1f)) {
                        Text(
                            item.title,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                            color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            item.artists.joinToString { it.name },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    // Togli dalla coda (non il brano in riproduzione)
                    if (!isCurrent) {
                        IconButton(onClick = { connection.player.removeMediaItem(index) }) {
                            AtxIcon(Res.drawable.close, tr("Togli dalla coda"), size = 20.dp, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    // Maniglia: trascinala per spostare il brano
                    Box(
                        Modifier
                            .size(44.dp)
                            .pointerInput(index, windows.size) {
                                detectDragGestures(
                                    onDragStart = {
                                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                        dragFrom = index
                                        dragOffset = 0f
                                    },
                                    onDrag = { change, amount ->
                                        change.consume()
                                        dragOffset += amount.y
                                    },
                                    onDragEnd = {
                                        val from = dragFrom
                                        val to = dragTarget
                                        if (from != null && to != null && from != to) connection.player.moveMediaItem(from, to)
                                        dragFrom = null
                                        dragOffset = 0f
                                    },
                                    onDragCancel = {
                                        dragFrom = null
                                        dragOffset = 0f
                                    },
                                )
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        AtxIcon(Res.drawable.drag_handle, tr("Sposta"), size = 22.dp, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}
