@file:OptIn(ExperimentalMaterial3Api::class)

package com.justlorenzo.archivetunex.ui.screens

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import archivetunex.shared.generated.resources.Res
import archivetunex.shared.generated.resources.add
import archivetunex.shared.generated.resources.check
import archivetunex.shared.generated.resources.close
import archivetunex.shared.generated.resources.delete
import archivetunex.shared.generated.resources.drag_handle
import archivetunex.shared.generated.resources.edit
import archivetunex.shared.generated.resources.playlist_add
import archivetunex.shared.generated.resources.remove
import archivetunex.shared.generated.resources.search
import archivetunex.shared.generated.resources.sort
import archivetunex.shared.generated.resources.arrow_downward
import archivetunex.shared.generated.resources.arrow_upward
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import com.justlorenzo.archivetunex.ui.PlaylistSortKey
import com.justlorenzo.archivetunex.ui.PlaylistSortDescendingKey
import moe.rukamori.archivetune.utils.rememberPreference
import com.justlorenzo.archivetunex.ui.LocalActions
import com.justlorenzo.archivetunex.ui.LocalNavigator
import com.justlorenzo.archivetunex.ui.components.Artwork
import com.justlorenzo.archivetunex.ui.components.AtxIcon
import com.justlorenzo.archivetunex.ui.components.ConfirmDialog
import com.justlorenzo.archivetunex.ui.components.DetailHeader
import com.justlorenzo.archivetunex.ui.components.DetailMenuItem
import com.justlorenzo.archivetunex.ui.components.EmptyState
import com.justlorenzo.archivetunex.ui.components.LoadingBox
import com.justlorenzo.archivetunex.ui.components.PlayShuffleButtons
import com.justlorenzo.archivetunex.ui.components.RenamePlaylistDialog
import com.justlorenzo.archivetunex.ui.components.SongMenuSheet
import com.justlorenzo.archivetunex.ui.components.SongRow
import com.justlorenzo.archivetunex.ui.components.subtitle
import com.justlorenzo.archivetunex.ui.tr
import com.justlorenzo.archivetunex.ui.trf
import kotlinx.coroutines.delay
import moe.rukamori.archivetune.db.entities.PlaylistSong
import moe.rukamori.archivetune.innertube.YouTube
import moe.rukamori.archivetune.innertube.YouTube.SearchFilter
import moe.rukamori.archivetune.innertube.models.SongItem
import moe.rukamori.archivetune.models.toMediaMetadata
import kotlin.math.roundToInt

/**
 * Una tua playlist: si ascolta, si cerca dentro, e con "Modifica" si rinomina,
 * si tolgono brani e si riordinano trascinandoli. "Aggiungi brani" cerca su YouTube Music.
 * Se la playlist è collegata a YouTube Music, ogni modifica viene fatta anche lì.
 */
