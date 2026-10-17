package com.justlorenzo.archivetunex.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import archivetunex.shared.generated.resources.Res
import archivetunex.shared.generated.resources.album
import archivetunex.shared.generated.resources.artist
import archivetunex.shared.generated.resources.delete
import archivetunex.shared.generated.resources.download
import archivetunex.shared.generated.resources.playlist_add
import archivetunex.shared.generated.resources.playlist_play
import archivetunex.shared.generated.resources.remove
import archivetunex.shared.generated.resources.queue_music
import archivetunex.shared.generated.resources.radio
import com.justlorenzo.archivetunex.ui.LocalActions
import com.justlorenzo.archivetunex.ui.LocalNavigator
import com.justlorenzo.archivetunex.ui.Route
import moe.rukamori.archivetune.compat.media3.Download
import moe.rukamori.archivetune.models.MediaMetadata
import org.jetbrains.compose.resources.DrawableResource

/** Menu di un brano (tenendo premuto o con i tre puntini). */
@Composable
fun SongMenuSheet(
    song: MediaMetadata,
    onDismiss: () -> Unit,
    onRemoveFromPlaylist: (() -> Unit)? = null,
) {
    val actions = LocalActions.current
    val navigator = LocalNavigator.current
    var addingToPlaylist by remember { mutableStateOf(false) }
    val download by remember(song.id) { actions.downloads.getDownload(song.id) }.collectAsState(null)
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    if (!addingToPlaylist) ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Artwork(song.thumbnailUrl, Modifier.size(56.dp), MaterialTheme.shapes.small, sizePx = 226)
            Column(Modifier.weight(1f)) {
                Text(
                    song.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    song.artists.joinToString { it.name },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        HorizontalDivider(Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)

        MenuEntry(Res.drawable.playlist_play, "Riproduci dopo") {
            actions.playNext(song)
            onDismiss()
        }
        MenuEntry(Res.drawable.queue_music, "Aggiungi in coda") {
            actions.addToQueue(song)
            onDismiss()
        }
        MenuEntry(Res.drawable.playlist_add, "Aggiungi a playlist") { addingToPlaylist = true }
        if (onRemoveFromPlaylist != null) {
            MenuEntry(Res.drawable.remove, "Rimuovi dalla playlist") {
                onRemoveFromPlaylist()
                onDismiss()
            }
        }
        MenuEntry(Res.drawable.radio, "Avvia radio") {
            actions.playSong(song)
            onDismiss()
        }
        when (download?.state) {
            Download.STATE_COMPLETED ->
                MenuEntry(Res.drawable.delete, "Rimuovi dal dispositivo") {
                    actions.removeDownload(song.id)
                    onDismiss()
                }
            Download.STATE_DOWNLOADING, Download.STATE_QUEUED, Download.STATE_RESTARTING ->
                MenuEntry(Res.drawable.delete, "Annulla download") {
                    actions.removeDownload(song.id)
                    onDismiss()
                }
            else ->
                MenuEntry(Res.drawable.download, "Scarica") {
                    actions.download(song)
                    onDismiss()
                }
        }
        song.album?.let { album ->
            MenuEntry(Res.drawable.album, "Vai all'album") {
                navigator.open(Route.Album(album.id))
                onDismiss()
            }
        }
        song.artists.firstOrNull { it.id != null }?.let { artist ->
            MenuEntry(Res.drawable.artist, "Vai all'artista") {
                navigator.open(Route.Artist(artist.id!!))
                onDismiss()
            }
        }
        Spacer(Modifier.height(24.dp))
    }

    if (addingToPlaylist) AddToPlaylistSheet(song, onDismiss = onDismiss)
}

@Composable
private fun MenuEntry(
    icon: DrawableResource,
    label: String,
    onClick: () -> Unit,
) {
    ListItem(
        headlineContent = { Text(label) },
        leadingContent = { AtxIcon(icon, null) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.clickable(onClick = onClick),
    )
}
