package com.justlorenzo.archivetunex.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import archivetunex.shared.generated.resources.Res
import archivetunex.shared.generated.resources.album
import archivetunex.shared.generated.resources.artist
import archivetunex.shared.generated.resources.download
import archivetunex.shared.generated.resources.favorite
import archivetunex.shared.generated.resources.queue_music
import com.justlorenzo.archivetunex.ui.LocalActions
import com.justlorenzo.archivetunex.ui.LocalNavigator
import com.justlorenzo.archivetunex.ui.Route
import com.justlorenzo.archivetunex.ui.components.AccountButton
import com.justlorenzo.archivetunex.ui.components.Artwork
import com.justlorenzo.archivetunex.ui.components.BottomContentPadding
import com.justlorenzo.archivetunex.ui.components.EmptyState
import com.justlorenzo.archivetunex.ui.components.LoadState
import com.justlorenzo.archivetunex.ui.components.PlayShuffleButtons
import com.justlorenzo.archivetunex.ui.components.SectionHeader
import com.justlorenzo.archivetunex.ui.components.SongMenuSheet
import com.justlorenzo.archivetunex.ui.components.SongRow
import com.justlorenzo.archivetunex.ui.components.rememberAccount
import com.justlorenzo.archivetunex.ui.components.rememberLoadable
import com.justlorenzo.archivetunex.ui.components.subtitle
import com.justlorenzo.archivetunex.ui.openItem
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import moe.rukamori.archivetune.compat.media3.Download
import moe.rukamori.archivetune.db.entities.Song
import moe.rukamori.archivetune.innertube.YouTube
import moe.rukamori.archivetune.models.MediaMetadata
import moe.rukamori.archivetune.models.toMediaMetadata

private enum class LibrarySection(
    val label: String,
) {
    Downloads("Scaricati"),
    Liked("Preferiti"),
    Playlists("Playlist"),
    Albums("Album"),
    Artists("Artisti"),
}

@Composable
fun LibraryScreen() {
    val actions = LocalActions.current
    val navigator = LocalNavigator.current
    val account = rememberAccount()
    var section by rememberSaveable { mutableStateOf(LibrarySection.Downloads) }
    var menuSong by remember { mutableStateOf<MediaMetadata?>(null) }

    val downloadedSongs by remember {
        actions.downloads.downloads
            .map { map -> map.filterValues { it.state != Download.STATE_FAILED }.keys.toList() }
            .distinctUntilChanged()
            .flatMapLatest { ids -> actions.database.songsByIds(ids) }
    }.collectAsState(emptyList())
    val likedSongs by remember { actions.database.likedSongsByCreateDateAsc().map { it.reversed() } }.collectAsState(emptyList())

    LazyColumn(Modifier.fillMaxSize(), contentPadding = BottomContentPadding) {
        item(key = "header") {
            Row(
                Modifier.fillMaxWidth().statusBarsPadding().padding(start = 20.dp, end = 8.dp, top = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Libreria",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                AccountButton(account) { navigator.accountVisible = true }
            }
        }
        item(key = "sections") {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(LibrarySection.entries) { entry ->
                    FilterChip(
                        selected = entry == section,
                        onClick = { section = entry },
                        label = { Text(entry.label) },
                        shape = RoundedCornerShape(18.dp),
                    )
                }
            }
        }
        when (section) {
            LibrarySection.Downloads ->
                songList(
                    key = "downloads",
                    songs = downloadedSongs,
                    title = "Scaricati",
                    emptyText = "I brani che scarichi appaiono qui e si ascoltano anche senza connessione.",
                    emptyIcon = Res.drawable.download,
                    onMenu = { menuSong = it },
                )
            LibrarySection.Liked ->
                songList(
                    key = "liked",
                    songs = likedSongs,
                    title = "Preferiti",
                    emptyText = "Tocca il cuore nel player per aggiungere un brano ai preferiti.",
                    emptyIcon = Res.drawable.favorite,
                    onMenu = { menuSong = it },
                )
            LibrarySection.Playlists -> playlistsSection(accountName = account?.name)
            LibrarySection.Albums -> albumsSection()
            LibrarySection.Artists -> artistsSection()
        }
    }

    menuSong?.let { song -> SongMenuSheet(song, onDismiss = { menuSong = null }) }
}

