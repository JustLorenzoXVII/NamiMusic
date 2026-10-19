@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.justlorenzo.archivetunex.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import archivetunex.shared.generated.resources.Res
import archivetunex.shared.generated.resources.check
import archivetunex.shared.generated.resources.library_add
import archivetunex.shared.generated.resources.refresh
import com.justlorenzo.archivetunex.insights.Mix
import com.justlorenzo.archivetunex.insights.MixKind
import com.justlorenzo.archivetunex.insights.MixMaker
import com.justlorenzo.archivetunex.ui.LocalActions
import com.justlorenzo.archivetunex.ui.LocalNavigator
import com.justlorenzo.archivetunex.ui.Route
import com.justlorenzo.archivetunex.ui.components.Artwork
import com.justlorenzo.archivetunex.ui.components.AtxIcon
import com.justlorenzo.archivetunex.ui.components.DetailHeader
import com.justlorenzo.archivetunex.ui.components.DetailToggle
import com.justlorenzo.archivetunex.ui.components.EmptyState
import com.justlorenzo.archivetunex.ui.components.PlayShuffleButtons
import com.justlorenzo.archivetunex.ui.components.SongMenuSheet
import com.justlorenzo.archivetunex.ui.components.SongRow
import com.justlorenzo.archivetunex.ui.tr
import com.justlorenzo.archivetunex.ui.trf
import kotlinx.coroutines.launch
import moe.rukamori.archivetune.models.MediaMetadata

/** Colori di ogni tipo di mix (la copertina li usa per la sfumatura). */
private fun Mix.colors(): List<Color> =
    when (kind) {
        MixKind.ForYou -> listOf(Color(0xFFFF8FB8), Color(0xFF7A3B6E))
        MixKind.Discover -> listOf(Color(0xFF5CC8FF), Color(0xFF2B3FA0))
        MixKind.Rediscover -> listOf(Color(0xFFFFC56B), Color(0xFFB34E2E))
        MixKind.Artist -> listOf(Color(0xFF9B7BFF), Color(0xFF3A1F7E))
    }

/** Sezione "Fatte per te" della Home: i mix si preparano da soli e si rinnovano ogni settimana. */
@Composable
fun MadeForYouSection() {
    val actions = LocalActions.current
    val navigator = LocalNavigator.current
    val scope = rememberCoroutineScope()
    val mixes by MixMaker.mixes.collectAsState()
    val working by MixMaker.working.collectAsState()
    LaunchedEffect(Unit) { MixMaker.refreshIfNeeded(actions.database) }
    if (mixes.isEmpty() && !working) return

    Column {
        Row(
            Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 20.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(tr("Fatte per te"), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.5).sp)
                Text(tr("Dai tuoi ascolti, ogni settimana"), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (working) {
                LoadingIndicator(Modifier.size(36.dp))
            } else {
                IconButton(onClick = { scope.launch { MixMaker.refreshIfNeeded(actions.database, force = true) } }) {
                    AtxIcon(Res.drawable.refresh, tr("Rigenera"), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        if (mixes.isEmpty()) {
            Text(
                tr("Sto preparando i tuoi mix…"),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
            return@Column
        }
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(mixes, key = { it.id }) { mix ->
                Column(
                    Modifier
                        .width(160.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .clickable { navigator.open(Route.Mix(mix.id, mix.title)) }
                        .padding(4.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    MixCover(mix, 152.dp)
                    Text(tr(mix.subtitle), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(horizontal = 4.dp))
                }
            }
        }
    }
}

/** Copertina del mix: 4 copertine dei brani, una sfumatura colorata e il titolo sopra. */
@Composable
private fun MixCover(
    mix: Mix,
    size: Dp,
) {
    val covers = mix.songs.mapNotNull { it.thumbnailUrl }.distinct().take(4)
    Box(Modifier.size(size).clip(RoundedCornerShape(22.dp))) {
        Column {
            for (row in 0 until 2) {
                Row {
                    for (col in 0 until 2) {
                        Artwork(covers.getOrNull(row * 2 + col) ?: covers.firstOrNull(), Modifier.size(size / 2), RoundedCornerShape(0.dp), sizePx = 240)
                    }
                }
            }
        }
        val (top, bottom) = mix.colors()
        Box(
            Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(top.copy(alpha = 0.35f), bottom.copy(alpha = 0.92f)))),
        )
        Text(
            tr(mix.title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black,
            color = Color.White,
            letterSpacing = (-0.5).sp,
            lineHeight = 26.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.align(Alignment.BottomStart).padding(14.dp),
        )
    }
}

/** Pagina di un mix: ascoltalo, mescolalo o salvalo come playlist. */
@Composable
fun MixScreen(
    mixId: String,
    title: String,
) {
    val actions = LocalActions.current
    val mixes by MixMaker.mixes.collectAsState()
    val mix = mixes.firstOrNull { it.id == mixId }
    var menuSong by remember { mutableStateOf<MediaMetadata?>(null) }
    var saved by remember(mixId) { mutableStateOf(false) }

    if (mix == null) {
        DetailScaffold(null) {
            EmptyState(tr("Questo mix è stato rinnovato. Torna alla Home per vedere quelli nuovi."), Modifier.padding(top = 140.dp))
        }
        return
    }
    val songs = remember(mix) { mix.songs.map { it.toMediaMetadata() } }
    val cover = songs.firstOrNull()?.thumbnailUrl

    DetailScaffold(cover) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = detailPadding()) {
            item(key = "header") {
                DetailHeader(
                        artworkUrl = null,
                        title = tr(mix.title.ifBlank { title }),
                        subtitle = tr(mix.subtitle),
                        detail = trf("{0} brani", songs.size),
                        artwork = { MixCover(mix, 250.dp) },
                    ) {
                        PlayShuffleButtons(
                            onPlay = { actions.playList(songs, title = mix.title) },
                            onShuffle = { actions.playList(songs, title = mix.title, shuffle = true) },
                            onDownload = { actions.downloadAll(songs) },
                            toggle =
                                DetailToggle(
                                    active = saved,
                                    labelOn = tr("Salvata"),
                                    labelOff = tr("Salva come playlist"),
                                    iconOn = Res.drawable.check,
                                    iconOff = Res.drawable.library_add,
                                ) {
                                    if (!saved) {
                                        saved = true
                                        actions.saveAsPlaylist(mix.title, songs)
                                    }
                                },
                        )
                    }
            }
            itemsIndexed(songs, key = { index, song -> "${song.id}-$index" }) { index, song ->
                SongRow(
                    songId = song.id,
                    title = song.title,
                    subtitle = song.artists.joinToString { it.name },
                    artworkUrl = song.thumbnailUrl,
                    explicit = song.explicit,
                    onClick = { actions.playList(songs, startIndex = index, title = mix.title) },
                    onMore = { menuSong = song },
                )
            }
        }
    }
    menuSong?.let { SongMenuSheet(it, onDismiss = { menuSong = null }) }
}
