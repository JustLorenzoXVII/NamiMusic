@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.justlorenzo.archivetunex.ui.screens

import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material3.toShape
import androidx.compose.material3.MaterialShapes
import com.justlorenzo.archivetunex.ui.tr
import com.justlorenzo.archivetunex.ui.trf
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.height
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.compositeOver
import com.justlorenzo.archivetunex.ui.Route
import com.justlorenzo.archivetunex.ui.components.ScreenTitle
import com.justlorenzo.archivetunex.ui.components.SectionHeader
import com.justlorenzo.archivetunex.ui.components.readableOn
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import archivetunex.shared.generated.resources.Res
import archivetunex.shared.generated.resources.navigate_next
import archivetunex.shared.generated.resources.arrow_top_left
import archivetunex.shared.generated.resources.close
import archivetunex.shared.generated.resources.history
import archivetunex.shared.generated.resources.search
import com.justlorenzo.archivetunex.ui.LocalActions
import com.justlorenzo.archivetunex.ui.LocalNavigator
import com.justlorenzo.archivetunex.ui.components.Artwork
import com.justlorenzo.archivetunex.ui.components.AtxIcon
import com.justlorenzo.archivetunex.ui.components.BottomContentPadding
import com.justlorenzo.archivetunex.ui.components.EmptyState
import com.justlorenzo.archivetunex.ui.components.ErrorBox
import com.justlorenzo.archivetunex.ui.components.LoadState
import com.justlorenzo.archivetunex.ui.components.LoadingBox
import com.justlorenzo.archivetunex.ui.components.SongMenuSheet
import com.justlorenzo.archivetunex.ui.components.SongRow
import com.justlorenzo.archivetunex.ui.components.rememberLoadable
import com.justlorenzo.archivetunex.ui.components.subtitle
import com.justlorenzo.archivetunex.ui.openItem
import kotlinx.coroutines.delay
import moe.rukamori.archivetune.db.entities.SearchHistory
import moe.rukamori.archivetune.innertube.YouTube.SearchFilter
import moe.rukamori.archivetune.innertube.YouTube
import kotlinx.coroutines.async
import moe.rukamori.archivetune.innertube.models.ArtistItem
import moe.rukamori.archivetune.innertube.models.SongItem
import moe.rukamori.archivetune.innertube.models.YTItem
import moe.rukamori.archivetune.models.MediaMetadata
import moe.rukamori.archivetune.models.toMediaMetadata

private enum class SearchKind(
    private val rawLabel: String,
    val filter: SearchFilter?,
) {
    // Tutto insieme, come Apple Music: artisti, album, brani e playlist più rilevanti
    Top("Migliori risultati", null),
    Artists("Artisti", SearchFilter.FILTER_ARTIST),
    Albums("Album", SearchFilter.FILTER_ALBUM),
    Songs("Brani", SearchFilter.FILTER_SONG),
    Playlists("Playlist", SearchFilter.FILTER_FEATURED_PLAYLIST),
    CommunityPlaylists("Playlist utenti", SearchFilter.FILTER_COMMUNITY_PLAYLIST),
    Videos("Video", SearchFilter.FILTER_VIDEO);

    val label: String get() = tr(rawLabel)
}