@Composable
fun LocalPlaylistScreen(
    playlistId: String,
    title: String,
) {
    val actions = LocalActions.current
    val navigator = LocalNavigator.current
    val density = LocalDensity.current
    val haptics = LocalHapticFeedback.current

    var menuEntryKey by remember { mutableStateOf<Int?>(null) }
    var renaming by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }
    var adding by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }

    val playlist by remember(playlistId) { actions.database.playlist(playlistId) }.collectAsState(null)
    val dbEntries by remember(playlistId) { actions.database.playlistSongs(playlistId) }.collectAsState(emptyList())
    // Copia locale: gli spostamenti e le rimozioni si vedono subito, poi arriva il database
    var entries by remember { mutableStateOf<List<PlaylistSong>>(emptyList()) }
    LaunchedEffect(dbEntries) { entries = dbEntries }
    // Se è sincronizzata con YouTube Music, all'apertura si riallinea (ordine e voci)
    var refreshed by remember { mutableStateOf(false) }
    LaunchedEffect(playlist?.playlist?.id) {
        val entity = playlist?.playlist ?: return@LaunchedEffect
        if (refreshed) return@LaunchedEffect
        refreshed = true
        runCatching { actions.refreshOwnPlaylist(entity) }
    }

    val songs = remember(entries) { entries.map { it.song.toMediaMetadata() } }
    val name = playlist?.playlist?.name ?: title
    val cover = playlist?.playlist?.thumbnailUrl ?: playlist?.thumbnails?.firstOrNull() ?: songs.firstOrNull()?.thumbnailUrl
    val synced = playlist?.playlist?.browseId != null

    // Ordine di visualizzazione: personalizzato (quello della playlist) o per titolo, artista, ecc.
    var sortName by rememberPreference(PlaylistSortKey, PlaylistSort.Custom.name)
    var descending by rememberPreference(PlaylistSortDescendingKey, false)
    val sort = PlaylistSort.entries.firstOrNull { it.name == sortName } ?: PlaylistSort.Custom
    val ordered =
        remember(entries, songs, sort, descending) {
            val base =
                when (sort) {
                    PlaylistSort.Custom -> entries.indices.toList()
                    PlaylistSort.Title -> entries.indices.sortedBy { songs[it].title.lowercase() }
                    PlaylistSort.Artist -> entries.indices.sortedBy { songs[it].artists.firstOrNull()?.name.orEmpty().lowercase() }
                    PlaylistSort.Album -> entries.indices.sortedBy { songs[it].album?.title.orEmpty().lowercase() }
                    PlaylistSort.Added -> entries.indices.sortedBy { entries[it].map.id }
                    PlaylistSort.Duration -> entries.indices.sortedBy { songs[it].duration }
                }
            if (descending) base.reversed() else base
        }
    // Ricerca dentro la playlist (titolo o artista)
    val visible =
        remember(ordered, songs, query) {
            val q = query.trim().lowercase()
            if (q.isEmpty()) {
                ordered
            } else {
                ordered.filter { i ->
                    songs[i].title.lowercase().contains(q) || songs[i].artists.any { it.name.lowercase().contains(q) }
                }
            }
        }

    // Trascinamento in modalità Modifica
    val rowHeight = 68.dp
    val rowPx = with(density) { rowHeight.toPx() }
    var dragFrom by remember { mutableStateOf<Int?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    val dragTarget =
        dragFrom?.let { from -> (from + (dragOffset / rowPx).roundToInt()).coerceIn(0, (entries.size - 1).coerceAtLeast(0)) }

    DetailScaffold(cover) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = detailPadding()) {
            item(key = "header") {
                DetailHeader(
                    artworkUrl = cover,
                    title = name,
                    subtitle = if (synced) tr("Sincronizzata con YouTube Music") else null,
                    detail = trf("{0} brani", songs.size),
                ) {
                    PlayShuffleButtons(
                        onPlay = { actions.playList(ordered.map { songs[it] }, title = name) },
                        onShuffle = { actions.playList(songs, title = name, shuffle = true) },
                        onDownload = { actions.downloadAll(songs) },
                        onDelete = { deleting = true },
                    )
                }
            }
            // Pulsanti ben visibili: aggiungi e modifica
            item(key = "tools") {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    com.justlorenzo.archivetunex.ui.ios.AtxButton(tonal = true, onClick = { adding = true }, modifier = Modifier.weight(1f).height(48.dp)) {
                        AtxIcon(Res.drawable.add, null, size = 20.dp)
                        Spacer(Modifier.width(8.dp))
                        Text(tr("Aggiungi brani"), fontWeight = FontWeight.Bold, maxLines = 1)
                    }
                    com.justlorenzo.archivetunex.ui.ios.AtxButton(
                        tonal = true,
                        highlighted = editing,
                        onClick = {
                            editing = !editing
                            query = ""
                        },
                        modifier = Modifier.weight(1f).height(48.dp),
                    ) {
                        AtxIcon(if (editing) Res.drawable.check else Res.drawable.edit, null, size = 20.dp)
                        Spacer(Modifier.width(8.dp))
                        Text(if (editing) tr("Fine") else tr("Modifica"), fontWeight = FontWeight.Bold, maxLines = 1)
                    }
                }
            }
            if (editing) {
                // Dettagli della playlist: tocca per cambiare il nome
                item(key = "details") {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .clickable { renaming = true }
                            .padding(horizontal = 18.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(tr("Nome della playlist"), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        AtxIcon(Res.drawable.edit, tr("Rinomina"), size = 20.dp, tint = MaterialTheme.colorScheme.primary)
                    }
                }
                if (entries.isNotEmpty()) {
                    item(key = "edit-hint") {
                        Text(
                            tr("Trascina ≡ per cambiare l'ordine, tocca − per togliere un brano."),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                        )
                    }
                }
            } else if (entries.isNotEmpty()) {
                item(key = "search") {
                    Row(
                        Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        PlaylistSearchField(
                            query = query,
                            onQueryChange = { query = it },
                            placeholder = tr("Cerca nella playlist"),
                            modifier = Modifier.weight(1f),
                        )
                        // Ordina per…
                        Box {
                            var sortOpen by remember { mutableStateOf(false) }
                            IconButton(onClick = { sortOpen = true }, modifier = Modifier.padding(start = 4.dp)) {
                                AtxIcon(
                                    Res.drawable.sort,
                                    tr("Ordina"),
                                    tint = if (sort != PlaylistSort.Custom) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            com.justlorenzo.archivetunex.ui.ios.AtxDropdownMenu(expanded = sortOpen, onDismissRequest = { sortOpen = false }) {
                                Text(
                                    tr("Ordina per"),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                )
                                PlaylistSort.entries.forEach { option ->
                                    val selected = option == sort
                                    com.justlorenzo.archivetunex.ui.ios.AtxMenuItem(
                                        text = option.label,
                                        icon = if (selected) (if (descending) Res.drawable.arrow_downward else Res.drawable.arrow_upward) else null,
                                        selected = selected,
                                        // Toccando di nuovo l'ordine scelto si inverte
                                        onClick = {
                                            if (selected) {
                                                descending = !descending
                                            } else {
                                                sortName = option.name
                                                descending = option == PlaylistSort.Added
                                            }
                                            sortOpen = false
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (entries.isEmpty()) {
                item(key = "empty") { EmptyState(tr("Questa playlist è vuota. Tocca “Aggiungi brani” per riempirla.")) }
            } else if (visible.isEmpty()) {
                item(key = "no-results") { EmptyState(trf("Nessun brano trovato per “{0}”.", query.trim()), icon = Res.drawable.search) }
            }

            if (editing) {
                itemsIndexed(entries, key = { _, entry -> "edit-${entry.map.id}" }) { index, entry ->
                    val song = songs.getOrNull(index) ?: return@itemsIndexed
                    val isDragged = dragFrom == index
                    val from = dragFrom
                    val shift =
                        when {
                            isDragged || from == null || dragTarget == null -> 0f
                            from < dragTarget && index in (from + 1)..dragTarget -> -rowPx
                            from > dragTarget && index in dragTarget until from -> rowPx
                            else -> 0f
                        }
                    val animatedShift by animateFloatAsState(shift, spring(stiffness = Spring.StiffnessMediumLow), label = "playlistShift")
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .height(rowHeight)
                            .graphicsLayer {
                                translationY = if (isDragged) dragOffset else if (from == null) 0f else animatedShift
                                shadowElevation = if (isDragged) 12.dp.toPx() else 0f
                            }.background(if (isDragged) MaterialTheme.colorScheme.surfaceContainerHighest else Color.Transparent)
                            .padding(start = 8.dp, end = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // Togli dalla playlist
                        IconButton(onClick = {
                            val current = playlist?.playlist ?: return@IconButton
                            entries = entries.filterNot { it.map.id == entry.map.id }
                            actions.removeFromPlaylist(current, entry.map)
                        }) {
                            Box(
                                Modifier.size(26.dp).clip(CircleShape).background(MaterialTheme.colorScheme.error),
                                contentAlignment = Alignment.Center,
                            ) { AtxIcon(Res.drawable.remove, tr("Togli dalla playlist"), size = 18.dp, tint = MaterialTheme.colorScheme.onError) }
                        }
                        Artwork(song.thumbnailUrl, Modifier.size(48.dp), RoundedCornerShape(12.dp), sizePx = 160)
                        Spacer(Modifier.width(14.dp))
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
                        // Maniglia: trascinala per spostare il brano
                        Box(
                            Modifier
                                .size(48.dp)
                                .fillMaxHeight()
                                .pointerInput(index, entries.size) {
                                    detectDragGestures(
                                        onDragStart = {
                                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                            dragFrom = index
                                            dragOffset = 0f
                                        },
                                        onDrag = { change, amount ->
                                            change.consume()
                                            dragOffset += amount.y
                                        },
                                        onDragEnd = {
                                            val start = dragFrom
                                            val end = dragTarget
                                            val current = playlist?.playlist
                                            if (start != null && end != null && start != end && current != null) {
                                                val before = entries
                                                val moved = before[start]
                                                // Il brano che dopo lo spostamento gli starà subito dopo (per YouTube Music)
                                                val successor = before.getOrNull(if (start > end) end else end + 1)
                                                entries = before.toMutableList().apply { add(end, removeAt(start)) }
                                                actions.moveInPlaylist(current, start, end, moved.map.setVideoId, successor?.map?.setVideoId)
                                            }
                                            dragFrom = null
                                            dragOffset = 0f
                                        },
                                        onDragCancel = {
                                            dragFrom = null
                                            dragOffset = 0f
                                        },
                                    )
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            AtxIcon(Res.drawable.drag_handle, tr("Sposta"), size = 24.dp, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            } else {
                items(visible, key = { index -> "song-${entries[index].map.id}" }) { index ->
                    val song = songs[index]
                    SongRow(
                        songId = song.id,
                        title = song.title,
                        subtitle = song.artists.joinToString { it.name },
                        artworkUrl = song.thumbnailUrl,
                        explicit = song.explicit,
                        onClick = {
                            val queue = visible.takeIf { query.isNotBlank() } ?: ordered
                            actions.playList(queue.map { songs[it] }, startIndex = queue.indexOf(index).coerceAtLeast(0), title = name)
                        },
                        onMore = { menuEntryKey = entries[index].map.id },
                    )
                }
            }
        }
    }

    menuEntryKey?.let { key ->
        val index = entries.indexOfFirst { it.map.id == key }
        val entry = entries.getOrNull(index)
        val song = songs.getOrNull(index)
        if (entry != null && song != null) {
            SongMenuSheet(
                song,
                onDismiss = { menuEntryKey = null },
                onRemoveFromPlaylist = { playlist?.playlist?.let { actions.removeFromPlaylist(it, entry.map) } },
            )
        }
    }
    if (adding) {
        AddSongsSheet(
            playlistId = playlistId,
            alreadyIn = remember(entries) { entries.map { it.song.id }.toSet() },
            onDismiss = { adding = false },
        )
    }
    if (renaming) {
        RenamePlaylistDialog(
            currentName = name,
            onDismiss = { renaming = false },
            onRename = { newName -> playlist?.playlist?.let { actions.renamePlaylist(it, newName) } },
        )
    }
    if (deleting) {
        val ownedOnYouTube = playlist?.playlist?.let { it.browseId != null && it.isEditable } == true
        ConfirmDialog(
            title = trf("Eliminare “{0}”?", name),
            text =
                if (ownedOnYouTube) {
                    tr("La playlist viene eliminata dal telefono e anche dal tuo account YouTube Music. I brani scaricati restano.")
                } else {
                    tr("La playlist viene tolta dalla libreria del telefono. I brani scaricati restano.")
                },
            confirmLabel = tr("Elimina"),
            onDismiss = { deleting = false },
            onConfirm = {
                playlist?.playlist?.let(actions::deletePlaylist)
                navigator.back()
            },
        )
    }
}

/** Come mostrare i brani di una playlist. */
private enum class PlaylistSort(
    private val rawLabel: String,
) {
    Custom("Personalizzato"),
    Title("Titolo"),
    Artist("Artista"),
    Album("Album"),
    Added("Data di aggiunta"),
    Duration("Durata");

    val label: String get() = tr(rawLabel)
}

/** Campo di ricerca arrotondato. */
@Composable
private fun PlaylistSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
) {
    com.justlorenzo.archivetunex.ui.ios.AtxSearchField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = placeholder,
        modifier = modifier.fillMaxWidth().let { if (focusRequester != null) it.focusRequester(focusRequester) else it },
    )
}

/** Foglio per aggiungere brani: cerca su YouTube Music e tocca + per aggiungerli. */
@Composable
private fun AddSongsSheet(
    playlistId: String,
    alreadyIn: Set<String>,
    onDismiss: () -> Unit,
) {
    val actions = LocalActions.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val focus = remember { FocusRequester() }
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<SongItem>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    // Aggiunti in questa sessione: il + diventa ✓ subito
    var added by remember { mutableStateOf(emptySet<String>()) }

    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    LaunchedEffect(query) {
        val text = query.trim()
        if (text.isEmpty()) {
            results = emptyList()
            loading = false
            return@LaunchedEffect
        }
        delay(350)
        loading = true
        results = YouTube.search(text, SearchFilter.FILTER_SONG).getOrNull()?.items?.filterIsInstance<SongItem>().orEmpty()
        loading = false
    }

    com.justlorenzo.archivetunex.ui.components.AtxModalSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Text(
            tr("Aggiungi brani"),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = com.justlorenzo.archivetunex.ui.ios.heavyWeight,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 12.dp),
        )
        PlaylistSearchField(
            query = query,
            onQueryChange = { query = it },
            placeholder = tr("Cerca un brano da aggiungere"),
            modifier = Modifier.padding(horizontal = 16.dp),
            focusRequester = focus,
        )
        Spacer(Modifier.height(8.dp))
        Box(Modifier.fillMaxWidth().fillMaxHeight(0.8f)) {
            when {
                loading && results.isEmpty() -> LoadingBox(Modifier.padding(top = 40.dp))
                query.isBlank() ->
                    Text(
                        tr("Scrivi il titolo o l'artista."),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(20.dp),
                    )
                else ->
                    LazyColumn(Modifier.fillMaxSize()) {
                        items(results, key = { it.id }) { item ->
                            val isIn = item.id in alreadyIn || item.id in added
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = !isIn) {
                                        added = added + item.id
                                        actions.addToPlaylist(playlistId, item.toMediaMetadata())
                                    }.padding(start = 20.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                            ) {
                                Artwork(item.thumbnail, Modifier.size(52.dp), RoundedCornerShape(12.dp), sizePx = 200)
                                Column(Modifier.weight(1f)) {
                                    Text(item.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(
                                        item.subtitle(),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                Box(
                                    Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(if (isIn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    AtxIcon(
                                        if (isIn) Res.drawable.check else Res.drawable.add,
                                        if (isIn) tr("Già nella playlist") else tr("Aggiungi"),
                                        size = 22.dp,
                                        tint = if (isIn) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                    )
                                }
                            }
                        }
                        item { Spacer(Modifier.height(24.dp)) }
                    }
            }
        }
    }
}
