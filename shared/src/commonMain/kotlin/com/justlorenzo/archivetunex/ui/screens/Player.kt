package com.justlorenzo.archivetunex.ui.screens

import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.IconButton
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import archivetunex.shared.generated.resources.Res
import archivetunex.shared.generated.resources.download
import archivetunex.shared.generated.resources.expand_more
import archivetunex.shared.generated.resources.favorite
import archivetunex.shared.generated.resources.favorite_border
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
import com.justlorenzo.archivetunex.ui.components.Artwork
import com.justlorenzo.archivetunex.ui.components.AtxIcon
import com.justlorenzo.archivetunex.ui.components.DownloadBadge
import com.justlorenzo.archivetunex.ui.components.SongMenuSheet
import com.justlorenzo.archivetunex.ui.components.SongRow
import kotlinx.coroutines.delay
import moe.rukamori.archivetune.compat.media3.C
import moe.rukamori.archivetune.compat.media3.Download
import moe.rukamori.archivetune.compat.media3.Player
import moe.rukamori.archivetune.extensions.metadata
import moe.rukamori.archivetune.playback.PlayerConnection

/** Posizione e durata del brano, aggiornate più volte al secondo. */
@Composable
private fun rememberProgress(connection: PlayerConnection): Pair<Long, Long> {
    var position by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    LaunchedEffect(connection) {
        while (true) {
            position = connection.player.currentPosition.coerceAtLeast(0L)
            duration = connection.player.duration.takeIf { it != C.TIME_UNSET && it > 0 } ?: 0L
            delay(250)
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

/** Mini player sopra la barra delle schede. */
@Composable
fun MiniPlayer(modifier: Modifier = Modifier) {
    val actions = LocalActions.current
    val navigator = LocalNavigator.current
    val connection = actions.player
    val metadata by connection.mediaMetadata.collectAsState()
    val isPlaying by connection.isPlaying.collectAsState()
    val song = metadata ?: return
    val (position, duration) = rememberProgress(connection)

    Surface(
        modifier = modifier.fillMaxWidth().padding(horizontal = 8.dp).clip(MaterialTheme.shapes.large).clickable { navigator.playerExpanded = true },
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = MaterialTheme.shapes.large,
        tonalElevation = 2.dp,
    ) {
        Column {
            Row(
                Modifier.fillMaxWidth().height(64.dp).padding(start = 8.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Artwork(song.thumbnailUrl, Modifier.size(48.dp), MaterialTheme.shapes.small, sizePx = 226)
                Column(Modifier.weight(1f)) {
                    Text(song.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        song.artists.joinToString { it.name },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                IconButton(onClick = actions::togglePlayPause) {
                    AtxIcon(if (isPlaying) Res.drawable.pause else Res.drawable.play, if (isPlaying) "Pausa" else "Riproduci", size = 28.dp)
                }
                IconButton(onClick = connection::seekToNext) {
                    AtxIcon(Res.drawable.skip_next, "Brano successivo", size = 28.dp)
                }
            }
            LinearProgressIndicator(
                progress = { if (duration > 0) (position.toFloat() / duration).coerceIn(0f, 1f) else 0f },
                modifier = Modifier.fillMaxWidth().height(2.dp),
                drawStopIndicator = {},
                gapSize = 0.dp,
            )
        }
    }
}

/** Player a schermo intero. */
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
    val artworkScale by animateFloatAsState(if (isPlaying) 1f else 0.86f, label = "artwork")
    val download by remember(song.id) { actions.downloads.getDownload(song.id) }.collectAsState(null)

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 24.dp),
        ) {
            Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { navigator.playerExpanded = false }) {
                    AtxIcon(Res.drawable.expand_more, "Chiudi player", size = 30.dp)
                }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "In riproduzione",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    queueTitle?.let {
                        Text(it, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                IconButton(onClick = { showMenu = true }) { AtxIcon(Res.drawable.more_vert, "Altre opzioni") }
            }

            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                val side = minOf(maxWidth, maxHeight - 16.dp)
                Artwork(
                    song.thumbnailUrl,
                    Modifier
                        .size(side)
                        .scale(artworkScale)
                        .shadow(24.dp, MaterialTheme.shapes.extraLarge),
                    shape = MaterialTheme.shapes.extraLarge,
                    sizePx = 1080,
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(song.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        song.artists.joinToString { it.name },
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier =
                            Modifier.clickable(enabled = song.artists.firstOrNull()?.id != null) {
                                song.artists.firstOrNull()?.id?.let { navigator.open(com.justlorenzo.archivetunex.ui.Route.Artist(it)) }
                            },
                    )
                }
                FilledTonalIconButton(onClick = connection::toggleLike) {
                    AtxIcon(
                        if (liked) Res.drawable.favorite else Res.drawable.favorite_border,
                        if (liked) "Togli dai preferiti" else "Aggiungi ai preferiti",
                        tint = if (liked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            Slider(
                value = if (seeking) seekValue else position.toFloat(),
                onValueChange = {
                    seeking = true
                    seekValue = it
                },
                onValueChangeFinished = {
                    connection.player.seekTo(seekValue.toLong())
                    seeking = false
                },
                valueRange = 0f..(duration.takeIf { it > 0 } ?: 1L).toFloat(),
                modifier = Modifier.fillMaxWidth(),
            )
            Row(Modifier.fillMaxWidth()) {
                Text(
                    formatTime(if (seeking) seekValue.toLong() else position),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    if (duration > 0) formatTime(duration) else "--:--",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
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
                        tint = if (shuffle) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                FilledTonalIconButton(
                    onClick = connection::seekToPrevious,
                    modifier = Modifier.size(64.dp),
                ) { AtxIcon(Res.drawable.skip_previous, "Brano precedente", size = 32.dp) }
                FilledIconButton(
                    onClick = actions::togglePlayPause,
                    modifier = Modifier.size(84.dp),
                    shape = if (isPlaying) RoundedCornerShape(28.dp) else CircleShape,
                ) { AtxIcon(if (isPlaying) Res.drawable.pause else Res.drawable.play, if (isPlaying) "Pausa" else "Riproduci", size = 40.dp) }
                FilledTonalIconButton(
                    onClick = connection::seekToNext,
                    modifier = Modifier.size(64.dp),
                ) { AtxIcon(Res.drawable.skip_next, "Brano successivo", size = 32.dp) }
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
                        tint = if (repeatMode != Player.REPEAT_MODE_OFF) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            Row(
                Modifier.fillMaxWidth().padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically,
            ) {
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
            "Coda",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 20.dp, bottom = 8.dp),
        )
        LazyColumn(state = listState, modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainerLow)) {
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
