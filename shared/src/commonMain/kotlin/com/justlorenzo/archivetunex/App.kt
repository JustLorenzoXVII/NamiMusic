package com.justlorenzo.archivetunex

import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.justlorenzo.archivetunex.player.Player
import com.justlorenzo.archivetunex.player.PlayerState
import kotlinx.coroutines.launch
import moe.rukamori.archivetune.innertube.YouTube
import moe.rukamori.archivetune.innertube.YouTube.SearchFilter
import moe.rukamori.archivetune.innertube.models.SongItem

/**
 * Schermata di prova: ricerca e riproduzione.
 * Serve a verificare client YouTube e lettore su Android e iPhone;
 * verrà sostituita dall'interfaccia vera.
 */
@Composable
fun App() {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()) {
        Surface(modifier = Modifier.fillMaxSize()) {
            val playerState by Player.controller.state.collectAsState()
            Column(Modifier.fillMaxSize()) {
                Box(Modifier.weight(1f)) {
                    SearchTestScreen(currentId = playerState.current?.id)
                }
                if (playerState.current != null || playerState.error != null) {
                    MiniPlayer(playerState)
                }
            }
        }
    }
}

@Composable
private fun SearchTestScreen(currentId: String?) {
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var songs by remember { mutableStateOf<List<SongItem>>(emptyList()) }

    fun search() {
        val text = query.trim()
        if (text.isEmpty() || loading) return
        loading = true
        error = null
        scope.launch {
            YouTube
                .search(text, SearchFilter.FILTER_SONG, useAccountContext = false)
                .onSuccess { result -> songs = result.items.filterIsInstance<SongItem>() }
                .onFailure { e -> error = e.message ?: e.toString() }
            loading = false
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("ArchiveTuneX", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(top = 8.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.weight(1f),
                singleLine = true,
                placeholder = { Text("Cerca un brano") },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { search() }),
            )
            Button(onClick = { search() }, enabled = !loading) { Text("Cerca") }
        }

        when {
            loading -> Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            error != null -> Text("Errore: $error", color = MaterialTheme.colorScheme.error)
        }

        LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
            itemsIndexed(songs, key = { _, song -> song.id }) { index, song ->
                val isCurrent = song.id == currentId
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clickable { Player.controller.playQueue(songs, index) }
                        .padding(vertical = 10.dp),
                ) {
                    Text(
                        song.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        fontWeight = if (isCurrent) FontWeight.Bold else null,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        song.artists.joinToString(", ") { it.name },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun MiniPlayer(state: PlayerState) {
    val controller = Player.controller
    Surface(tonalElevation = 3.dp, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.navigationBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp)) {
            state.current?.let { song ->
                Text(song.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    song.artists.joinToString(", ") { it.name },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            state.error?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error, maxLines = 4)
            }

            var dragging by remember { mutableStateOf<Float?>(null) }
            val duration = state.durationMs.coerceAtLeast(1)
            Slider(
                value = dragging ?: (state.positionMs.toFloat() / duration).coerceIn(0f, 1f),
                onValueChange = { dragging = it },
                onValueChangeFinished = {
                    dragging?.let { controller.seekTo((it * duration).toLong()) }
                    dragging = null
                },
                enabled = state.durationMs > 0,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(formatTime(state.positionMs), style = MaterialTheme.typography.labelSmall)
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = { controller.previous() }) { Text("⏮", fontSize = 22.sp) }
                        if (state.isLoading) {
                            CircularProgressIndicator(Modifier.size(28.dp).padding(2.dp), strokeWidth = 3.dp)
                        } else {
                            TextButton(onClick = { controller.togglePlayPause() }) {
                                Text(if (state.isPlaying) "⏸" else "▶", fontSize = 26.sp)
                            }
                        }
                        TextButton(onClick = { controller.next() }, enabled = state.hasNext) { Text("⏭", fontSize = 22.sp) }
                    }
                }
                Text(formatTime(state.durationMs), style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}
