package com.justlorenzo.archivetunex.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import archivetunex.shared.generated.resources.Res
import archivetunex.shared.generated.resources.add
import com.justlorenzo.archivetunex.ui.LocalActions
import kotlinx.coroutines.flow.map
import moe.rukamori.archivetune.models.MediaMetadata

/** Nome della nuova playlist e, con l'account collegato, se crearla anche su YouTube Music. */
@Composable
fun CreatePlaylistDialog(
    onDismiss: () -> Unit,
    firstSong: MediaMetadata? = null,
) {
    val actions = LocalActions.current
    val account = rememberAccount()
    var name by remember { mutableStateOf("") }
    var onYouTube by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nuova playlist") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (account != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Crea anche su YouTube Music", modifier = Modifier.weight(1f))
                        Switch(checked = onYouTube, onCheckedChange = { onYouTube = it })
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank(),
                onClick = {
                    actions.createPlaylist(name.trim(), onYouTube = account != null && onYouTube, firstSong = firstSong)
                    onDismiss()
                },
            ) { Text("Crea") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annulla") } },
    )
}

/** Scelta della playlist a cui aggiungere un brano. */
@Composable
fun AddToPlaylistSheet(
    song: MediaMetadata,
    onDismiss: () -> Unit,
) {
    val actions = LocalActions.current
    val playlists by remember {
        actions.database.playlistsByCreateDateAsc().map { list -> list.filter { it.playlist.isEditable }.reversed() }
    }.collectAsState(emptyList())
    var creating by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Text(
            "Aggiungi a playlist",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 20.dp, bottom = 8.dp),
        )
        LazyColumn(Modifier.fillMaxWidth().heightIn(max = 480.dp)) {
            item {
                Row(
                    Modifier.fillMaxWidth().clickable { creating = true }.padding(horizontal = 20.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    androidx.compose.foundation.layout.Box(
                        Modifier.size(52.dp),
                        contentAlignment = Alignment.Center,
                    ) { AtxIcon(Res.drawable.add, null, size = 28.dp, tint = MaterialTheme.colorScheme.primary) }
                    Text("Nuova playlist", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                }
            }
            items(playlists, key = { it.id }) { playlist ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable {
                            actions.addToPlaylist(playlist.id, song)
                            onDismiss()
                        }.padding(horizontal = 20.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Artwork(
                        playlist.thumbnails.firstOrNull() ?: playlist.playlist.thumbnailUrl,
                        Modifier.size(52.dp),
                        MaterialTheme.shapes.small,
                        sizePx = 226,
                    )
                    Column(Modifier.weight(1f)) {
                        Text(playlist.playlist.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            "${playlist.songCount} brani" + if (playlist.playlist.browseId != null) " · YouTube Music" else "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }

    if (creating) {
        CreatePlaylistDialog(
            onDismiss = {
                creating = false
                onDismiss()
            },
            firstSong = song,
        )
    }
}

/** Rinomina una playlist. */
@Composable
fun RenamePlaylistDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onRename: (String) -> Unit,
) {
    var name by remember { mutableStateOf(currentName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rinomina playlist") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(enabled = name.isNotBlank(), onClick = {
                onRename(name.trim())
                onDismiss()
            }) { Text("Salva") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annulla") } },
    )
}

/** Conferma per le azioni che non si possono annullare. */
@Composable
fun ConfirmDialog(
    title: String,
    text: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = {
            TextButton(onClick = {
                onConfirm()
                onDismiss()
            }) { Text(confirmLabel, color = MaterialTheme.colorScheme.error) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annulla") } },
    )
}
