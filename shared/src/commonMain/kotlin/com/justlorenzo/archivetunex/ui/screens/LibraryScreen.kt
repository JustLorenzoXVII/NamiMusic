@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.justlorenzo.archivetunex.ui.screens

import androidx.compose.material3.toShape
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.LaunchedEffect
import moe.rukamori.archivetune.innertube.models.YTItem
import androidx.compose.material3.MaterialShapes
import androidx.compose.foundation.border
import com.justlorenzo.archivetunex.ui.tr
import com.justlorenzo.archivetunex.ui.trf
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
import androidx.compose.foundation.layout.fillMaxHeight
import archivetunex.shared.generated.resources.navigate_next
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
import archivetunex.shared.generated.resources.grid_view
import archivetunex.shared.generated.resources.list
import archivetunex.shared.generated.resources.queue_music
import com.justlorenzo.archivetunex.ui.LocalActions
import com.justlorenzo.archivetunex.ui.LocalNavigator
import com.justlorenzo.archivetunex.ui.LibraryListViewKey
import moe.rukamori.archivetune.utils.rememberPreference
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
    private val rawLabel: String,
) {
    Playlists("Playlist"),
    Albums("Album"),
    Artists("Artisti");

    val label: String get() = tr(rawLabel)
}

/** Una card della griglia della libreria. */
private data class LibraryCard(
    val key: String,
    val title: String,
    val subtitle: String,
    val artworkUrl: String?,
    val round: Boolean = false,
    val isCreate: Boolean = false,
    val onClick: () -> Unit,
)

