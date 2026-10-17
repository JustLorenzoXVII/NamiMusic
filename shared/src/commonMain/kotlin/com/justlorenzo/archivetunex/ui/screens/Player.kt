package com.justlorenzo.archivetunex.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import archivetunex.shared.generated.resources.Res
import archivetunex.shared.generated.resources.download
import archivetunex.shared.generated.resources.expand_more
import archivetunex.shared.generated.resources.favorite
import archivetunex.shared.generated.resources.favorite_border
import archivetunex.shared.generated.resources.lyrics
import archivetunex.shared.generated.resources.more_vert
import archivetunex.shared.generated.resources.pause
import archivetunex.shared.generated.resources.play
import archivetunex.shared.generated.resources.queue_music
import archivetunex.shared.generated.resources.repeat
import archivetunex.shared.generated.resources.repeat_on
import archivetunex.shared.generated.resources.repeat_one_on
import archivetunex.shared.generated.resources.shuffle
import archivetunex.shared.generated.resources.shuffle_on
import archivetunex.shared.generated.resources.skip_next
import archivetunex.shared.generated.resources.skip_previous
import com.justlorenzo.archivetunex.ui.AtxStyle
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
import com.justlorenzo.archivetunex.ui.components.DownloadBadge
import com.justlorenzo.archivetunex.ui.components.SongMenuSheet
import com.justlorenzo.archivetunex.ui.components.SongRow
import com.justlorenzo.archivetunex.ui.components.animatedArtworkColor
import com.justlorenzo.archivetunex.ui.components.readableOn
import com.justlorenzo.archivetunex.ui.components.rememberArtworkColors
import com.justlorenzo.archivetunex.ui.components.tint
import kotlinx.coroutines.delay
import moe.rukamori.archivetune.compat.media3.C
import moe.rukamori.archivetune.compat.media3.Download
import moe.rukamori.archivetune.compat.media3.Player
import moe.rukamori.archivetune.extensions.metadata
import moe.rukamori.archivetune.models.MediaMetadata
import moe.rukamori.archivetune.playback.PlayerConnection
import moe.rukamori.archivetune.ui.player.WavySliderExpressive

// region Utilità

/** Posizione e durata del brano, aggiornate più volte al secondo. */
@Composable
private fun rememberProgress(connection: PlayerConnection): Pair<Long, Long> {
    var position by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    LaunchedEffect(connection) {
        while (true) {
            position = connection.player.currentPosition.coerceAtLeast(0L)
            duration = connection.player.duration.takeIf { it != C.TIME_UNSET && it > 0 } ?: 0L
            delay(200)
        }
    }
    return position to duration
}

private fun formatTime(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}

/** Rotazione continua (vinile, Play che gira). */
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

/** Disco in vinile con i solchi e la copertina come etichetta al centro. */
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

// endregion

// region Mini player

private data class MiniGeometry(
    val shape: Shape,
    val outerPadding: Dp,
    val height: Dp,
    val artSize: Dp,
)

