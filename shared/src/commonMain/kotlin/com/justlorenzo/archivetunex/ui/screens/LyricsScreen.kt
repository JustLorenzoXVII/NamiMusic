package com.justlorenzo.archivetunex.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import archivetunex.shared.generated.resources.Res
import archivetunex.shared.generated.resources.expand_more
import archivetunex.shared.generated.resources.lyrics
import com.justlorenzo.archivetunex.ui.LocalActions
import com.justlorenzo.archivetunex.ui.LocalNavigator
import com.justlorenzo.archivetunex.ui.components.Artwork
import com.justlorenzo.archivetunex.ui.components.AtxIcon
import com.justlorenzo.archivetunex.ui.components.EmptyState
import com.justlorenzo.archivetunex.ui.components.LoadingBox
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import moe.rukamori.archivetune.compat.Timber
import moe.rukamori.archivetune.db.entities.LyricsEntity
import moe.rukamori.archivetune.lyrics.LyricsEntry
import moe.rukamori.archivetune.lyrics.LyricsHelper
import moe.rukamori.archivetune.lyrics.LyricsUtils
import org.koin.mp.KoinPlatform

/** Testo del brano corrente: sincronizzato (la riga attuale si illumina e scorre) o semplice. */
@Composable
fun LyricsOverlay() {
    val actions = LocalActions.current
    val navigator = LocalNavigator.current
    val connection = actions.player
    val metadata by connection.mediaMetadata.collectAsState()
    val lyricsEntity by connection.currentLyrics.collectAsState(null)
    val song = metadata
    var searching by remember(song?.id) { mutableStateOf(false) }

    // Se il testo non è ancora nel database lo cerca nei siti dei testi (come ArchiveTune)
    LaunchedEffect(song?.id) {
        val current = song ?: return@LaunchedEffect
        if (actions.database.lyrics(current.id).first() != null) return@LaunchedEffect
        searching = true
        try {
            val lyrics = KoinPlatform.getKoin().get<LyricsHelper>().getLyrics(current)
            actions.database.query { insertLyricsIfAbsent(id = current.id, lyrics = lyrics) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.tag("Lyrics").w(e, "Testo non trovato")
        } finally {
            searching = false
        }
    }

    val text = lyricsEntity?.lyrics?.takeIf { it.isNotBlank() && it != LyricsEntity.LYRICS_NOT_FOUND }
    val lines =
        remember(text, song?.duration) {
            when {
                text == null -> emptyList()
                LyricsUtils.isTtml(text) -> LyricsUtils.parseTtml(text, song?.duration).filter { it.text.isNotBlank() }
                LyricsUtils.isLineSyncedLrc(text) -> LyricsUtils.parseLyrics(text).filter { it.text.isNotBlank() }
                else -> emptyList()
            }
        }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                IconButton(onClick = { navigator.lyricsVisible = false }) {
                    AtxIcon(Res.drawable.expand_more, "Chiudi testo", size = 30.dp)
                }
                if (song != null) {
                    Artwork(song.thumbnailUrl, Modifier.size(44.dp), MaterialTheme.shapes.small, sizePx = 226)
                    Column(Modifier.weight(1f)) {
                        Text(song.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            song.artists.joinToString { it.name },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            when {
                text == null && searching -> LoadingBox(Modifier.padding(top = 80.dp))
                text == null -> EmptyState("Testo non disponibile per questo brano.", icon = Res.drawable.lyrics)
                lines.isNotEmpty() -> SyncedLyrics(lines)
                else ->
                    Text(
                        text,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
                    )
            }
        }
    }
}

@Composable
private fun SyncedLyrics(lines: List<LyricsEntry>) {
    val connection = LocalActions.current.player
    var position by remember { mutableLongStateOf(0L) }
    LaunchedEffect(connection) {
        while (true) {
            position = connection.player.currentPosition
            delay(100)
        }
    }
    val current = LyricsUtils.findCurrentLineIndex(lines, position)
    val listState = rememberLazyListState()

    // La riga attuale resta nel terzo alto dello schermo
    LaunchedEffect(current) {
        if (current >= 0) listState.animateScrollToItem((current - 2).coerceAtLeast(0))
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 320.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        itemsIndexed(lines) { index, line ->
            val isCurrent = index == current
            val alpha by animateFloatAsState(if (isCurrent) 1f else if (index < current) 0.45f else 0.6f, label = "alpha")
            val color by animateColorAsState(
                if (isCurrent) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant,
                label = "color",
            )
            Text(
                line.text,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = color,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .alpha(alpha)
                        .clickable { connection.player.seekTo(line.time) },
            )
        }
    }
}
