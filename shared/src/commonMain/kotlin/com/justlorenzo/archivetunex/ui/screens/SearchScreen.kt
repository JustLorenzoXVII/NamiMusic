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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FilterChip
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
import moe.rukamori.archivetune.innertube.SearchFilter
import moe.rukamori.archivetune.innertube.YouTube
import moe.rukamori.archivetune.innertube.models.ArtistItem
import moe.rukamori.archivetune.innertube.models.SongItem
import moe.rukamori.archivetune.innertube.models.YTItem
import moe.rukamori.archivetune.models.MediaMetadata
import moe.rukamori.archivetune.models.toMediaMetadata

private enum class SearchKind(
    val label: String,
    val filter: SearchFilter,
) {
    Songs("Brani", SearchFilter.FILTER_SONG),
    Albums("Album", SearchFilter.FILTER_ALBUM),
    Artists("Artisti", SearchFilter.FILTER_ARTIST),
    Playlists("Playlist", SearchFilter.FILTER_FEATURED_PLAYLIST),
    Videos("Video", SearchFilter.FILTER_VIDEO),
}

@Composable
fun SearchScreen() {
    val actions = LocalActions.current
    val navigator = LocalNavigator.current
    val focusManager = LocalFocusManager.current

    var text by rememberSaveable { mutableStateOf("") }
    var submitted by rememberSaveable { mutableStateOf("") }
    var kind by rememberSaveable { mutableStateOf(SearchKind.Songs) }
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
        Column(
            Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp).padding(top = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "Cerca",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 4.dp),
            )
            TextField(
                value = text,
                onValueChange = {
                    text = it
                    if (it.isBlank()) submitted = ""
                },
                placeholder = { Text("Brani, artisti, album, playlist") },
                leadingIcon = { AtxIcon(Res.drawable.search, null) },
                trailingIcon = {
                    if (text.isNotEmpty()) {
                        IconButton(onClick = {
                            text = ""
                            submitted = ""
                        }) { AtxIcon(Res.drawable.close, "Cancella") }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(28.dp),
                colors =
                    TextFieldDefaults.colors(
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { submit(text) }),
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

            history.isNotEmpty() ->
                LazyColumn(contentPadding = BottomContentPadding, modifier = Modifier.fillMaxSize()) {
                    item {
                        Row(
                            Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                "Ricerche recenti",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f),
                            )
                            TextButton(onClick = { actions.database.query { clearSearchHistory() } }) { Text("Cancella") }
                        }
                    }
                    items(history, key = { it.id }) { entry ->
                        QueryRow(entry.query, Res.drawable.history, onClick = { submit(entry.query) }, onFill = { text = entry.query })
                    }
                }

            else -> EmptyState("Cerca brani, album, artisti e playlist su YouTube Music.", icon = Res.drawable.search)
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
            AtxIcon(Res.drawable.arrow_top_left, "Modifica la ricerca", tint = MaterialTheme.colorScheme.onSurfaceVariant)
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
    val results = rememberLoadable(query to kind) { YouTube.search(query, kind.filter) }

    LazyColumn(contentPadding = BottomContentPadding, modifier = Modifier.fillMaxSize()) {
        item(key = "filters") {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(SearchKind.entries) { entry ->
                    FilterChip(
                        selected = entry == kind,
                        onClick = { onKindChange(entry) },
                        label = { Text(entry.label) },
                        shape = RoundedCornerShape(18.dp),
                    )
                }
            }
        }
        when (val state = results.value) {
            LoadState.Loading -> item(key = "loading") { LoadingBox() }
            is LoadState.Failed -> item(key = "error") { ErrorBox(state.message, results::reload) }
            is LoadState.Ready -> {
                if (state.value.items.isEmpty()) {
                    item(key = "empty") { EmptyState("Nessun risultato per “$query”.", icon = Res.drawable.search) }
                }
                items(state.value.items) { item ->
                    if (item is SongItem) {
                        val metadata = remember(item) { item.toMediaMetadata() }
                        SongRow(
                            songId = item.id,
                            title = item.title,
                            subtitle = item.subtitle(),
                            artworkUrl = item.thumbnail,
                            explicit = item.explicit,
                            onClick = { onOpen(item) },
                            onMore = { onSongMenu(metadata) },
                        )
                    } else {
                        ItemRow(item, onClick = { onOpen(item) })
                    }
                }
            }
        }
    }
}

/** Riga per album, artisti e playlist nei risultati. */
@Composable
private fun ItemRow(
    item: YTItem,
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
                item.subtitle(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