/** Lista di brani del database con Riproduci / Casuale in cima. */
private fun LazyListScope.songList(
    key: String,
    songs: List<Song>,
    title: String,
    emptyText: String,
    emptyIcon: org.jetbrains.compose.resources.DrawableResource,
    onMenu: (MediaMetadata) -> Unit,
) {
    if (songs.isEmpty()) {
        item(key = "$key-empty") { EmptyState(emptyText, icon = emptyIcon) }
        return
    }
    item(key = "$key-buttons") {
        val actions = LocalActions.current
        val metadata = remember(songs) { songs.map { it.toMediaMetadata() } }
        Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                "${songs.size} brani",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            PlayShuffleButtons(
                onPlay = { actions.playList(metadata, title = title) },
                onShuffle = { actions.playList(metadata, title = title, shuffle = true) },
            )
        }
    }
    itemsIndexed(songs, key = { _, song -> "$key-${song.id}" }) { index, song ->
        val actions = LocalActions.current
        val metadata = remember(song) { song.toMediaMetadata() }
        SongRow(
            songId = song.id,
            title = song.title,
            subtitle = metadata.artists.joinToString { it.name },
            artworkUrl = song.thumbnailUrl,
            explicit = song.song.explicit,
            onClick = { actions.playList(songs.map { it.toMediaMetadata() }, startIndex = index, title = title) },
            onMore = { onMenu(metadata) },
        )
    }
}

private fun LazyListScope.playlistsSection(accountName: String?) {
    item(key = "playlists") {
        val actions = LocalActions.current
        val navigator = LocalNavigator.current
        val local by remember { actions.database.playlistsByCreateDateAsc().map { it.reversed() } }.collectAsState(emptyList())
        val remote = rememberLoadable(accountName) {
            if (accountName == null) Result.success(emptyList()) else YouTube.library("FEmusic_liked_playlists").map { it.items }
        }
        Column {
            local.forEach { playlist ->
                LibraryRow(
                    title = playlist.playlist.name,
                    subtitle = "Playlist · ${playlist.songCount} brani",
                    artworkUrl = playlist.thumbnails.firstOrNull() ?: playlist.playlist.thumbnailUrl,
                    onClick = {
                        val browseId = playlist.playlist.browseId
                        if (browseId != null) {
                            navigator.open(Route.Playlist(browseId))
                        } else {
                            navigator.open(Route.LocalPlaylist(playlist.playlist.id, playlist.playlist.name))
                        }
                    },
                )
            }
            val remoteItems = (remote.value as? LoadState.Ready)?.value.orEmpty()
                .filter { item -> local.none { it.playlist.browseId == item.id } }
            if (remoteItems.isNotEmpty()) {
                SectionHeader("Dal tuo account YouTube")
                remoteItems.forEach { item ->
                    LibraryRow(
                        title = item.title,
                        subtitle = item.subtitle(),
                        artworkUrl = item.thumbnail,
                        onClick = { openItem(item, actions, navigator) },
                    )
                }
            }
            if (local.isEmpty() && remoteItems.isEmpty()) {
                EmptyState(
                    if (accountName == null) {
                        "Le tue playlist appaiono qui. Collega l'account YouTube per vedere anche quelle salvate su YouTube Music."
                    } else {
                        "Nessuna playlist ancora."
                    },
                    icon = Res.drawable.queue_music,
                )
            }
        }
    }
}

private fun LazyListScope.albumsSection() {
    item(key = "albums") {
        val actions = LocalActions.current
        val navigator = LocalNavigator.current
        val albums by remember { actions.database.albumsLikedByCreateDateAsc().map { it.reversed() } }.collectAsState(emptyList())
        Column {
            if (albums.isEmpty()) EmptyState("Gli album che salvi appaiono qui.", icon = Res.drawable.album)
            albums.forEach { album ->
                LibraryRow(
                    title = album.album.title,
                    subtitle = listOfNotNull(album.artists.joinToString { it.name }.takeIf(String::isNotBlank), album.album.year?.toString()).joinToString(" · "),
                    artworkUrl = album.album.thumbnailUrl,
                    onClick = { navigator.open(Route.Album(album.album.id)) },
                )
            }
        }
    }
}

private fun LazyListScope.artistsSection() {
    item(key = "artists") {
        val actions = LocalActions.current
        val navigator = LocalNavigator.current
        val artists by remember { actions.database.artistsBookmarkedByCreateDateAsc().map { it.reversed() } }.collectAsState(emptyList())
        Column {
            if (artists.isEmpty()) EmptyState("Gli artisti che segui appaiono qui.", icon = Res.drawable.artist)
            artists.forEach { artist ->
                LibraryRow(
                    title = artist.artist.name,
                    subtitle = "${artist.songCount} brani",
                    artworkUrl = artist.artist.thumbnailUrl,
                    shape = CircleShape,
                    onClick = { navigator.open(Route.Artist(artist.artist.id)) },
                )
            }
        }
    }
}

@Composable
private fun LibraryRow(
    title: String,
    subtitle: String,
    artworkUrl: String?,
    onClick: () -> Unit,
    shape: Shape = MaterialTheme.shapes.small,
) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Artwork(artworkUrl, Modifier.size(56.dp), shape = shape, sizePx = 226)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
