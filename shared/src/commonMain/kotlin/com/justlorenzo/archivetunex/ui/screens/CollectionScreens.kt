package com.justlorenzo.archivetunex.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import archivetunex.shared.generated.resources.Res
import archivetunex.shared.generated.resources.download
import archivetunex.shared.generated.resources.favorite
import com.justlorenzo.archivetunex.ui.LocalActions
import com.justlorenzo.archivetunex.ui.LocalNavigator
import com.justlorenzo.archivetunex.ui.components.BackButton
import com.justlorenzo.archivetunex.ui.components.BottomContentPadding
import com.justlorenzo.archivetunex.ui.components.EmptyState
import com.justlorenzo.archivetunex.ui.components.ErrorBox
import com.justlorenzo.archivetunex.ui.components.ItemCard
import com.justlorenzo.archivetunex.ui.components.LoadState
import com.justlorenzo.archivetunex.ui.components.LoadingBox
import com.justlorenzo.archivetunex.ui.components.PlayShuffleButtons
import com.justlorenzo.archivetunex.ui.components.SectionHeader
import com.justlorenzo.archivetunex.ui.components.SongMenuSheet
import com.justlorenzo.archivetunex.ui.components.SongRow
import com.justlorenzo.archivetunex.ui.components.rememberLoadable
import com.justlorenzo.archivetunex.ui.components.subtitle
import com.justlorenzo.archivetunex.ui.components.tint
import com.justlorenzo.archivetunex.ui.openItem
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import moe.rukamori.archivetune.compat.media3.Download
import moe.rukamori.archivetune.db.entities.Song
import moe.rukamori.archivetune.innertube.YouTube
import moe.rukamori.archivetune.innertube.models.SongItem
import moe.rukamori.archivetune.models.MediaMetadata
import moe.rukamori.archivetune.models.toMediaMetadata
import org.jetbrains.compose.resources.DrawableResource

/** Brani scaricati sul telefono. */
@Composable
fun DownloadsScreen() {
    val actions = LocalActions.current
    val songs by remember {
        actions.downloads.downloads
            .map { map -> map.filterValues { it.state != Download.STATE_FAILED }.keys.toList() }
            .distinctUntilChanged()
            .flatMapLatest { ids -> actions.database.songsByIds(ids) }
    }.collectAsState(emptyList())
    SongCollection(
        title = "Scaricati",
        overline = "Sul telefono",
        colors = listOf(Color(0xFFFA4058), Color(0xFFB0124A)),
        songs = songs,
        emptyText = "I brani che scarichi appaiono qui e si ascoltano anche senza connessione.",
        emptyIcon = Res.drawable.download,
    )
}

/** Brani con il cuore. */
@Composable
fun LikedScreen() {
    val actions = LocalActions.current
    val songs by remember { actions.database.likedSongsByCreateDateAsc().map { it.reversed() } }.collectAsState(emptyList())
    SongCollection(
        title = "Preferiti",
        overline = "I tuoi cuori",
        colors = listOf(Color(0xFF7B5CFF), Color(0xFF3A1F9E)),
        songs = songs,
        emptyText = "Tocca il cuore nel player per aggiungere un brano ai preferiti.",
        emptyIcon = Res.drawable.favorite,
    )
}

@Composable
private fun SongCollection(
    title: String,
    overline: String,
    colors: List<Color>,
    songs: List<Song>,
    emptyText: String,
    emptyIcon: DrawableResource,
) {
    val actions = LocalActions.current
    var menuSong by remember { mutableStateOf<MediaMetadata?>(null) }
    val metadata = remember(songs) { songs.map { it.toMediaMetadata() } }
    val background = MaterialTheme.colorScheme.background
    val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(420.dp)
                .background(Brush.verticalGradient(listOf(colors.first().tint(background, 0.7f), colors.last().tint(background, 0.25f), background))),
        )
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(top = top + 64.dp, bottom = BottomContentPadding.calculateBottomPadding())) {
            item(key = "header") {
                Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        overline.uppercase(),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp,
                    )
                    Text(title, style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Black, letterSpacing = (-1.5).sp)
                    Text(
                        "${songs.size} brani",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (songs.isNotEmpty()) {
                        PlayShuffleButtons(
                            onPlay = { actions.playList(metadata, title = title) },
                            onShuffle = { actions.playList(metadata, title = title, shuffle = true) },
                            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
                        )
                    }
                }
            }
            if (songs.isEmpty()) item(key = "empty") { EmptyState(emptyText, icon = emptyIcon) }
            itemsIndexed(metadata, key = { _, song -> song.id }) { index, song ->
                SongRow(
                    songId = song.id,
                    title = song.title,
                    subtitle = song.artists.joinToString { it.name },
                    artworkUrl = song.thumbnailUrl,
                    explicit = song.explicit,
                    onClick = { actions.playList(metadata, startIndex = index, title = title) },
                    onMore = { menuSong = song },
                )
            }
        }
        BackButton()
    }
    menuSong?.let { SongMenuSheet(it, onDismiss = { menuSong = null }) }
}

/** Pagina generica di YouTube Music (un genere, un mood, "altro" di una sezione). */
@Composable
fun BrowseScreen(
    browseId: String,
    params: String?,
    title: String,
) {
    val actions = LocalActions.current
    val navigator = LocalNavigator.current
    val page = rememberLoadable(browseId to params) { YouTube.browse(browseId, params) }
    val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    Box(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(top = top + 56.dp, bottom = BottomContentPadding.calculateBottomPadding())) {
            item(key = "title") {
                Text(
                    (page.value as? LoadState.Ready)?.value?.title ?: title,
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-1).sp,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
            }
            when (val state = page.value) {
                LoadState.Loading -> item(key = "loading") { LoadingBox() }
                is LoadState.Failed -> item(key = "error") { ErrorBox(state.message, page::reload) }
                is LoadState.Ready ->
                    state.value.items.forEachIndexed { index, section ->
                        if (section.items.isEmpty()) return@forEachIndexed
                        item(key = "section-$index") {
                            section.title?.let { SectionHeader(it) }
                            if (section.items.all { it is SongItem }) {
                                Column {
                                    section.items.take(8).forEach { item ->
                                        val song = item as SongItem
                                        SongRow(
                                            songId = song.id,
                                            title = song.title,
                                            subtitle = song.subtitle(),
                                            artworkUrl = song.thumbnail,
                                            explicit = song.explicit,
                                            onClick = { actions.playSong(song) },
                                        )
                                    }
                                }
                            } else {
                                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(section.items) { item -> ItemCard(item, onClick = { openItem(item, actions, navigator) }) }
                                }
                            }
                        }
                    }
            }
        }
        BackButton()
    }
}
