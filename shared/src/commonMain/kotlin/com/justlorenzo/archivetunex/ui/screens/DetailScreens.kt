package com.justlorenzo.archivetunex.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Brush
import com.justlorenzo.archivetunex.ui.components.animatedArtworkColor
import com.justlorenzo.archivetunex.ui.components.rememberArtworkColors
import com.justlorenzo.archivetunex.ui.components.tint
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.justlorenzo.archivetunex.ui.LocalActions
import com.justlorenzo.archivetunex.ui.LocalNavigator
import com.justlorenzo.archivetunex.ui.components.BackButton
import com.justlorenzo.archivetunex.ui.components.ConfirmDialog
import com.justlorenzo.archivetunex.ui.components.RenamePlaylistDialog
import com.justlorenzo.archivetunex.ui.components.BottomContentPadding
import com.justlorenzo.archivetunex.ui.components.DetailHeader
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
import com.justlorenzo.archivetunex.ui.openItem
import kotlinx.coroutines.flow.first
import moe.rukamori.archivetune.db.entities.ArtistEntity
import moe.rukamori.archivetune.innertube.YouTube
import moe.rukamori.archivetune.innertube.models.SongItem
import moe.rukamori.archivetune.innertube.models.distinctByPlaylistEntry
import moe.rukamori.archivetune.models.MediaMetadata
import moe.rukamori.archivetune.models.toMediaMetadata

private const val MAX_PLAYLIST_PAGES = 20

/** Spazio sopra (barra di stato + freccia indietro) e sotto (mini player + schede). */
@Composable
private fun detailPadding(): PaddingValues {
    val top = androidx.compose.foundation.layout.WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    return PaddingValues(top = top + 56.dp, bottom = BottomContentPadding.calculateBottomPadding())
}

/** Pagina con freccia indietro sopra il contenuto. */
@Composable
private fun DetailScaffold(
    accentUrl: String?,
    content: @Composable () -> Unit,
) {
    // In alto una sfumatura del colore della copertina, che sfuma nello sfondo
    val accent = animatedArtworkColor(rememberArtworkColors(accentUrl), MaterialTheme.colorScheme.background)
    val background = MaterialTheme.colorScheme.background
    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(520.dp)
                .background(Brush.verticalGradient(listOf(accent.tint(background, 0.6f), accent.tint(background, 0.2f), background))),
        )
        content()
        BackButton()
    }
}

/** Lista di brani di YouTube Music (album o playlist) con menu. */
@Composable
private fun rememberSongMenu(): Pair<MediaMetadata?, (MediaMetadata?) -> Unit> {
    var menuSong by remember { mutableStateOf<MediaMetadata?>(null) }
    return menuSong to { menuSong = it }
}

