@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.justlorenzo.archivetunex.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import archivetunex.shared.generated.resources.Res
import archivetunex.shared.generated.resources.stats
import com.justlorenzo.archivetunex.insights.ArtistResolver
import com.justlorenzo.archivetunex.insights.GenreService
import com.justlorenzo.archivetunex.insights.Insights
import com.justlorenzo.archivetunex.insights.ListeningSignals
import com.justlorenzo.archivetunex.insights.ListeningStats
import com.justlorenzo.archivetunex.insights.StatsPeriod
import com.justlorenzo.archivetunex.ui.LocalActions
import com.justlorenzo.archivetunex.ui.LocalNavigator
import com.justlorenzo.archivetunex.ui.Route
import com.justlorenzo.archivetunex.ui.components.AccountButton
import com.justlorenzo.archivetunex.ui.components.ArtistShape
import com.justlorenzo.archivetunex.ui.components.Artwork
import com.justlorenzo.archivetunex.ui.components.BottomContentPadding
import com.justlorenzo.archivetunex.ui.components.EmptyState
import com.justlorenzo.archivetunex.ui.components.ScreenTitle
import com.justlorenzo.archivetunex.ui.components.SectionHeader
import com.justlorenzo.archivetunex.ui.components.rememberAccount
import com.justlorenzo.archivetunex.ui.tr
import com.justlorenzo.archivetunex.ui.trf

