package com.justlorenzo.archivetunex.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.justlorenzo.archivetunex.ui.LocalActions
import com.justlorenzo.archivetunex.ui.LocalNavigator
import com.justlorenzo.archivetunex.ui.components.AccountButton
import com.justlorenzo.archivetunex.ui.components.Artwork
import com.justlorenzo.archivetunex.ui.components.BottomContentPadding
import com.justlorenzo.archivetunex.ui.components.ErrorBox
import com.justlorenzo.archivetunex.ui.components.ItemCard
import com.justlorenzo.archivetunex.ui.components.LoadState
import com.justlorenzo.archivetunex.ui.components.LoadingBox
import com.justlorenzo.archivetunex.ui.components.SectionHeader
import com.justlorenzo.archivetunex.ui.components.SongRow
import com.justlorenzo.archivetunex.ui.components.rememberAccount
import com.justlorenzo.archivetunex.ui.components.rememberLoadable
import com.justlorenzo.archivetunex.ui.components.subtitle
import com.justlorenzo.archivetunex.ui.openItem
import moe.rukamori.archivetune.innertube.YouTube
import moe.rukamori.archivetune.innertube.models.SongItem
import moe.rukamori.archivetune.models.toMediaMetadata

@Composable
fun HomeScreen() {
    val actions = LocalActions.current
    val navigator = LocalNavigator.current
    val account = rememberAccount()
    // La Home cambia quando si fa login o logout
    val home = rememberLoadable(account?.name) { YouTube.home() }
    val recent by remember { actions.database.recentSongs(20) }.collectAsState(emptyList())

    PullToRefreshBox(
        isRefreshing = home.value is LoadState.Loading,
        onRefresh = home::reload,
        modifier = Modifier.fillMaxSize(),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = BottomContentPadding,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "header") {
                Row(
                    Modifier.fillMaxWidth().statusBarsPadding().padding(start = 20.dp, end = 8.dp, top = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "Home",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                    )
                    AccountButton(account) { navigator.accountVisible = true }
                }
            }

            if (recent.isNotEmpty()) {
                item(key = "recent") {
                    SectionHeader("Ascoltati di recente")
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(recent, key = { it.id }) { song ->
                            val metadata = remember(song) { song.toMediaMetadata() }
                            RecentSongCard(
                                title = metadata.title,
                                subtitle = metadata.artists.joinToString { it.name },
                                artworkUrl = metadata.thumbnailUrl,
                                onClick = { actions.playSong(metadata) },
                            )
                        }
                    }
                }
            }

            when (val state = home.value) {
                LoadState.Loading -> if (recent.isEmpty()) item(key = "loading") { LoadingBox() }
                is LoadState.Failed -> item(key = "error") { ErrorBox(state.message, home::reload) }
                is LoadState.Ready ->
                    state.value.sections.forEachIndexed { index, section ->
                        if (section.items.isEmpty()) return@forEachIndexed
                        item(key = "section-$index-${section.title}") {
                            SectionHeader(section.title)
                            if (section.items.all { it is SongItem }) {
                                // Brani: griglia di righe scorrevole (come "Scelte rapide" di YouTube Music)
                                LazyHorizontalGrid(
                                    rows = GridCells.Fixed(minOf(4, section.items.size)),
                                    modifier = Modifier.height((minOf(4, section.items.size) * 64).dp),
                                ) {
                                    items(section.items) { item ->
                                        val song = item as SongItem
                                        SongRow(
                                            songId = song.id,
                                            title = song.title,
                                            subtitle = song.subtitle(),
                                            artworkUrl = song.thumbnail,
                                            explicit = song.explicit,
                                            onClick = { actions.playSong(song) },
                                            modifier = Modifier.width(320.dp),
                                        )
                                    }
                                }
                            } else {
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    items(section.items) { item ->
                                        ItemCard(item, onClick = { openItem(item, actions, navigator) })
                                    }
                                }
                            }
                        }
                    }
            }
        }
    }
}

@Composable
private fun RecentSongCard(
    title: String,
    subtitle: String,
    artworkUrl: String?,
    onClick: () -> Unit,
) {
    Column(
        Modifier
            .width(128.dp)
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .padding(4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Artwork(
            artworkUrl,
            Modifier.width(120.dp).height(120.dp),
            MaterialTheme.shapes.large,
            sizePx = 300,
        )
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