@Composable
fun AlbumScreen(browseId: String) {
    val actions = LocalActions.current
    val page = rememberLoadable(browseId) { YouTube.album(browseId) }
    val saved by remember(browseId) { actions.database.album(browseId) }.collectAsState(null)
    val (menuSong, setMenuSong) = rememberSongMenu()

    DetailScaffold((page.value as? LoadState.Ready)?.value?.album?.thumbnail) {
        when (val state = page.value) {
            LoadState.Loading -> LoadingBox(Modifier.padding(top = 120.dp))
            is LoadState.Failed -> ErrorBox(state.message, page::reload, Modifier.padding(top = 120.dp))
            is LoadState.Ready -> {
                val albumItem = state.value.album
                val songs = remember(state.value) { state.value.songs.map { it.toMediaMetadata() } }
                val isSaved = saved?.album?.bookmarkedAt != null
                LazyColumn(Modifier.fillMaxSize(), contentPadding = detailPadding()) {
                    item(key = "header") {
                        DetailHeader(
                            artworkUrl = albumItem.thumbnail,
                            title = albumItem.title,
                            subtitle = albumItem.artists?.joinToString { it.name },
                            detail = listOfNotNull(albumItem.year?.toString(), "${songs.size} brani").joinToString(" · "),
                        ) {
                            androidx.compose.foundation.layout.Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                PlayShuffleButtons(
                                    onPlay = { actions.playList(songs, title = albumItem.title) },
                                    onShuffle = { actions.playList(songs, title = albumItem.title, shuffle = true) },
                                    onDownload = { actions.downloadAll(songs) },
                                )
                                OutlinedButton(
                                    onClick = {
                                        actions.database.transaction {
                                            insert(state.value)
                                            album(browseId).first()?.album?.let { update(it.toggleLike()) }
                                        }
                                    },
                                    modifier = Modifier.padding(horizontal = 0.dp),
                                ) { Text(if (isSaved) "Salvato in libreria" else "Salva in libreria") }
                            }
                        }
                    }
                    itemsIndexed(songs) { index, song ->
                        SongRow(
                            songId = song.id,
                            title = song.title,
                            subtitle = song.artists.joinToString { it.name },
                            artworkUrl = song.thumbnailUrl,
                            explicit = song.explicit,
                            leadingNumber = index + 1,
                            onClick = { actions.playList(songs, startIndex = index, title = albumItem.title) },
                            onMore = { setMenuSong(song) },
                        )
                    }
                    if (state.value.otherVersions.isNotEmpty()) {
                        item(key = "versions") {
                            val navigator = LocalNavigator.current
                            SectionHeader("Altre versioni")
                            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp)) {
                                items(state.value.otherVersions) { other ->
                                    ItemCard(other, onClick = { openItem(other, actions, navigator) })
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    menuSong?.let { SongMenuSheet(it, onDismiss = { setMenuSong(null) }) }
}

@Composable
fun PlaylistScreen(playlistId: String) {
    val actions = LocalActions.current
    val savedPlaylist by remember(playlistId) { actions.database.playlistByBrowseId(playlistId) }.collectAsState(null)
    val (menuSong, setMenuSong) = rememberSongMenu()
    val page =
        rememberLoadable(playlistId) {
            YouTube.playlist(playlistId).mapCatching { first ->
                // Le playlist lunghe arrivano a pagine: si caricano tutte (fino a un limite)
                val songs = first.songs.toMutableList()
                var continuation = first.songsContinuation ?: first.continuation
                var pages = 0
                while (continuation != null && pages < MAX_PLAYLIST_PAGES) {
                    val next = YouTube.playlistContinuation(continuation, playlistId).getOrNull() ?: break
                    songs += next.songs
                    continuation = next.continuation
                    pages++
                }
                first to songs.distinctByPlaylistEntry()
            }
        }

    DetailScaffold((page.value as? LoadState.Ready)?.value?.first?.playlist?.thumbnail) {
        when (val state = page.value) {
            LoadState.Loading -> LoadingBox(Modifier.padding(top = 120.dp))
            is LoadState.Failed -> ErrorBox(state.message, page::reload, Modifier.padding(top = 120.dp))
            is LoadState.Ready -> {
                val (info, songItems) = state.value
                val playlist = info.playlist
                val songs = remember(songItems) { songItems.map(SongItem::toMediaMetadata) }
                LazyColumn(Modifier.fillMaxSize(), contentPadding = detailPadding()) {
                    item(key = "header") {
                        DetailHeader(
                            artworkUrl = playlist.thumbnail,
                            title = playlist.title,
                            subtitle = playlist.author?.name,
                            detail = playlist.songCountText ?: "${songs.size} brani",
                        ) {
                            androidx.compose.foundation.layout.Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                PlayShuffleButtons(
                                    onPlay = { actions.playList(songs, title = playlist.title) },
                                    onShuffle = { actions.playList(songs, title = playlist.title, shuffle = true) },
                                    onDownload = { actions.downloadAll(songs) },
                                )
                                OutlinedButton(onClick = {
                                    if (savedPlaylist != null) {
                                        actions.unsaveYouTubePlaylist(playlistId)
                                    } else {
                                        actions.saveYouTubePlaylist(playlist, songs)
                                    }
                                }) { Text(if (savedPlaylist != null) "Salvata in libreria" else "Salva in libreria") }
                            }
                        }
                    }
                    if (songs.isEmpty()) item(key = "empty") { EmptyState("Questa playlist è vuota.") }
                    itemsIndexed(songs) { index, song ->
                        SongRow(
                            songId = song.id,
                            title = song.title,
                            subtitle = song.artists.joinToString { it.name },
                            artworkUrl = song.thumbnailUrl,
                            explicit = song.explicit,
                            onClick = { actions.playList(songs, startIndex = index, title = playlist.title) },
                            onMore = { setMenuSong(song) },
                        )
                    }
                }
            }
        }
    }
    menuSong?.let { SongMenuSheet(it, onDismiss = { setMenuSong(null) }) }
}

/** Playlist creata nell'app (solo nel database del telefono). */
@Composable
fun LocalPlaylistScreen(
    playlistId: String,
    title: String,
) {
    val actions = LocalActions.current
    val navigator = LocalNavigator.current
    var menuEntryIndex by remember { mutableStateOf<Int?>(null) }
    var renaming by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }
    val playlist by remember(playlistId) { actions.database.playlist(playlistId) }.collectAsState(null)
    val entries by remember(playlistId) { actions.database.playlistSongs(playlistId) }.collectAsState(emptyList())
    val songs = remember(entries) { entries.map { it.song.toMediaMetadata() } }
    val name = playlist?.playlist?.name ?: title

    DetailScaffold(songs.firstOrNull()?.thumbnailUrl) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = detailPadding()) {
            item(key = "header") {
                DetailHeader(
                    artworkUrl = songs.firstOrNull()?.thumbnailUrl,
                    title = name,
                    subtitle = null,
                    detail = "${songs.size} brani",
                ) {
                    androidx.compose.foundation.layout.Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        PlayShuffleButtons(
                            onPlay = { actions.playList(songs, title = name) },
                            onShuffle = { actions.playList(songs, title = name, shuffle = true) },
                            onDownload = { actions.downloadAll(songs) },
                        )
                        androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { renaming = true }) { Text("Rinomina") }
                            OutlinedButton(onClick = { deleting = true }) { Text("Elimina") }
                        }
                    }
                }
            }
            if (songs.isEmpty()) item(key = "empty") { EmptyState("Questa playlist è vuota. Aggiungi brani dal menu ⋮ di un brano.") }
            itemsIndexed(songs) { index, song ->
                SongRow(
                    songId = song.id,
                    title = song.title,
                    subtitle = song.artists.joinToString { it.name },
                    artworkUrl = song.thumbnailUrl,
                    explicit = song.explicit,
                    onClick = { actions.playList(songs, startIndex = index, title = name) },
                    onMore = { menuEntryIndex = index },
                )
            }
        }
    }
    menuEntryIndex?.let { index ->
        val entry = entries.getOrNull(index)
        val song = songs.getOrNull(index)
        if (entry != null && song != null) {
            SongMenuSheet(
                song,
                onDismiss = { menuEntryIndex = null },
                onRemoveFromPlaylist = { playlist?.playlist?.let { actions.removeFromPlaylist(it, entry.map) } },
            )
        }
    }
    if (renaming) {
        RenamePlaylistDialog(
            currentName = name,
            onDismiss = { renaming = false },
            onRename = { newName -> playlist?.playlist?.let { actions.renamePlaylist(it, newName) } },
        )
    }
    if (deleting) {
        ConfirmDialog(
            title = "Eliminare \u201c$name\u201d?",
            text = "La playlist viene tolta dalla libreria del telefono. I brani scaricati restano.",
            confirmLabel = "Elimina",
            onDismiss = { deleting = false },
            onConfirm = {
                playlist?.playlist?.let(actions::deletePlaylist)
                navigator.back()
            },
        )
    }
}