@Composable
fun LibraryScreen() {
    val actions = LocalActions.current
    val navigator = LocalNavigator.current
    val account = rememberAccount()
    var section by rememberSaveable { mutableStateOf(LibrarySection.Playlists) }
    var creating by remember { mutableStateOf(false) }
    var listView by rememberPreference(LibraryListViewKey, false)

    val downloadCount by remember {
        actions.downloads.downloads.map { map -> map.count { it.value.state == Download.STATE_COMPLETED } }.distinctUntilChanged()
    }.collectAsState(0)
    val likedCount by remember { actions.database.likedSongsCount() }.collectAsState(0)

    val playlists by remember { actions.database.playlistsByCreateDateAsc().map { it.reversed() } }.collectAsState(emptyList())
    val albums by remember { actions.database.albumsLikedByCreateDateAsc().map { it.reversed() } }.collectAsState(emptyList())
    val artists by remember { actions.database.artistsBookmarkedByCreateDateAsc().map { it.reversed() } }.collectAsState(emptyList())
    val remote =
        rememberLoadable(account?.name, cache = "library") {
            if (account == null) Result.success(emptyList()) else YouTube.library("FEmusic_liked_playlists").map { it.items }
        }

    // Ultimo elenco del tuo account: resta visibile mentre si aggiorna (niente sfarfallio)
    var remoteItems by remember { mutableStateOf<List<YTItem>>(emptyList()) }
    LaunchedEffect(remote.value) { (remote.value as? LoadState.Ready)?.value?.let { remoteItems = it } }
    // Aggiornamento automatico: quando crei, elimini o salvi una playlist si ricarica l'elenco del tuo account
    val playlistIds = remember(playlists) { playlists.map { it.playlist.id }.toSet() }
    var lastIds by remember { mutableStateOf<Set<String>?>(null) }
    LaunchedEffect(playlistIds) {
        val previous = lastIds
        lastIds = playlistIds
        if (previous != null && previous != playlistIds && account != null) remote.reload()
    }
    // Trascina verso il basso per aggiornare
    var pulling by remember { mutableStateOf(false) }
    LaunchedEffect(remote.value) { if (remote.value !is LoadState.Loading) pulling = false }

    val cards: List<LibraryCard> =
        when (section) {
            LibrarySection.Playlists -> {
                val local =
                    playlists.map { playlist ->
                        LibraryCard(
                            key = "pl-${playlist.id}",
                            title = playlist.playlist.name,
                            subtitle = trf("{0} brani", playlist.songCount),
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
                    remoteItems
                        .filter { item -> playlists.none { it.playlist.browseId == item.id } && item.id !in actions.deletedPlaylistIds }
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
                        subtitle = tr("Artista"),
                        artworkUrl = artist.artist.thumbnailUrl,
                        round = true,
                    ) { navigator.open(Route.Artist(artist.artist.id)) }
                }
        }

    val pullState = rememberPullToRefreshState()
    com.justlorenzo.archivetunex.ui.ios.AtxPullToRefreshBox(
        isRefreshing = pulling,
        onRefresh = {
            pulling = true
            if (account != null) remote.reload() else pulling = false
        },
        state = pullState,
        modifier = Modifier.fillMaxSize(),
    ) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = BottomContentPadding) {
        item(key = "header") {
            ScreenTitle(tr("Libreria"), overline = tr("La tua musica")) {
                AccountButton(account) { navigator.accountVisible = true }
            }
        }
        item(key = "pinned") {
            if (com.justlorenzo.archivetunex.ui.ios.iosStyle) {
                // Apple Music: elenco con icona d'accento, nome e freccetta
                IosLibraryLinks(
                    listOf(
                        Triple(tr("Scaricati"), Res.drawable.download, downloadCount) to { navigator.open(Route.Downloads) },
                        Triple(tr("Preferiti"), Res.drawable.favorite, likedCount) to { navigator.open(Route.Liked) },
                    ),
                )
                return@item
            }
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                PinnedTile(
                    title = tr("Scaricati"),
                    count = downloadCount,
                    icon = Res.drawable.download,
                    colors = listOf(Color(0xFFFF8FB8), Color(0xFF7A3B6E)),
                    modifier = Modifier.weight(1f),
                ) { navigator.open(Route.Downloads) }
                PinnedTile(
                    title = tr("Preferiti"),
                    count = likedCount,
                    icon = Res.drawable.favorite,
                    colors = listOf(Color(0xFF7B5CFF), Color(0xFF3A1F9E)),
                    modifier = Modifier.weight(1f),
                ) { navigator.open(Route.Liked) }
            }
        }
        item(key = "sections") {
            Row(verticalAlignment = Alignment.CenterVertically) {
            LazyRow(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(LibrarySection.entries) { entry ->
                    com.justlorenzo.archivetunex.ui.ios.AtxFilterChip(
                        selected = entry == section,
                        onClick = { section = entry },
                        label = { Text(entry.label, fontWeight = FontWeight.SemiBold) },
                    )
                }
            }
            // Griglia o elenco: la scelta resta salvata
            Box(
                Modifier
                    .padding(end = 16.dp)
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .clickable { listView = !listView },
                contentAlignment = Alignment.Center,
            ) {
                AtxIcon(
                    if (listView) Res.drawable.grid_view else Res.drawable.list,
                    if (listView) tr("Mostra a griglia") else tr("Mostra a elenco"),
                    size = 22.dp,
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
            }
        }
        if (cards.isEmpty() && section != LibrarySection.Playlists) {
            item(key = "empty-$section") {
                EmptyState(
                    when (section) {
                        LibrarySection.Playlists ->
                            if (account == null) {
                                tr("Crea una playlist con “Nuova”, o collega l'account YouTube per vedere le tue.")
                            } else {
                                tr("Nessuna playlist ancora. Creane una con “Nuova”.")
                            }
                        LibrarySection.Albums -> tr("Gli album che salvi in libreria appaiono qui.")
                        LibrarySection.Artists -> tr("Gli artisti che segui appaiono qui.")
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
        // Nelle playlist la prima card è sempre "Nuova playlist", ben visibile
        val shown =
            if (section == LibrarySection.Playlists) {
                listOf(LibraryCard(key = "new-playlist", title = tr("Nuova playlist"), subtitle = tr("Crea"), artworkUrl = null, isCreate = true) { creating = true }) + cards
            } else {
                cards
            }
        if (listView) cardList(shown) else cardGrid(shown)
    }
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

/** Elenco: una riga per elemento, copertina piccola a sinistra. */
private fun LazyListScope.cardList(cards: List<LibraryCard>) {
    items(cards, key = { "list-" + it.key }) { card -> ListCard(card) }
}

@Composable
private fun ListCard(card: LibraryCard) {
    val ios = com.justlorenzo.archivetunex.ui.ios.iosStyle
    val shape: Shape = if (card.round) ArtistShape else RoundedCornerShape(if (ios) 6.dp else 12.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = card.onClick)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (card.isCreate && ios) {
            Box(
                Modifier.size(56.dp).clip(shape).background(com.justlorenzo.archivetunex.ui.ios.iosFill),
                contentAlignment = Alignment.Center,
            ) { AtxIcon(Res.drawable.add, null, size = 26.dp, tint = MaterialTheme.colorScheme.primary) }
        } else if (card.isCreate) {
            Box(
                Modifier.size(64.dp).clip(shape).background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier.size(40.dp).clip(com.justlorenzo.archivetunex.ui.ios.iosShape(MaterialShapes.Cookie9Sided.toShape())).background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center,
                ) { AtxIcon(Res.drawable.add, null, size = 22.dp, tint = MaterialTheme.colorScheme.onPrimary) }
            }
        } else {
            Artwork(card.artworkUrl, Modifier.size(if (ios) 56.dp else 64.dp), shape, sizePx = 200)
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                card.title,
                style = if (ios) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.titleMedium,
                fontWeight = if (ios) FontWeight.Normal else FontWeight.Bold,
                color = if (card.isCreate) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                card.subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun GridCard(
    card: LibraryCard,
    modifier: Modifier = Modifier,
) {
    val ios = com.justlorenzo.archivetunex.ui.ios.iosStyle
    Column(
        if (ios) {
            modifier.clickable(interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, indication = null, onClick = card.onClick).padding(4.dp)
        } else {
            modifier.clip(RoundedCornerShape(24.dp)).clickable(onClick = card.onClick).padding(4.dp)
        },
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalAlignment = if (card.round) Alignment.CenterHorizontally else Alignment.Start,
    ) {
        val shape: Shape = if (card.round) ArtistShape else RoundedCornerShape(com.justlorenzo.archivetunex.ui.LocalAtxStyle.current.corners.radius)
        if (card.isCreate && ios) {
            Box(
                Modifier.fillMaxWidth().aspectRatio(1f).clip(shape).background(com.justlorenzo.archivetunex.ui.ios.iosFill),
                contentAlignment = Alignment.Center,
            ) { AtxIcon(Res.drawable.add, null, size = 40.dp, tint = MaterialTheme.colorScheme.primary) }
        } else if (card.isCreate) {
            // Riquadro per creare una playlist: tinta del colore principale e un grande "+" a biscotto
            Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(shape)
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), shape),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier.size(64.dp).clip(com.justlorenzo.archivetunex.ui.ios.iosShape(MaterialShapes.Cookie9Sided.toShape())).background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center,
                ) { AtxIcon(Res.drawable.add, null, size = 32.dp, tint = MaterialTheme.colorScheme.onPrimary) }
            }
        } else {
            Artwork(card.artworkUrl, Modifier.fillMaxWidth().aspectRatio(1f), shape, sizePx = 400)
        }
        Text(
            card.title,
            style = if (ios) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.titleSmall,
            fontWeight = if (ios) FontWeight.Medium else FontWeight.Bold,
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
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = com.justlorenzo.archivetunex.ui.ios.heavyWeight, color = Color.White, letterSpacing = (-0.3).sp)
            Text(trf("{0} brani", count), style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.85f))
        }
    }
}

/** Le voci in cima alla Libreria di Apple Music: icona d'accento, nome, conteggio e freccetta. */
@Composable
private fun IosLibraryLinks(links: List<Pair<Triple<String, DrawableResource, Int>, () -> Unit>>) {
    Column(Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 8.dp)) {
        links.forEach { (info, onClick) ->
            val (title, icon, count) = info
            Row(
                Modifier.fillMaxWidth().height(50.dp).clickable(onClick = onClick).padding(start = 20.dp, end = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AtxIcon(icon, null, size = 22.dp, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(16.dp))
                Box(Modifier.weight(1f).fillMaxHeight()) {
                    Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                        Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                        if (count > 0) Text(count.toString(), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(8.dp))
                        AtxIcon(Res.drawable.navigate_next, null, size = 15.dp, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                    }
                    Box(Modifier.align(Alignment.BottomStart).fillMaxWidth().height(0.5.dp).background(MaterialTheme.colorScheme.outlineVariant))
                }
            }
        }
    }
}
