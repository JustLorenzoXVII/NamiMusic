package com.justlorenzo.archivetunex.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import archivetunex.shared.generated.resources.Res
import archivetunex.shared.generated.resources.add
import archivetunex.shared.generated.resources.album
import archivetunex.shared.generated.resources.artist
import archivetunex.shared.generated.resources.download
import archivetunex.shared.generated.resources.favorite
import archivetunex.shared.generated.resources.queue_music
import com.justlorenzo.archivetunex.ui.LocalActions
import com.justlorenzo.archivetunex.ui.LocalNavigator
import com.justlorenzo.archivetunex.ui.Route
import com.justlorenzo.archivetunex.ui.components.AccountButton
import com.justlorenzo.archivetunex.ui.components.ArtistShape
import com.justlorenzo.archivetunex.ui.components.Artwork
import com.justlorenzo.archivetunex.ui.components.AtxIcon
import com.justlorenzo.archivetunex.ui.components.BottomContentPadding
import com.justlorenzo.archivetunex.ui.components.CreatePlaylistDialog
import com.justlorenzo.archivetunex.ui.components.EmptyState
import com.justlorenzo.archivetunex.ui.components.LoadState
import com.justlorenzo.archivetunex.ui.components.ScreenTitle
import com.justlorenzo.archivetunex.ui.components.rememberAccount
import com.justlorenzo.archivetunex.ui.components.rememberLoadable
import com.justlorenzo.archivetunex.ui.components.subtitle
import com.justlorenzo.archivetunex.ui.openItem
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import moe.rukamori.archivetune.compat.media3.Download
import moe.rukamori.archivetune.innertube.YouTube
import org.jetbrains.compose.resources.DrawableResource

private enum class LibrarySection(
    val label: String,
) {
    Playlists("Playlist"),
    Albums("Album"),
    Artists("Artisti"),
}

/** Una card della griglia della libreria. */
private data class LibraryCard(
    val key: String,
    val title: String,
    val subtitle: String,
    val artworkUrl: String?,
    val round: Boolean = false,
    val onClick: () -> Unit,
)