/** Mini player: forma, colori, copertina, pulsanti e avanzamento a scelta. */
@Composable
fun MiniPlayer(modifier: Modifier = Modifier) {
    val actions = LocalActions.current
    val navigator = LocalNavigator.current
    val connection = actions.player
    val metadata by connection.mediaMetadata.collectAsState()
    val isPlaying by connection.isPlaying.collectAsState()
    val song = metadata ?: return
    val (position, duration) = rememberProgress(connection)
    val style = LocalAtxStyle.current
    val colors = rememberArtworkColors(song.thumbnailUrl)
    val accent = animatedArtworkColor(colors)
    val progress = if (duration > 0) (position.toFloat() / duration).coerceIn(0f, 1f) else 0f

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
            MiniLayout.Floating -> MiniGeometry(RoundedCornerShape(26.dp), 12.dp, 68.dp, 48.dp)
            MiniLayout.Pill -> MiniGeometry(RoundedCornerShape(50), 28.dp, 58.dp, 42.dp)
            MiniLayout.Docked -> MiniGeometry(RoundedCornerShape(14.dp), 0.dp, 64.dp, 46.dp)
            MiniLayout.Large -> MiniGeometry(RoundedCornerShape(30.dp), 12.dp, 92.dp, 72.dp)
        }
    val spin = rememberSpin(8000)

    Box(
        modifier
            .fillMaxWidth()
            .padding(horizontal = geometry.outerPadding)
            .then(if (style.miniLayout == MiniLayout.Docked) Modifier else Modifier.shadow(14.dp, geometry.shape))
            .clip(geometry.shape)
            .background(background)
            .clickable { navigator.playerExpanded = true },
    ) {
        // Avanzamento "riempimento": lo sfondo si colora da sinistra
        if (style.miniProgress == MiniProgress.Fill) {
            Box(Modifier.height(geometry.height).fillMaxWidth(progress).background(progressColor.copy(alpha = 0.2f)))
        }
        Row(
            Modifier.fillMaxWidth().height(geometry.height).padding(start = 10.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(Modifier.size(geometry.artSize + 6.dp), contentAlignment = Alignment.Center) {
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
                if (style.miniProgress == MiniProgress.Ring) {
                    CircularProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.size(geometry.artSize + 6.dp),
                        color = progressColor,
                        strokeWidth = 3.dp,
                        trackColor = Color.Transparent,
                    )
                }
            }
            Column(Modifier.weight(1f)) {
                Text(
                    song.title,
                    style = if (style.miniLayout == MiniLayout.Large) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = content,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
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
        if (style.miniProgress == MiniProgress.Line) {
            Box(
                Modifier
                    .align(Alignment.BottomStart)
                    .padding(horizontal = if (style.miniLayout == MiniLayout.Pill) 30.dp else 22.dp)
                    .fillMaxWidth(progress)
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(progressColor),
            )
        }
    }
}

// endregion

// region Player a schermo intero

/** Tutto quello che serve alle varie disposizioni del player. */
private class PlayerUi(
    val song: MediaMetadata,
    val style: AtxStyle,
    val accent: Color,
    val artworkAccent: Color,
    val onAccent: Color,
    val isPlaying: Boolean,
    val liked: Boolean,
    val position: Long,
    val duration: Long,
)

@Composable
fun PlayerScreen() {
    val actions = LocalActions.current
    val connection = actions.player
    val metadata by connection.mediaMetadata.collectAsState()
    val currentSong by connection.currentSong.collectAsState(null)
    val isPlaying by connection.isPlaying.collectAsState()
    val (position, duration) = rememberProgress(connection)
    var showMenu by remember { mutableStateOf(false) }
    val song = metadata ?: return
    val style = LocalAtxStyle.current
    val colors = rememberArtworkColors(song.thumbnailUrl)
    val artworkAccent = animatedArtworkColor(colors)
    val accent = if (style.coloredPlayerControls) artworkAccent else MaterialTheme.colorScheme.onSurface
    val background = MaterialTheme.colorScheme.background
    val ui =
        PlayerUi(
            song = song,
            style = style,
            accent = accent,
            artworkAccent = artworkAccent,
            onAccent = accent.readableOn(),
            isPlaying = isPlaying,
            liked = currentSong?.song?.liked == true,
            position = position,
            duration = duration,
        )

    // Surface: blocca i tocchi verso le schermate sotto il player
    Surface(Modifier.fillMaxSize(), color = background) {
        Box(Modifier.fillMaxSize()) {
            when (style.playerBackground) {
                PlayerBackground.Gradient ->
                    Box(
                        Modifier.fillMaxSize().background(
                            Brush.verticalGradient(
                                listOf(
                                    artworkAccent.tint(background, 0.55f),
                                    (colors?.gradient?.getOrNull(1) ?: artworkAccent).tint(background, 0.25f),
                                    background,
                                ),
                            ),
                        ),
                    )
                PlayerBackground.Blur -> {
                    Artwork(song.thumbnailUrl, Modifier.fillMaxSize().blur(70.dp), RoundedCornerShape(0.dp), sizePx = 300)
                    Box(Modifier.fillMaxSize().background(background.copy(alpha = 0.55f)))
                }
                PlayerBackground.Solid -> Box(Modifier.fillMaxSize().background(artworkAccent.tint(background, 0.6f)))
                PlayerBackground.Plain -> Unit
            }

            when (style.playerLayout) {
                PlayerLayout.Classic -> ClassicLayout(ui) { showMenu = true }
                PlayerLayout.Immersive -> ImmersiveLayout(ui) { showMenu = true }
                PlayerLayout.Vinyl -> VinylLayout(ui) { showMenu = true }
                PlayerLayout.Minimal -> MinimalLayout(ui) { showMenu = true }
                PlayerLayout.Card -> CardLayout(ui) { showMenu = true }
            }
        }
    }

    if (showMenu) SongMenuSheet(song, onDismiss = { showMenu = false })
}

@Composable
private fun ClassicLayout(
    ui: PlayerUi,
    onMenu: () -> Unit,
) {
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 24.dp)) {
        PlayerTopBar(onMenu)
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            BigArtwork(ui, minOf(maxWidth, maxHeight - 24.dp))
        }
        TitleRow(ui)
        ProgressSection(ui)
        Controls(ui)
        ActionsRow(ui)
    }
}