@Composable
fun SearchScreen() {
    val actions = LocalActions.current
    val navigator = LocalNavigator.current
    val focusManager = LocalFocusManager.current

    var text by rememberSaveable { mutableStateOf("") }
    var submitted by rememberSaveable { mutableStateOf("") }
    var kind by rememberSaveable { mutableStateOf(SearchKind.Top) }
    var menuSong by remember { mutableStateOf<MediaMetadata?>(null) }

    val history by remember { actions.database.searchHistory() }.collectAsState(emptyList())

    fun submit(query: String) {
        val q = query.trim()
        if (q.isEmpty()) return
        text = q
        submitted = q
        focusManager.clearFocus()
        actions.database.query { insert(SearchHistory(query = q)) }
    }

    // Suggerimenti mentre si scrive (solo finché non si è premuto Cerca)
    var suggestions by remember { mutableStateOf<List<String>>(emptyList()) }
    LaunchedEffect(text, submitted) {
        if (text.isBlank() || text == submitted) {
            suggestions = emptyList()
            return@LaunchedEffect
        }
        delay(250)
        suggestions = YouTube.searchSuggestions(text).getOrNull()?.queries.orEmpty().take(8)
    }

    Column(Modifier.fillMaxSize()) {
        ScreenTitle(tr("Cerca"), overline = "YouTube Music")
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(top = 8.dp, bottom = 4.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            com.justlorenzo.archivetunex.ui.ios.AtxSearchField(
                value = text,
                onValueChange = {
                    text = it
                    if (it.isBlank()) submitted = ""
                },
                placeholder = tr("Brani, artisti, album, playlist"),
                onSearch = { submit(text) },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        when {
            suggestions.isNotEmpty() ->
                LazyColumn(contentPadding = BottomContentPadding, modifier = Modifier.fillMaxSize()) {
                    items(suggestions) { suggestion ->
                        QueryRow(suggestion, Res.drawable.search, onClick = { submit(suggestion) }, onFill = { text = suggestion })
                    }
                }

            submitted.isNotEmpty() ->
                SearchResults(
                    query = submitted,
                    kind = kind,
                    onKindChange = { kind = it },
                    onSongMenu = { menuSong = it },
                    onOpen = { openItem(it, actions, navigator) },
                )

            else ->
                SearchHome(
                    history = history.take(6),
                    onQuery = { submit(it) },
                    onFill = { text = it },
                    onClearHistory = { actions.database.query { clearSearchHistory() } },
                )
        }
    }

    menuSong?.let { song -> SongMenuSheet(song, onDismiss = { menuSong = null }) }
}

@Composable
private fun QueryRow(
    text: String,
    icon: org.jetbrains.compose.resources.DrawableResource,
    onClick: () -> Unit,
    onFill: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(start = 20.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        AtxIcon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        IconButton(onClick = onFill) {
            AtxIcon(Res.drawable.arrow_top_left, tr("Modifica la ricerca"), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SearchResults(
    query: String,
    kind: SearchKind,
    onKindChange: (SearchKind) -> Unit,
    onSongMenu: (MediaMetadata) -> Unit,
    onOpen: (YTItem) -> Unit,
) {
    val results =
        rememberLoadable(query to kind, cache = "search-v3") {
            val filter = kind.filter
            if (filter == null) {
                // Migliori risultati: le sezioni di YouTube Music in una lista sola, poi le playlist
                // ufficiali e quelle degli utenti legate alla ricerca (tre richieste in parallelo)
                kotlinx.coroutines.coroutineScope {
                    val summary = async { YouTube.searchSummary(query) }
                    val featured = async { YouTube.search(query, SearchFilter.FILTER_FEATURED_PLAYLIST).getOrNull()?.items.orEmpty() }
                    val community = async { YouTube.search(query, SearchFilter.FILTER_COMMUNITY_PLAYLIST).getOrNull()?.items.orEmpty() }
                    summary.await().map { page ->
                        val seen = mutableSetOf<String>()
                        val rows = mutableListOf<SearchRow>()
                        page.summaries.flatMap { it.items }.forEach { if (seen.add(it.id)) rows += SearchRow.Entry(it) }
                        val official = featured.await().filter { seen.add(it.id) }.take(6)
                        if (official.isNotEmpty()) {
                            rows += SearchRow.Header(tr("Playlist in evidenza"))
                            official.forEach { rows += SearchRow.Entry(it) }
                        }
                        val users = community.await().filter { seen.add(it.id) }.take(6)
                        if (users.isNotEmpty()) {
                            rows += SearchRow.Header(tr("Playlist degli utenti"))
                            users.forEach { rows += SearchRow.Entry(it) }
                        }
                        rows.toList()
                    }
                }
            } else {
                YouTube.search(query, filter).map { result -> result.items.map { SearchRow.Entry(it) } }
            }
        }

    LazyColumn(contentPadding = BottomContentPadding, modifier = Modifier.fillMaxSize()) {
        item(key = "filters") {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(SearchKind.entries) { entry ->
                    com.justlorenzo.archivetunex.ui.ios.AtxFilterChip(
                        selected = entry == kind,
                        onClick = { onKindChange(entry) },
                        label = { Text(entry.label, fontWeight = FontWeight.SemiBold) },
                    )
                }
            }
        }
        when (val state = results.value) {
            LoadState.Loading -> item(key = "loading") { LoadingBox() }
            is LoadState.Failed -> item(key = "error") { ErrorBox(state.message, results::reload) }
            is LoadState.Ready -> {
                if (state.value.isEmpty()) {
                    item(key = "empty") { EmptyState(trf("Nessun risultato per “{0}”.", query), icon = Res.drawable.search) }
                }
                val showType = kind == SearchKind.Top
                items(state.value) { row ->
                    if (row is SearchRow.Header) {
                        Text(
                            row.title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = com.justlorenzo.archivetunex.ui.ios.extraBoldWeight,
                            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 6.dp),
                        )
                        return@items
                    }
                    val item = (row as SearchRow.Entry).item
                    if (item is SongItem) {
                        val metadata = remember(item) { item.toMediaMetadata() }
                        SongRow(
                            songId = item.id,
                            title = item.title,
                            // Nei migliori risultati si dice cos'è (Brano · artista), come Apple Music
                            subtitle = if (showType) listOf(tr("Brano"), item.artists.joinToString { it.name }).filter { it.isNotBlank() }.joinToString(" · ") else item.subtitle(),
                            artworkUrl = item.thumbnail,
                            explicit = item.explicit,
                            onClick = { onOpen(item) },
                            onMore = { onSongMenu(metadata) },
                        )
                    } else {
                        ItemRow(item, showType = showType, onClick = { onOpen(item) })
                    }
                }
            }
        }
    }
}

/** Una riga dei risultati: un titolo di sezione o un elemento. */
private sealed interface SearchRow {
    data class Header(val title: String) : SearchRow

    data class Entry(val item: YTItem) : SearchRow
}

/** Riga per album, artisti e playlist nei risultati. */
@Composable
private fun ItemRow(
    item: YTItem,
    showType: Boolean = false,
    onClick: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Artwork(
            item.thumbnail,
            Modifier.size(56.dp),
            shape = if (item is ArtistItem) CircleShape else MaterialTheme.shapes.small,
            sizePx = 226,
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                item.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                if (showType) typedSubtitle(item) else item.subtitle(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        // Freccia: si apre una pagina
        AtxIcon(Res.drawable.navigate_next, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** "Album · Eminem", "Artista", "Playlist · YouTube Music"… */
private fun typedSubtitle(item: YTItem): String {
    val type = itemTypeLabel(item)
    val sub = item.subtitle()
    return when {
        item is ArtistItem -> type
        type.isBlank() -> sub
        sub.startsWith(type) -> sub
        sub.isBlank() -> type
        else -> "$type · $sub"
    }
}

/** Cos'è un risultato (Artista, Album, Playlist…). */
private fun itemTypeLabel(item: YTItem): String =
    when (item) {
        is ArtistItem -> tr("Artista")
        is moe.rukamori.archivetune.innertube.models.AlbumItem -> tr("Album")
        is moe.rukamori.archivetune.innertube.models.PlaylistItem -> tr("Playlist")
        is moe.rukamori.archivetune.innertube.models.PodcastItem -> tr("Podcast")
        is moe.rukamori.archivetune.innertube.models.EpisodeItem -> tr("Episodio")
        else -> ""
    }

/** Cerca senza testo: ricerche recenti e i generi/mood di YouTube Music, colorati. */
@Composable
private fun SearchHome(
    history: List<SearchHistory>,
    onQuery: (String) -> Unit,
    onFill: (String) -> Unit,
    onClearHistory: () -> Unit,
) {
    val navigator = LocalNavigator.current
    val moods = rememberLoadable(Unit, cache = "moods", maxAgeMs = 6 * 60 * 60 * 1000L) { YouTube.moodAndGenres() }

    LazyColumn(contentPadding = BottomContentPadding, modifier = Modifier.fillMaxSize()) {
        if (history.isNotEmpty()) {
            item(key = "history-title") {
                Row(
                    Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        tr("Ricerche recenti"),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = com.justlorenzo.archivetunex.ui.ios.extraBoldWeight,
                        modifier = Modifier.weight(1f),
                    )
                    com.justlorenzo.archivetunex.ui.ios.AtxTextButton(onClick = onClearHistory) { Text(tr("Cancella")) }
                }
            }
            items(history, key = { it.id }) { entry ->
                QueryRow(entry.query, Res.drawable.history, onClick = { onQuery(entry.query) }, onFill = { onFill(entry.query) })
            }
        }
        when (val state = moods.value) {
            LoadState.Loading -> item(key = "moods-loading") { LoadingBox() }
            is LoadState.Failed -> Unit
            is LoadState.Ready ->
                state.value.forEachIndexed { index, group ->
                    item(key = "moods-title-$index") { SectionHeader(group.title) }
                    val rows = group.items.chunked(2)
                    items(rows.size, key = { "moods-$index-$it" }) { rowIndex ->
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 5.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            rows[rowIndex].forEach { mood ->
                                MoodTile(
                                    title = mood.title,
                                    color = Color(mood.stripeColor),
                                    modifier = Modifier.weight(1f),
                                    onClick = { navigator.open(Route.Browse(mood.endpoint.browseId, mood.endpoint.params, mood.title)) },
                                )
                            }
                            if (rows[rowIndex].size == 1) androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
                        }
                    }
                }
        }
    }
}

/** Le forme Expressive usate come decorazione dei riquadri dei generi. */
private val MoodShapes =
    listOf(
        MaterialShapes.Cookie9Sided,
        MaterialShapes.Clover4Leaf,
        MaterialShapes.Sunny,
        MaterialShapes.Flower,
        MaterialShapes.Gem,
        MaterialShapes.SoftBurst,
        MaterialShapes.Puffy,
        MaterialShapes.Cookie6Sided,
        MaterialShapes.Pentagon,
        MaterialShapes.Burst,
    )

/**
 * Riquadro di un genere: colore di YouTube Music con sfumatura, una grande forma Expressive
 * che spunta dall'angolo (diversa per ogni genere), angoli asimmetrici e un rimbalzo quando lo premi.
 */
@Composable
private fun MoodTile(
    title: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val base = color.copy(alpha = 1f)
    val seed = remember(title) { title.fold(7) { acc, c -> acc * 31 + c.code }.let { if (it < 0) -it else it } }
    val shape = MoodShapes[seed % MoodShapes.size].toShape()
    val rotation = (seed % 90).toFloat()
    val content = base.readableOn()
    val source = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by androidx.compose.animation.core.animateFloatAsState(
        if (pressed) 0.94f else 1f,
        androidx.compose.animation.core.spring(dampingRatio = 0.5f, stiffness = 600f),
        label = "moodScale",
    )
    val ios = com.justlorenzo.archivetunex.ui.ios.iosStyle
    // Angoli: grandi da una parte e piccoli dall'altra, alternati da un genere all'altro
    // (tema iOS: riquadro semplice con angoli piccoli, come le categorie di Apple Music)
    val tileShape =
        if (ios) {
            RoundedCornerShape(12.dp)
        } else if (seed % 2 == 0) {
            RoundedCornerShape(topStart = 30.dp, topEnd = 10.dp, bottomStart = 10.dp, bottomEnd = 30.dp)
        } else {
            RoundedCornerShape(topStart = 10.dp, topEnd = 30.dp, bottomStart = 30.dp, bottomEnd = 10.dp)
        }
    androidx.compose.foundation.layout.Box(
        modifier
            .height(96.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }.clip(tileShape)
            .background(
                androidx.compose.ui.graphics.Brush.linearGradient(
                    if (ios) listOf(base, base) else listOf(base, base.copy(alpha = 0.6f).compositeOver(Color.Black)),
                ),
            ).clickable(interactionSource = source, indication = androidx.compose.foundation.LocalIndication.current, onClick = onClick),
    ) {
        // La forma decorativa che esce dall'angolo in alto a destra (non nel tema iOS)
        if (!ios) androidx.compose.foundation.layout.Box(
            Modifier
                .align(Alignment.TopEnd)
                .offset(x = 26.dp, y = (-22).dp)
                .size(104.dp)
                .graphicsLayer { rotationZ = rotation }
                .clip(shape)
                .background(content.copy(alpha = 0.16f)),
        )
        if (!ios) androidx.compose.foundation.layout.Box(
            Modifier
                .align(Alignment.TopEnd)
                .offset(x = (-30).dp, y = 14.dp)
                .size(18.dp)
                .graphicsLayer { rotationZ = -rotation }
                .clip(shape)
                .background(content.copy(alpha = 0.22f)),
        )
        Text(
            title,
            style = if (ios) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge,
            fontWeight = if (ios) FontWeight.Bold else FontWeight.Black,
            color = content,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.align(Alignment.BottomStart).padding(16.dp),
        )
    }
}
