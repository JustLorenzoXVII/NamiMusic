package com.justlorenzo.archivetunex.ui.screens

import com.justlorenzo.archivetunex.ui.tr
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.toShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import archivetunex.shared.generated.resources.Res
import archivetunex.shared.generated.resources.play
import com.justlorenzo.archivetunex.ui.LocalActions
import com.justlorenzo.archivetunex.ui.LocalNavigator
import com.justlorenzo.archivetunex.ui.components.AccountButton
import com.justlorenzo.archivetunex.ui.components.Artwork
import com.justlorenzo.archivetunex.ui.components.AtxIcon
import com.justlorenzo.archivetunex.ui.components.BottomContentPadding
import com.justlorenzo.archivetunex.ui.components.ErrorBox
import com.justlorenzo.archivetunex.ui.components.ItemCard
import com.justlorenzo.archivetunex.ui.components.LoadState
import com.justlorenzo.archivetunex.ui.components.LoadingBox
import com.justlorenzo.archivetunex.ui.components.ScreenTitle
import com.justlorenzo.archivetunex.ui.components.SectionHeader
import com.justlorenzo.archivetunex.ui.components.SongRow
import com.justlorenzo.archivetunex.ui.components.animatedArtworkColor
import com.justlorenzo.archivetunex.ui.components.readableOn
import com.justlorenzo.archivetunex.ui.components.rememberAccount
import com.justlorenzo.archivetunex.ui.components.rememberArtworkColors
import com.justlorenzo.archivetunex.ui.components.rememberLoadable
import com.justlorenzo.archivetunex.ui.components.subtitle
import com.justlorenzo.archivetunex.ui.components.tint
import com.justlorenzo.archivetunex.ui.openItem
import moe.rukamori.archivetune.compat.time.LocalDateTime
import moe.rukamori.archivetune.innertube.YouTube
import moe.rukamori.archivetune.innertube.models.SongItem
import moe.rukamori.archivetune.innertube.models.YTItem
import moe.rukamori.archivetune.models.MediaMetadata
import moe.rukamori.archivetune.models.toMediaMetadata

private fun greeting(): String =
    when (LocalDateTime.now().hour) {
        in 5..12 -> tr("Buongiorno")
        in 13..17 -> tr("Buon pomeriggio")
        else -> tr("Buonasera")
    }

