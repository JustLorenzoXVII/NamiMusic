package com.justlorenzo.archivetunex.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.ui.draw.blur
import com.justlorenzo.archivetunex.ui.LocalAtxStyle
import com.justlorenzo.archivetunex.ui.MiniPlayerStyle
import com.justlorenzo.archivetunex.ui.PlayButtonShape
import com.justlorenzo.archivetunex.ui.PlayerBackground
import com.justlorenzo.archivetunex.ui.SliderStyleChoice
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
import com.justlorenzo.archivetunex.ui.LocalActions
import com.justlorenzo.archivetunex.ui.LocalNavigator
import com.justlorenzo.archivetunex.ui.Route
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
import moe.rukamori.archivetune.playback.PlayerConnection
import moe.rukamori.archivetune.ui.player.WavySliderExpressive

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

/** Mini player fluttuante, colorato con la copertina del brano. */
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
    val accent = animatedArtworkColor(rememberArtworkColors(song.thumbnailUrl))
    val container =
        when (style.miniPlayer) {
            MiniPlayerStyle.Tinted -> accent.tint(MaterialTheme.colorScheme.surfaceContainerHigh, 0.28f)
            MiniPlayerStyle.Accent -> accent
            MiniPlayerStyle.Neutral -> MaterialTheme.colorScheme.surfaceContainerHigh
        }
    val miniContent = if (style.miniPlayer == MiniPlayerStyle.Accent) accent.readableOn() else MaterialTheme.colorScheme.onSurface
    val progressColor = if (style.miniPlayer == MiniPlayerStyle.Accent) miniContent else accent

    Surface(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .shadow(14.dp, RoundedCornerShape(26.dp))
                .clip(RoundedCornerShape(26.dp))
                .clickable { navigator.playerExpanded = true },
        color = container,
        shape = RoundedCornerShape(26.dp),
        contentColor = miniContent,
    ) {
        Box {
            Row(
                Modifier.fillMaxWidth().height(68.dp).padding(start = 10.dp, end = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Artwork(song.thumbnailUrl, Modifier.size(48.dp), RoundedCornerShape(14.dp), sizePx = 226)
                Column(Modifier.weight(1f)) {
                    Text(song.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        song.artists.joinToString { it.name },
                        style = MaterialTheme.typography.bodyMedium,
                        color = miniContent.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                IconButton(onClick = actions::togglePlayPause) {
                    AtxIcon(if (isPlaying) Res.drawable.pause else Res.drawable.play, if (isPlaying) "Pausa" else "Riproduci", size = 30.dp)
                }
                if (style.miniPlayerNext) {
                    IconButton(onClick = connection::seekToNext) {
                        AtxIcon(Res.drawable.skip_next, "Brano successivo", size = 28.dp)
                    }
                }
            }
            // Avanzamento: una linea del colore della copertina lungo il bordo inferiore
            if (style.miniPlayerProgress) Box(
                Modifier
                    .align(Alignment.BottomStart)
                    .padding(horizontal = 22.dp)
                    .fillMaxWidth(if (duration > 0) (position.toFloat() / duration).coerceIn(0f, 1f) else 0f)
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(progressColor),
            )
        }
    }
}

/** Player a schermo intero: sfondo e comandi presi dai colori della copertina. */
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
    val queueTitle by connection.queueTitle.collectAsState()
    val (position, duration) = rememberProgress(connection)
    var seeking by remember { mutableStateOf(false) }
    var seekValue by remember { mutableFloatStateOf(0f) }
    var showMenu by remember { mutableStateOf(false) }
    val song = metadata ?: return
    val liked = currentSong?.song?.liked == true
    val download by remember(song.id) { actions.downloads.getDownload(song.id) }.collectAsState(null)

    val style = LocalAtxStyle.current
    val colors = rememberArtworkColors(song.thumbnailUrl)
    val artworkAccent = animatedArtworkColor(colors)
    val accent = if (style.coloredPlayerControls) artworkAccent else MaterialTheme.colorScheme.onSurface
    val background = MaterialTheme.colorScheme.background
    val top = artworkAccent.tint(background, 0.55f)
    val middle = (colors?.gradient?.getOrNull(1) ?: artworkAccent).tint(background, 0.25f)
    val onAccent = accent.readableOn()
    val artworkShape = RoundedCornerShape(style.corners.radius)

    val artworkScale by animateFloatAsState(if (isPlaying || !style.shrinkArtworkOnPause) 1f else 0.84f, spring(dampingRatio = 0.55f, stiffness = 300f), label = "artwork")
    // Il pulsante Play è un "biscotto" Expressive che ruota piano mentre la musica suona
    val rotation by rememberInfiniteTransition(label = "play").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(12000, easing = LinearEasing), RepeatMode.Restart),
        label = "rotation",
    )
    val playShape =
        when (style.playButton) {
            PlayButtonShape.CookieSpin, PlayButtonShape.Cookie -> MaterialShapes.Cookie9Sided.toShape()
            PlayButtonShape.Circle -> androidx.compose.foundation.shape.CircleShape
            PlayButtonShape.Squircle -> RoundedCornerShape(30.dp)
        }
    val spin = style.playButton == PlayButtonShape.CookieSpin && isPlaying

    // Surface: blocca i tocchi verso le schermate sotto il player
    Surface(Modifier.fillMaxSize(), color = background) {
    Box(
        Modifier
            .fillMaxSize()
            .then(
                when (style.playerBackground) {
                    PlayerBackground.Gradient -> Modifier.background(Brush.verticalGradient(listOf(top, middle, background)))
                    PlayerBackground.Solid -> Modifier.background(artworkAccent.tint(background, 0.6f))
                    PlayerBackground.Blur, PlayerBackground.Plain -> Modifier
                },
            ),
    ) {
        if (style.playerBackground == PlayerBackground.Blur) {
            Artwork(song.thumbnailUrl, Modifier.fillMaxSize().blur(70.dp), RoundedCornerShape(0.dp), sizePx = 300)
            Box(Modifier.fillMaxSize().background(background.copy(alpha = 0.55f)))
        }
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 24.dp)) {
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
                IconButton(onClick = { showMenu = true }) { AtxIcon(Res.drawable.more_vert, "Altre opzioni") }
            }

            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                val side = minOf(maxWidth, maxHeight - 24.dp)
                Artwork(
                    song.thumbnailUrl,
                    Modifier
                        .size(side)
                        .scale(artworkScale)
                        .shadow(32.dp, artworkShape, ambientColor = artworkAccent, spotColor = artworkAccent),
                    shape = artworkShape,
                    sizePx = 1080,
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        song.title,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.5).sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        song.artists.joinToString { it.name },
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier =
                            Modifier.clickable(enabled = song.artists.firstOrNull()?.id != null) {
                                song.artists.firstOrNull()?.id?.let { navigator.open(Route.Artist(it)) }
                            },
                    )
                }
                IconButton(onClick = connection::toggleLike) {
                    AtxIcon(
                        if (liked) Res.drawable.favorite else Res.drawable.favorite_border,
                        if (liked) "Togli dai preferiti" else "Aggiungi ai preferiti",
                        size = 28.dp,
                        tint = if (liked) accent else MaterialTheme.colorScheme.onSurface,
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            val onSeek: (Float) -> Unit = {
                seeking = true
                seekValue = it
            }
            val onSeekDone = {
                connection.player.seekTo(seekValue.toLong())
                seeking = false
            }
            val range = 0f..(duration.takeIf { it > 0 } ?: 1L).toFloat()
            when (style.slider) {
                SliderStyleChoice.Classic ->
                    Slider(
                        value = if (seeking) seekValue else position.toFloat(),
                        onValueChange = onSeek,
                        onValueChangeFinished = onSeekDone,
                        valueRange = range,
                        colors = SliderDefaults.colors(thumbColor = accent, activeTrackColor = accent),
                        modifier = Modifier.fillMaxWidth(),
                    )
                else ->
                    WavySliderExpressive(
                        value = { if (seeking) seekValue else position.toFloat() },
                        onValueChange = onSeek,
                        onValueChangeFinished = onSeekDone,
                        valueRange = range,
                        activeTrackColor = accent,
                        thumbColor = accent,
                        inactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f),
                        isPlaying = isPlaying,
                        strokeWidth = if (style.slider == SliderStyleChoice.Thick) 10.dp else 5.dp,
                        waveAmplitudeWhenPlaying = if (style.slider == SliderStyleChoice.Thick) 0.dp else 4.dp,
                        modifier = Modifier.fillMaxWidth(),
                    )
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
                Text(
                    formatTime(if (seeking) seekValue.toLong() else position),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    when {
                        duration <= 0 -> "--:--"
                        style.showRemainingTime -> "-" + formatTime(duration - position)
                        else -> formatTime(duration)
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.height(10.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { connection.player.shuffleModeEnabled = !shuffle }) {
                    AtxIcon(
                        if (shuffle) Res.drawable.shuffle_on else Res.drawable.shuffle,
                        "Riproduzione casuale",
                        tint = if (shuffle) accent else MaterialTheme.colorScheme.onSurfaceVariant,
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
                        .background(accent)
                        .clickable(onClick = actions::togglePlayPause),
                    contentAlignment = Alignment.Center,
                ) {
                    AtxIcon(
                        if (isPlaying) Res.drawable.pause else Res.drawable.play,
                        if (isPlaying) "Pausa" else "Riproduci",
                        size = 40.dp,
                        tint = onAccent,
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
                        tint = if (repeatMode != Player.REPEAT_MODE_OFF) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            // Barra delle azioni: una pillola tonale come nelle app Expressive
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f))
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { navigator.lyricsVisible = true }) {
                    AtxIcon(Res.drawable.lyrics, "Testo")
                }
                when (download?.state) {
                    Download.STATE_COMPLETED, Download.STATE_DOWNLOADING, Download.STATE_QUEUED, Download.STATE_RESTARTING ->
                        Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) { DownloadBadge(song.id, Modifier.size(22.dp)) }
                    else ->
                        IconButton(onClick = { actions.download(song) }) {
                            AtxIcon(Res.drawable.download, "Scarica")
                        }
                }
                IconButton(onClick = { navigator.queueVisible = true }) {
                    AtxIcon(Res.drawable.queue_music, "Coda")
                }
            }
        }
    }

    }

    if (showMenu) SongMenuSheet(song, onDismiss = { showMenu = false })
}

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