/** Statistiche d'ascolto: quanto, cosa, quando, generi e cosa salti. Tutto calcolato sul telefono. */
@Composable
fun StatsScreen() {
    val actions = LocalActions.current
    val navigator = LocalNavigator.current
    val account = rememberAccount()
    var period by rememberSaveable { mutableStateOf(StatsPeriod.Month) }
    val genres by GenreService.genres.collectAsState()
    val signals by ListeningSignals.signals.collectAsState()
    val resolvedArtists by ArtistResolver.resolved.collectAsState()
    var loading by remember { mutableStateOf(true) }
    val stats by produceState<ListeningStats?>(null, period, genres, signals) {
        loading = true
        value = runCatching { Insights.compute(actions.database, period) }.getOrNull() ?: ListeningStats()
        loading = false
    }
    // Generi: si chiedono a MusicBrainz quelli degli artisti che ancora non si conoscono
    var fetchingGenres by remember { mutableStateOf(false) }
    val missing = stats?.artistsForGenres.orEmpty()
    LaunchedEffect(missing.take(15)) {
        if (missing.isEmpty()) return@LaunchedEffect
        fetchingGenres = true
        runCatching { GenreService.fetchMissing(missing, limit = 15) }
        fetchingGenres = false
    }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = BottomContentPadding) {
        item(key = "header") {
            ScreenTitle(tr("Statistiche"), overline = tr("Le tue abitudini")) {
                AccountButton(account) { navigator.accountVisible = true }
            }
        }
        item(key = "periods") {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(StatsPeriod.entries) { entry ->
                    FilterChip(
                        selected = entry == period,
                        onClick = { period = entry },
                        label = { Text(tr(entry.rawLabel), fontWeight = FontWeight.SemiBold) },
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

        val current = stats
        if (current == null || (loading && current.plays == 0)) {
            item(key = "loading") {
                Box(Modifier.fillMaxWidth().padding(top = 80.dp), contentAlignment = Alignment.Center) {
                    LoadingIndicator(Modifier.size(56.dp))
                }
            }
            return@LazyColumn
        }
        if (current.plays == 0) {
            item(key = "empty") {
                EmptyState(
                    tr("Ancora niente da mostrare per questo periodo. Ascolta un po' di musica e torna qui!"),
                    icon = Res.drawable.stats,
                )
            }
            return@LazyColumn
        }

        item(key = "hero") {
            HeroTotals(current)
            // Foto e pagina vera degli artisti più ascoltati (cercati su YouTube Music una volta sola)
            LaunchedEffect(current.topArtists.map { it.name }) {
                runCatching { ArtistResolver.resolve(current.topArtists.map { it.name to it.id }) }
            }
        }

        // Brani più ascoltati
        item(key = "songs-title") { SectionHeader(tr("Brani più ascoltati")) }
        itemsIndexed(current.topSongs, key = { _, s -> "song-" + s.song.id }) { index, stat ->
            RankRow(
                rank = index + 1,
                artworkUrl = stat.song.thumbnailUrl,
                title = stat.song.title,
                subtitle = stat.song.artists.joinToString { it.name },
                trailing = trf("{0} ascolti", stat.plays),
                onClick = { actions.playList(current.topSongs.map { it.song }, startIndex = index, title = tr("Brani più ascoltati")) },
            )
        }

        // Artisti più ascoltati
        if (current.topArtists.isNotEmpty()) {
            item(key = "artists") {
                SectionHeader(tr("Artisti più ascoltati"))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    itemsIndexed(current.topArtists, key = { _, a -> a.name }) { index, artist ->
                        val found = resolvedArtists[artist.name.lowercase()]
                        // Si apre solo un artista che esiste su YouTube Music (i codici interni non si aprono)
                        val pageId = found?.browseId ?: artist.id?.takeIf { ArtistResolver.isYouTubeId(it) }
                        val photo = artist.thumbnailUrl ?: found?.thumbnailUrl
                        Column(
                            Modifier
                                .width(104.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .clickable(enabled = pageId != null) { pageId?.let { navigator.open(Route.Artist(it)) } }
                                .padding(4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Box {
                                Artwork(photo, Modifier.size(96.dp), ArtistShape, sizePx = 300)
                                Box(
                                    Modifier
                                        .align(Alignment.BottomEnd)
                                        .size(30.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text("${index + 1}", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Black, style = MaterialTheme.typography.labelLarge)
                                }
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(artist.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(formatMinutes(artist.timeMs), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        // Generi
        item(key = "genres") {
            SectionHeader(tr("I tuoi generi"))
            GenresCard(current, fetchingGenres)
        }

        // Quando ascolti
        item(key = "hours") {
            SectionHeader(tr("Quando ascolti"))
            HoursCard(current.byHour)
        }

        // Cosa salti
        item(key = "skips-title") { SectionHeader(tr("Brani che salti")) }
        if (current.skippedSongs.isEmpty()) {
            item(key = "skips-empty") {
                Text(
                    tr("Nessun brano saltato in questo periodo. I salti si registrano da quando hai aggiornato l'app."),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                )
            }
        } else {
            itemsIndexed(current.skippedSongs.take(6), key = { _, s -> "skip-" + s.id }) { index, skip ->
                RankRow(
                    rank = index + 1,
                    artworkUrl = skip.thumbnailUrl,
                    title = skip.title,
                    subtitle = skip.artists,
                    trailing = trf("{0} salti", skip.skips),
                    trailingColor = MaterialTheme.colorScheme.error,
                    onClick = null,
                )
            }
            if (current.skippedArtists.isNotEmpty()) {
                item(key = "skip-artists") {
                    Column(Modifier.padding(horizontal = 20.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(tr("Artisti che salti più spesso"), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            current.skippedArtists.forEach { (name, count) -> Pill("$name · $count", MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer) }
                        }
                        Text(
                            tr("Brani e artisti che salti spesso restano fuori dai mix “Fatti per te”."),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroTotals(stats: ListeningStats) {
    val scheme = MaterialTheme.colorScheme
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(32.dp))
            .background(Brush.linearGradient(listOf(scheme.primary, scheme.tertiary)))
            .padding(24.dp),
    ) {
        Text(tr("Hai ascoltato"), style = MaterialTheme.typography.titleMedium, color = scheme.onPrimary.copy(alpha = 0.85f))
        Text(
            formatHours(stats.totalMs),
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Black,
            color = scheme.onPrimary,
            letterSpacing = (-1).sp,
        )
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            HeroNumber(stats.plays.toString(), tr("ascolti"), Modifier.weight(1f))
            HeroNumber(stats.distinctSongs.toString(), tr("brani"), Modifier.weight(1f))
            HeroNumber(stats.distinctArtists.toString(), tr("artisti"), Modifier.weight(1f))
        }
    }
}

@Composable
private fun HeroNumber(
    value: String,
    label: String,
    modifier: Modifier,
) {
    Column(
        modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.16f))
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onPrimary)
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f))
    }
}

@Composable
private fun RankRow(
    rank: Int,
    artworkUrl: String?,
    title: String,
    subtitle: String,
    trailing: String,
    onClick: (() -> Unit)?,
    trailingColor: Color = MaterialTheme.colorScheme.primary,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 20.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            rank.toString(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Black,
            color = if (rank <= 3) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(22.dp),
        )
        Artwork(artworkUrl, Modifier.size(52.dp), RoundedCornerShape(12.dp), sizePx = 200)
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text(trailing, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = trailingColor)
    }
}

@Composable
private fun GenresCard(
    stats: ListeningStats,
    fetching: Boolean,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (stats.genres.isEmpty()) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (fetching) LoadingIndicator(Modifier.size(32.dp))
                Text(
                    if (fetching) tr("Sto scoprendo i generi dei tuoi artisti…") else tr("Generi non ancora disponibili. Riprova tra poco (serve internet)."),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            return@Column
        }
        stats.genres.take(6).forEachIndexed { index, share ->
            val target = share.share
            val animated by animateFloatAsState(target, tween(700, delayMillis = index * 80), label = "genre")
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row {
                    Text(tr(share.genre.label), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Text("${(target * 100).toInt()}%", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(12.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(animated.coerceIn(0.02f, 1f))
                            .height(12.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.primary),
                    )
                }
            }
        }
        if (stats.neverGenres.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            Text(tr("Non ascolti quasi mai"), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                stats.neverGenres.forEach { Pill(tr(it.label), MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer) }
            }
        }
        if (fetching || stats.genreCoverage < 0.6f) {
            Text(
                if (fetching) tr("Sto ancora analizzando alcuni artisti…") else trf("Basato sul {0}% del tuo ascolto.", (stats.genreCoverage * 100).toInt()),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun HoursCard(byHour: List<Long>) {
    val max = byHour.maxOrNull()?.takeIf { it > 0 } ?: 1L
    val peak = byHour.indices.maxByOrNull { byHour[it] } ?: 0
    val moment =
        when (peak) {
            in 5..11 -> tr("Ascolti soprattutto la mattina")
            in 12..17 -> tr("Ascolti soprattutto il pomeriggio")
            in 18..22 -> tr("Ascolti soprattutto la sera")
            else -> tr("Ascolti soprattutto di notte")
        }
    val bar = MaterialTheme.colorScheme.primary
    val dim = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(moment, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Canvas(Modifier.fillMaxWidth().height(120.dp)) {
            val gap = 3.dp.toPx()
            val w = (size.width - gap * 23) / 24f
            byHour.forEachIndexed { hour, value ->
                val h = (value.toFloat() / max) * size.height
                val height = h.coerceAtLeast(4.dp.toPx())
                drawRoundRect(
                    color = if (hour == peak) bar else dim,
                    topLeft = Offset(hour * (w + gap), size.height - height),
                    size = Size(w, height),
                    cornerRadius = CornerRadius(w / 2, w / 2),
                )
            }
        }
        Row(Modifier.fillMaxWidth()) {
            listOf("0", "6", "12", "18", "24").forEachIndexed { i, label ->
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                    textAlign =
                        when (i) {
                            0 -> androidx.compose.ui.text.style.TextAlign.Start
                            4 -> androidx.compose.ui.text.style.TextAlign.End
                            else -> androidx.compose.ui.text.style.TextAlign.Center
                        },
                )
            }
        }
    }
}

@Composable
private fun Pill(
    text: String,
    background: Color,
    content: Color,
) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = content,
        modifier = Modifier.clip(RoundedCornerShape(16.dp)).background(background).padding(horizontal = 14.dp, vertical = 8.dp),
    )
}

private fun formatHours(ms: Long): String {
    val minutes = ms / 60_000
    val hours = minutes / 60
    return if (hours > 0) trf("{0} h {1} min", hours, minutes % 60) else trf("{0} min", minutes)
}

private fun formatMinutes(ms: Long): String {
    val minutes = ms / 60_000
    return if (minutes >= 60) trf("{0} h", minutes / 60) else trf("{0} min", minutes)
}