@Composable
fun HomeScreen() {
    val actions = LocalActions.current
    val navigator = LocalNavigator.current
    val account = rememberAccount()
    var chipParams by rememberSaveable { mutableStateOf<String?>(null) }
    // La Home cambia con login/logout e con il mood scelto
    val home = rememberLoadable(account?.name to chipParams) { YouTube.home(params = chipParams) }
    val recent by remember { actions.database.recentSongs(20) }.collectAsState(emptyList())
    val ready = (home.value as? LoadState.Ready)?.value

    PullToRefreshBox(
        isRefreshing = home.value is LoadState.Loading && ready != null,
        onRefresh = home::reload,
        modifier = Modifier.fillMaxSize(),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = BottomContentPadding,
        ) {
            item(key = "header") {
                ScreenTitle(
                    title = account?.name?.substringBefore(' ') ?: tr("Ascolta ora"),
                    overline = greeting(),
                ) { AccountButton(account) { navigator.accountVisible = true } }
            }

            val chips = ready?.chips.orEmpty()
            if (chips.isNotEmpty()) {
                item(key = "chips") {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(chips) { chip ->
                            val selected = chip.endpoint?.params != null && chip.endpoint?.params == chipParams
                            FilterChip(
                                selected = selected,
                                onClick = { chipParams = if (selected) null else chip.endpoint?.params },
                                label = { Text(chip.title, fontWeight = FontWeight.SemiBold) },
                                shape = RoundedCornerShape(20.dp),
                                colors =
                                    FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                    ),
                            )
                        }
                    }
                }
            }

            // In evidenza: il primo brano consigliato, grande e colorato
            val heroSong =
                ready?.sections?.firstNotNullOfOrNull { section -> section.items.firstOrNull { it is SongItem } as SongItem? }
            if (heroSong != null) {
                item(key = "hero") {
                    val metadata = remember(heroSong) { heroSong.toMediaMetadata() }
                    HeroCard(metadata, onPlay = { actions.playSong(heroSong) })
                }
            }

            if (recent.isNotEmpty()) {
                item(key = "recent") {
                    SectionHeader(tr("Ascoltati di recente"))
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(recent, key = { it.id }) { song ->
                            val metadata = remember(song) { song.toMediaMetadata() }
                            RecentSongCard(metadata, onClick = { actions.playSong(metadata) })
                        }
                    }
                }
            }

            when (val state = home.value) {
                LoadState.Loading -> if (ready == null) item(key = "loading") { LoadingBox() }
                is LoadState.Failed -> item(key = "error") { ErrorBox(state.message, home::reload) }
                is LoadState.Ready -> Unit
            }
            var carouselShown = false
            ready?.sections?.forEachIndexed { index, section ->
                if (section.items.isEmpty()) return@forEachIndexed
                val allSongs = section.items.all { it is SongItem }
                val useCarousel = !allSongs && !carouselShown && section.items.size >= 3
                if (useCarousel) carouselShown = true
                item(key = "section-$index-${section.title}") {
                    SectionHeader(section.title)
                    when {
                        allSongs -> SongGrid(section.items.map { it as SongItem })
                        useCarousel -> BigCarousel(section.items)
                        else ->
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
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

/** Card grande con la copertina, i colori del brano e un pulsante "biscotto". */
@Composable
private fun HeroCard(
    song: MediaMetadata,
    onPlay: () -> Unit,
) {
    val colored = com.justlorenzo.archivetunex.ui.LocalAtxStyle.current.coloredHome
    val accent =
        if (colored) {
            animatedArtworkColor(rememberArtworkColors(song.thumbnailUrl))
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        }
    val onAccent = if (colored) accent.readableOn() else MaterialTheme.colorScheme.onSurface
    Box(
        Modifier
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(32.dp))
            .background(accent)
            .clickable(onClick = onPlay),
    ) {
        Artwork(
            song.thumbnailUrl,
            Modifier.align(Alignment.CenterEnd).size(200.dp),
            shape = RoundedCornerShape(0.dp),
            sizePx = 600,
        )
        Box(
            Modifier.fillMaxSize().background(
                Brush.horizontalGradient(
                    0f to accent,
                    0.45f to accent,
                    0.85f to accent.copy(alpha = 0f),
                ),
            ),
        )
        Column(
            Modifier.align(Alignment.CenterStart).padding(start = 22.dp, end = 150.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                tr("PER TE ADESSO"),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
                color = onAccent.copy(alpha = 0.8f),
            )
            Text(
                song.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                color = onAccent,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                song.artists.joinToString { it.name },
                style = MaterialTheme.typography.bodyLarge,
                color = onAccent.copy(alpha = 0.85f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Box(
            Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .size(56.dp)
                .clip(MaterialShapes.Cookie9Sided.toShape())
                .background(onAccent),
            contentAlignment = Alignment.Center,
        ) {
            AtxIcon(Res.drawable.play, tr("Riproduci"), size = 30.dp, tint = accent)
        }
    }
}

/** Brani in righe scorrevoli (come "Scelte rapide" di YouTube Music). */
@Composable
private fun SongGrid(songs: List<SongItem>) {
    val actions = LocalActions.current
    val rows = minOf(4, songs.size)
    LazyHorizontalGrid(
        rows = GridCells.Fixed(rows),
        modifier = Modifier.height((rows * 64).dp),
    ) {
        items(songs) { song ->
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
}

/** Carosello Expressive: elementi grandi che si restringono ai bordi. */
@Composable
private fun BigCarousel(items: List<YTItem>) {
    val actions = LocalActions.current
    val navigator = LocalNavigator.current
    val state = rememberCarouselState { items.size }
    HorizontalMultiBrowseCarousel(
        state = state,
        preferredItemWidth = 240.dp,
        itemSpacing = 10.dp,
        contentPadding = PaddingValues(horizontal = 16.dp),
        modifier = Modifier.fillMaxWidth().height(240.dp),
    ) { index ->
        val item = items[index]
        Box(
            Modifier
                .fillMaxSize()
                .maskClip(RoundedCornerShape(32.dp))
                .clickable { openItem(item, actions, navigator) },
        ) {
            Artwork(item.thumbnail, Modifier.fillMaxSize(), RoundedCornerShape(0.dp), sizePx = 720)
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(0.5f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.75f))),
            )
            Column(Modifier.align(Alignment.BottomStart).padding(16.dp)) {
                Text(item.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(item.subtitle(), style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.8f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun RecentSongCard(
    song: MediaMetadata,
    onClick: () -> Unit,
) {
    val colored = com.justlorenzo.archivetunex.ui.LocalAtxStyle.current.coloredHome
    val accent =
        if (colored) {
            animatedArtworkColor(rememberArtworkColors(song.thumbnailUrl), MaterialTheme.colorScheme.surfaceContainerHigh)
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        }
    Column(
        Modifier
            .width(136.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(accent.tint(MaterialTheme.colorScheme.surfaceContainer, 0.35f))
            .clickable(onClick = onClick)
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Artwork(song.thumbnailUrl, Modifier.size(120.dp), RoundedCornerShape(18.dp), sizePx = 300)
        Text(
            song.title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        Text(
            song.artists.joinToString { it.name },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 4.dp),
        )
    }
}