@Composable
fun LibraryScreen() {
    val actions = LocalActions.current
    val navigator = LocalNavigator.current
    val account = rememberAccount()
    var section by rememberSaveable { mutableStateOf(LibrarySection.Playlists) }
    var creating by remember { mutableStateOf(false) }

    val downloadCount by remember {
        actions.downloads.downloads.map { map -> map.count { it.value.state == Download.STATE_COMPLETED } }.distinctUntilChanged()
    }.collectAsState(0)
    val likedCount by remember { actions.database.likedSongsCount() }.collectAsState(0)

    val playlists by remember { actions.database.playlistsByCreateDateAsc().map { it.reversed() } }.collectAsState(emptyList())
    val albums by remember { actions.database.albumsLikedByCreateDateAsc().map { it.reversed() } }.collectAsState(emptyList())
    val artists by remember { actions.database.artistsBookmarkedByCreateDateAsc().map { it.reversed() } }.collectAsState(emptyList())
    val remote =
        rememberLoadable(account?.name) {
            if (account == null) Result.success(emptyList()) else YouTube.library("FEmusic_liked_playlists").map { it.items }
        }

    val cards: List<LibraryCard> =
        when (section) {
            LibrarySection.Playlists -> {
                val local =
                    playlists.map { playlist ->
                        LibraryCard(
                            key = "pl-${playlist.id}",
                            title = playlist.playlist.name,
                            subtitle = "${playlist.songCount} brani",
                            artworkUrl = playlist.thumbnails.firstOrNull() ?: playlist.playlist.thumbnailUrl,
                        ) {
                            val browseId = playlist.playlist.browseId
                            // Le tue playlist si aprono dal telefono (modificabili), le altre da YouTube Music
                            if (browseId != null && !playlist.playlist.isEditable) {
                                navigator.open(Route.Playlist(browseId))
                            } else {
                                navigator.open(Route.LocalPlaylist(playlist.playlist.id, playlist.playlist.name))
                            }
                        }
                    }
                val fromAccount =
                    (remote.value as? LoadState.Ready)?.value.orEmpty()
                        .filter { item -> playlists.none { it.playlist.browseId == item.id } }
                        .map { item ->
                            LibraryCard("yt-${item.id}", item.title, item.subtitle(), item.thumbnail) { openItem(item, actions, navigator) }
                        }
                local + fromAccount
            }
            LibrarySection.Albums ->
                albums.map { album ->
                    LibraryCard(
                        key = "al-${album.album.id}",
                        title = album.album.title,
                        subtitle = listOfNotNull(album.artists.joinToString { it.name }.takeIf(String::isNotBlank), album.album.year?.toString()).joinToString(" · "),
                        artworkUrl = album.album.thumbnailUrl,
                    ) { navigator.open(Route.Album(album.album.id)) }
                }
            LibrarySection.Artists ->
                artists.map { artist ->
                    LibraryCard(
                        key = "ar-${artist.artist.id}",
                        title = artist.artist.name,
                        subtitle = "Artista",
                        artworkUrl = artist.artist.thumbnailUrl,
                        round = true,
                    ) { navigator.open(Route.Artist(artist.artist.id)) }
                }
        }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = BottomContentPadding) {
        item(key = "header") {
            ScreenTitle("Libreria", overline = "La tua musica") {
                AccountButton(account) { navigator.accountVisible = true }
            }
        }
        item(key = "pinned") {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                PinnedTile(
                    title = "Scaricati",
                    count = downloadCount,
                    icon = Res.drawable.download,
                    colors = listOf(Color(0xFFFF8FB8), Color(0xFF7A3B6E)),
                    modifier = Modifier.weight(1f),
                ) { navigator.open(Route.Downloads) }
                PinnedTile(
                    title = "Preferiti",
                    count = likedCount,
                    icon = Res.drawable.favorite,
                    colors = listOf(Color(0xFF7B5CFF), Color(0xFF3A1F9E)),
                    modifier = Modifier.weight(1f),
                ) { navigator.open(Route.Liked) }
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
                        label = { Text(entry.label, fontWeight = FontWeight.SemiBold) },
                        shape = RoundedCornerShape(20.dp),
                        colors =
                            FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            ),
                    )
                }
                if (section == LibrarySection.Playlists) {
                    item {
                        FilterChip(
                            selected = false,
                            onClick = { creating = true },
                            label = { Text("Nuova", fontWeight = FontWeight.SemiBold) },
                            leadingIcon = { AtxIcon(Res.drawable.add, null, size = 18.dp) },
                            shape = RoundedCornerShape(20.dp),
                        )
                    }
                }
            }
        }
        if (cards.isEmpty()) {
            item(key = "empty-$section") {
                EmptyState(
                    when (section) {
                        LibrarySection.Playlists ->
                            if (account == null) {
                                "Crea una playlist con “Nuova”, o collega l'account YouTube per vedere le tue."
                            } else {
                                "Nessuna playlist ancora. Creane una con “Nuova”."
                            }
                        LibrarySection.Albums -> "Gli album che salvi in libreria appaiono qui."
                        LibrarySection.Artists -> "Gli artisti che segui appaiono qui."
                    },
                    icon =
                        when (section) {
                            LibrarySection.Playlists -> Res.drawable.queue_music
                            LibrarySection.Albums -> Res.drawable.album
                            LibrarySection.Artists -> Res.drawable.artist
                        },
                )
            }
        }
        cardGrid(cards)
    }

    if (creating) CreatePlaylistDialog(onDismiss = { creating = false })
}

/** Griglia a due colonne dentro la lista. */
private fun LazyListScope.cardGrid(cards: List<LibraryCard>) {
    val rows = cards.chunked(2)
    itemsIndexed(rows, key = { _, row -> row.joinToString("|") { it.key } }) { _, row ->
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            row.forEach { card -> GridCard(card, Modifier.weight(1f)) }
            if (row.size == 1) Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
private fun GridCard(
    card: LibraryCard,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier.clip(RoundedCornerShape(24.dp)).clickable(onClick = card.onClick).padding(4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalAlignment = if (card.round) Alignment.CenterHorizontally else Alignment.Start,
    ) {
        val shape: Shape = if (card.round) ArtistShape else RoundedCornerShape(com.justlorenzo.archivetunex.ui.LocalAtxStyle.current.corners.radius)
        Artwork(card.artworkUrl, Modifier.fillMaxWidth().aspectRatio(1f), shape, sizePx = 400)
        Text(
            card.title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        Text(
            card.subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
    }
}

/** Riquadro colorato in cima alla libreria (Scaricati, Preferiti). */
@Composable
private fun PinnedTile(
    title: String,
    count: Int,
    icon: DrawableResource,
    colors: List<Color>,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier
            .height(132.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(Brush.linearGradient(colors))
            .clickable(onClick = onClick)
            .padding(16.dp),
    ) {
        Box(
            Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(Color.White.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center,
        ) { AtxIcon(icon, null, tint = Color.White) }
        Column(Modifier.align(Alignment.BottomStart)) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = Color.White, letterSpacing = (-0.3).sp)
            Text("$count brani", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.85f))
        }
    }
}