/** Copertina a tutta larghezza in alto che sfuma nello sfondo. */
@Composable
private fun ImmersiveLayout(
    ui: PlayerUi,
    onMenu: () -> Unit,
) {
    val background = MaterialTheme.colorScheme.background
    Column(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxWidth().weight(1f)) {
            Artwork(ui.song.thumbnailUrl, Modifier.fillMaxSize(), RoundedCornerShape(0.dp), sizePx = 1080)
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(
                        0f to background.copy(alpha = 0.45f),
                        0.25f to Color.Transparent,
                        0.65f to Color.Transparent,
                        1f to background,
                    ),
                ),
            )
            Box(Modifier.statusBarsPadding().padding(horizontal = 24.dp)) { PlayerTopBar(onMenu) }
        }
        Column(Modifier.navigationBarsPadding().padding(horizontal = 24.dp)) {
            TitleRow(ui, big = true)
            ProgressSection(ui)
            Controls(ui)
            ActionsRow(ui)
        }
    }
}

/** Disco in vinile che gira mentre la musica suona. */
@Composable
private fun VinylLayout(
    ui: PlayerUi,
    onMenu: () -> Unit,
) {
    val rotation = rememberSpin(6000)
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 24.dp)) {
        PlayerTopBar(onMenu)
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            val side = minOf(maxWidth, maxHeight - 24.dp)
            VinylDisc(
                ui.song.thumbnailUrl,
                if (ui.isPlaying) rotation else 0f,
                Modifier.size(side).shadow(28.dp, CircleShape, ambientColor = ui.artworkAccent, spotColor = ui.artworkAccent),
            )
        }
        TitleRow(ui, centered = true)
        ProgressSection(ui)
        Controls(ui)
        ActionsRow(ui)
    }
}

/** Titolo enorme, copertina piccola: per chi vuole poco. */
@Composable
private fun MinimalLayout(
    ui: PlayerUi,
    onMenu: () -> Unit,
) {
    val shape = RoundedCornerShape(ui.style.corners.radius)
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 28.dp)) {
        PlayerTopBar(onMenu)
        Spacer(Modifier.weight(1f))
        Artwork(ui.song.thumbnailUrl, Modifier.size(132.dp).shadow(20.dp, shape), shape, sizePx = 400)
        Spacer(Modifier.height(28.dp))
        Text(
            ui.song.title,
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Black,
            letterSpacing = (-1).sp,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            ui.song.artists.joinToString { it.name },
            style = MaterialTheme.typography.headlineSmall,
            color = ui.accent,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.weight(1f))
        ProgressSection(ui)
        Controls(ui)
        ActionsRow(ui)
    }
}

/** Comandi raccolti in una scheda arrotondata sotto la copertina. */
@Composable
private fun CardLayout(
    ui: PlayerUi,
    onMenu: () -> Unit,
) {
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 16.dp)) {
        Box(Modifier.padding(horizontal = 8.dp)) { PlayerTopBar(onMenu) }
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 8.dp), contentAlignment = Alignment.Center) {
            BigArtwork(ui, minOf(maxWidth, maxHeight - 16.dp))
        }
        Column(
            Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
                .clip(RoundedCornerShape(36.dp))
                .background(MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.92f))
                .padding(horizontal = 18.dp, vertical = 14.dp),
        ) {
            TitleRow(ui)
            ProgressSection(ui)
            Controls(ui)
            ActionsRow(ui, pill = false)
        }
    }
}