@Composable
fun ArtistScreen(browseId: String) {
    val actions = LocalActions.current
    val navigator = LocalNavigator.current
    val (menuSong, setMenuSong) = rememberSongMenu()
    val page = rememberLoadable(browseId) { YouTube.artist(browseId) }
    val saved by remember(browseId) { actions.database.artist(browseId) }.collectAsState(null)

    DetailScaffold((page.value as? LoadState.Ready)?.value?.artist?.thumbnail) {
        when (val state = page.value) {
            LoadState.Loading -> LoadingBox(Modifier.padding(top = 120.dp))
            is LoadState.Failed -> ErrorBox(state.message, page::reload, Modifier.padding(top = 120.dp))
            is LoadState.Ready -> {
                val artistItem = state.value.artist
                val isFollowed = saved?.artist?.bookmarkedAt != null
                LazyColumn(Modifier.fillMaxSize(), contentPadding = detailPadding()) {
                    item(key = "header") {
                        DetailHeader(
                            artworkUrl = artistItem.thumbnail,
                            title = artistItem.title,
                            subtitle = null,
                            detail = artistItem.monthlyListenerCountText ?: artistItem.subscriberCountText,
                            artworkShape = com.justlorenzo.archivetunex.ui.components.ArtistShape,
                        ) {
                            androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                (artistItem.shuffleEndpoint ?: artistItem.playEndpoint)?.let { endpoint ->
                                    androidx.compose.material3.Button(onClick = { actions.playEndpoint(endpoint) }) { Text("Riproduci") }
                                }
                                artistItem.radioEndpoint?.let { endpoint ->
                                    FilledTonalButton(onClick = { actions.playEndpoint(endpoint) }) { Text("Radio") }
                                }
                                OutlinedButton(onClick = {
                                    actions.database.transaction {
                                        val existing = artist(browseId).first()?.artist
                                        val entity =
                                            existing ?: ArtistEntity(
                                                id = browseId,
                                                name = artistItem.title,
                                                thumbnailUrl = artistItem.thumbnail,
                                                channelId = artistItem.channelId,
                                            ).also { insert(it) }
                                        update(entity.toggleLike())
                                    }
                                }) { Text(if (isFollowed) "Segui già" else "Segui") }
                            }
                        }
                    }
                    state.value.sections.forEachIndexed { index, section ->
                        if (section.items.isEmpty()) return@forEachIndexed
                        if (section.items.all { it is SongItem }) {
                            item(key = "title-$index") { SectionHeader(section.title) }
                            val songs = section.items.map { (it as SongItem).toMediaMetadata() }
                            itemsIndexed(section.items, key = { i, _ -> "song-$index-$i" }) { i, item ->
                                val song = item as SongItem
                                SongRow(
                                    songId = song.id,
                                    title = song.title,
                                    subtitle = song.subtitle(),
                                    artworkUrl = song.thumbnail,
                                    explicit = song.explicit,
                                    onClick = { actions.playList(songs, startIndex = i, title = artistItem.title) },
                                    onMore = { setMenuSong(songs[i]) },
                                )
                            }
                        } else {
                            item(key = "carousel-$index") {
                                SectionHeader(section.title)
                                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp)) {
                                    items(section.items) { item ->
                                        ItemCard(item, onClick = { openItem(item, actions, navigator) })
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    menuSong?.let { SongMenuSheet(it, onDismiss = { setMenuSong(null) }) }
}