@Composable
private fun PlayerTopBar(onMenu: () -> Unit) {
    val navigator = LocalNavigator.current
    val queueTitle by LocalActions.current.player.queueTitle.collectAsState()
    Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { navigator.playerExpanded = false }) {
            AtxIcon(Res.drawable.expand_more, "Chiudi player", size = 32.dp)
        }
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "IN RIPRODUZIONE",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            queueTitle?.let {
                Text(it, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        IconButton(onClick = onMenu) { AtxIcon(Res.drawable.more_vert, "Altre opzioni") }
    }
}

@Composable
private fun BigArtwork(
    ui: PlayerUi,
    side: Dp,
) {
    val scale by animateFloatAsState(
        if (ui.isPlaying || !ui.style.shrinkArtworkOnPause) 1f else 0.84f,
        spring(dampingRatio = 0.55f, stiffness = 300f),
        label = "artwork",
    )
    val shape = RoundedCornerShape(ui.style.corners.radius)
    Artwork(
        ui.song.thumbnailUrl,
        Modifier.size(side).scale(scale).shadow(32.dp, shape, ambientColor = ui.artworkAccent, spotColor = ui.artworkAccent),
        shape = shape,
        sizePx = 1080,
    )
}

@Composable
private fun TitleRow(
    ui: PlayerUi,
    big: Boolean = false,
    centered: Boolean = false,
) {
    val navigator = LocalNavigator.current
    val connection = LocalActions.current.player
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
        Column(Modifier.weight(1f), horizontalAlignment = if (centered) Alignment.CenterHorizontally else Alignment.Start) {
            Text(
                ui.song.title,
                style = if (big) MaterialTheme.typography.headlineLarge else MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                letterSpacing = (-0.5).sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = if (centered) TextAlign.Center else TextAlign.Start,
            )
            Text(
                ui.song.artists.joinToString { it.name },
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier =
                    Modifier.clickable(enabled = ui.song.artists.firstOrNull()?.id != null) {
                        ui.song.artists.firstOrNull()?.id?.let { navigator.open(Route.Artist(it)) }
                    },
            )
        }
        IconButton(onClick = connection::toggleLike) {
            AtxIcon(
                if (ui.liked) Res.drawable.favorite else Res.drawable.favorite_border,
                if (ui.liked) "Togli dai preferiti" else "Aggiungi ai preferiti",
                size = 28.dp,
                tint = if (ui.liked) ui.accent else MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun ProgressSection(ui: PlayerUi) {
    val connection = LocalActions.current.player
    var seeking by remember { mutableStateOf(false) }
    var seekValue by remember { mutableFloatStateOf(0f) }
    val onSeek: (Float) -> Unit = {
        seeking = true
        seekValue = it
    }
    val onSeekDone = {
        connection.player.seekTo(seekValue.toLong())
        seeking = false
    }
    val range = 0f..(ui.duration.takeIf { it > 0 } ?: 1L).toFloat()
    Spacer(Modifier.height(10.dp))
    when (ui.style.slider) {
        SliderStyleChoice.Classic ->
            Slider(
                value = if (seeking) seekValue else ui.position.toFloat(),
                onValueChange = onSeek,
                onValueChangeFinished = onSeekDone,
                valueRange = range,
                colors = SliderDefaults.colors(thumbColor = ui.accent, activeTrackColor = ui.accent),
                modifier = Modifier.fillMaxWidth(),
            )
        else ->
            WavySliderExpressive(
                value = { if (seeking) seekValue else ui.position.toFloat() },
                onValueChange = onSeek,
                onValueChangeFinished = onSeekDone,
                valueRange = range,
                activeTrackColor = ui.accent,
                thumbColor = ui.accent,
                inactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f),
                isPlaying = ui.isPlaying,
                strokeWidth = if (ui.style.slider == SliderStyleChoice.Thick) 10.dp else 5.dp,
                waveAmplitudeWhenPlaying = if (ui.style.slider == SliderStyleChoice.Thick) 0.dp else 4.dp,
                modifier = Modifier.fillMaxWidth(),
            )
    }
    Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
        Text(
            formatTime(if (seeking) seekValue.toLong() else ui.position),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            when {
                ui.duration <= 0 -> "--:--"
                ui.style.showRemainingTime -> "-" + formatTime(ui.duration - ui.position)
                else -> formatTime(ui.duration)
            },
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun Controls(ui: PlayerUi) {
    val actions = LocalActions.current
    val connection = actions.player
    val shuffle by connection.shuffleModeEnabled.collectAsState()
    val repeatMode by connection.repeatMode.collectAsState()
    val rotation = rememberSpin(12000)
    val spin = ui.style.playButton == PlayButtonShape.CookieSpin && ui.isPlaying
    val playShape: Shape =
        when (ui.style.playButton) {
            PlayButtonShape.CookieSpin, PlayButtonShape.Cookie -> MaterialShapes.Cookie9Sided.toShape()
            PlayButtonShape.Circle -> CircleShape
            PlayButtonShape.Squircle -> RoundedCornerShape(30.dp)
        }
    Spacer(Modifier.height(8.dp))
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = { connection.player.shuffleModeEnabled = !shuffle }) {
            AtxIcon(
                if (shuffle) Res.drawable.shuffle_on else Res.drawable.shuffle,
                "Riproduzione casuale",
                tint = if (shuffle) ui.accent else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = connection::seekToPrevious, modifier = Modifier.size(64.dp)) {
            AtxIcon(Res.drawable.skip_previous, "Brano precedente", size = 40.dp)
        }
        Box(
            Modifier
                .size(88.dp)
                .rotate(if (spin) rotation else 0f)
                .clip(playShape)
                .background(ui.accent)
                .clickable(onClick = actions::togglePlayPause),
            contentAlignment = Alignment.Center,
        ) {
            AtxIcon(
                if (ui.isPlaying) Res.drawable.pause else Res.drawable.play,
                if (ui.isPlaying) "Pausa" else "Riproduci",
                size = 40.dp,
                tint = ui.onAccent,
                modifier = Modifier.rotate(if (spin) -rotation else 0f),
            )
        }
        IconButton(onClick = connection::seekToNext, modifier = Modifier.size(64.dp)) {
            AtxIcon(Res.drawable.skip_next, "Brano successivo", size = 40.dp)
        }
        IconButton(onClick = {
            connection.player.repeatMode =
                when (repeatMode) {
                    Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                    Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                    else -> Player.REPEAT_MODE_OFF
                }
        }) {
            AtxIcon(
                when (repeatMode) {
                    Player.REPEAT_MODE_ALL -> Res.drawable.repeat_on
                    Player.REPEAT_MODE_ONE -> Res.drawable.repeat_one_on
                    else -> Res.drawable.repeat
                },
                "Ripeti",
                tint = if (repeatMode != Player.REPEAT_MODE_OFF) ui.accent else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ActionsRow(
    ui: PlayerUi,
    pill: Boolean = true,
) {
    val actions = LocalActions.current
    val navigator = LocalNavigator.current
    val download by remember(ui.song.id) { actions.downloads.getDownload(ui.song.id) }.collectAsState(null)
    Spacer(Modifier.height(12.dp))
    Row(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .then(
                if (pill) {
                    Modifier.clip(RoundedCornerShape(28.dp)).background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f))
                } else {
                    Modifier
                },
            ).padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = { navigator.lyricsVisible = true }) {
            AtxIcon(Res.drawable.lyrics, "Testo")
        }
        when (download?.state) {
            Download.STATE_COMPLETED, Download.STATE_DOWNLOADING, Download.STATE_QUEUED, Download.STATE_RESTARTING ->
                Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) { DownloadBadge(ui.song.id, Modifier.size(22.dp)) }
            else ->
                IconButton(onClick = { actions.download(ui.song) }) {
                    AtxIcon(Res.drawable.download, "Scarica")
                }
        }
        IconButton(onClick = { navigator.queueVisible = true }) {
            AtxIcon(Res.drawable.queue_music, "Coda")
        }
    }
}

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
