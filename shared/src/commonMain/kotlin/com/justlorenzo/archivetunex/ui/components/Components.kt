package com.justlorenzo.archivetunex.ui.components

import com.justlorenzo.archivetunex.ui.tr
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.toShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import archivetunex.shared.generated.resources.Res
import archivetunex.shared.generated.resources.check
import archivetunex.shared.generated.resources.download
import archivetunex.shared.generated.resources.explicit
import archivetunex.shared.generated.resources.more_vert
import archivetunex.shared.generated.resources.music_note
import archivetunex.shared.generated.resources.navigate_next
import coil3.compose.AsyncImage
import com.justlorenzo.archivetunex.ui.LocalActions
import moe.rukamori.archivetune.compat.media3.Download
import moe.rukamori.archivetune.innertube.models.AlbumItem
import moe.rukamori.archivetune.innertube.models.ArtistItem
import moe.rukamori.archivetune.innertube.models.EpisodeItem
import moe.rukamori.archivetune.innertube.models.PlaylistItem
import moe.rukamori.archivetune.innertube.models.PodcastItem
import moe.rukamori.archivetune.innertube.models.SongItem
import moe.rukamori.archivetune.innertube.models.YTItem
import moe.rukamori.archivetune.ui.utils.resize
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

/** Icona dalle risorse (Material Symbols Rounded, come in Fuji Music). */
@Composable
fun AtxIcon(
    icon: DrawableResource,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    tint: androidx.compose.ui.graphics.Color = androidx.compose.material3.LocalContentColor.current,
) {
    Icon(painterResource(icon), contentDescription, modifier.size(size), tint = tint)
}

/** Copertina con angoli arrotondati; finché non arriva mostra una nota musicale. */
@Composable
fun Artwork(
    url: String?,
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.medium,
    sizePx: Int = 544,
    colorFilter: androidx.compose.ui.graphics.ColorFilter? = null,
) {
    Box(
        modifier = modifier.clip(shape).background(MaterialTheme.colorScheme.surfaceContainerHigh),
        contentAlignment = Alignment.Center,
    ) {
        AtxIcon(
            Res.drawable.music_note,
            contentDescription = null,
            size = 28.dp,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (!url.isNullOrBlank()) {
            val context = coil3.compose.LocalPlatformContext.current
            AsyncImage(
                // Grandezza fissa: senza, Coil la prende dal riquadro al primo disegno e non la cambia più
                // (la copertina del player parte piccola dal mini player e resterebbe sgranata)
                model =
                    remember(url, sizePx) {
                        coil3.request.ImageRequest
                            .Builder(context)
                            .data(url.resize(sizePx, sizePx))
                            .size(sizePx, sizePx)
                            .build()
                    },
                contentDescription = null,
                contentScale = ContentScale.Crop,
                colorFilter = colorFilter,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

/** Titolo di una sezione, con "Altro" se c'è una pagina completa. */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    onMore: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 20.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = (-0.5).sp,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (onMore != null) {
            IconButton(onClick = onMore) {
                AtxIcon(Res.drawable.navigate_next, tr("Altro"))
            }
        }
    }
}

/** Stato del download di un brano: segno di spunta, cerchio di avanzamento o niente. */
@Composable
fun DownloadBadge(
    songId: String,
    modifier: Modifier = Modifier,
) {
    val actions = LocalActions.current
    val download by remember(songId) { actions.downloads.getDownload(songId) }.collectAsState(null)
    when (download?.state) {
        Download.STATE_COMPLETED ->
            AtxIcon(
                Res.drawable.check,
                contentDescription = tr("Scaricato"),
                size = 16.dp,
                tint = MaterialTheme.colorScheme.primary,
                modifier = modifier,
            )
        Download.STATE_DOWNLOADING, Download.STATE_QUEUED, Download.STATE_RESTARTING ->
            CircularWavyProgressIndicator(
                progress = { ((download?.percentDownloaded ?: 0f) / 100f).coerceIn(0f, 1f) },
                modifier = modifier.size(16.dp),
            )
        else -> Unit
    }
}

/** Una riga brano: copertina, titolo, artisti, stato del download e menu. */
@Composable
fun SongRow(
    songId: String,
    title: String,
    subtitle: String,
    artworkUrl: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    explicit: Boolean = false,
    isCurrent: Boolean = false,
    leadingNumber: Int? = null,
    onMore: (() -> Unit)? = null,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(start = 20.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (leadingNumber != null) {
            Text(
                leadingNumber.toString(),
                style = MaterialTheme.typography.bodyLarge,
                color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(24.dp),
            )
        } else {
            Artwork(artworkUrl, Modifier.size(52.dp), MaterialTheme.shapes.small, sizePx = 226)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                DownloadBadge(songId)
                if (explicit) {
                    AtxIcon(Res.drawable.explicit, tr("Esplicito"), size = 16.dp, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (onMore != null) {
            IconButton(onClick = onMore) {
                AtxIcon(Res.drawable.more_vert, tr("Altre opzioni"), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/** Testo secondario di un elemento di YouTube Music. */
fun YTItem.subtitle(): String =
    when (this) {
        is SongItem -> listOfNotNull(artists.joinToString { it.name }.takeIf(String::isNotBlank), album?.name).joinToString(" · ")
        is AlbumItem -> listOfNotNull(artists?.joinToString { it.name }?.takeIf(String::isNotBlank), year?.toString()).joinToString(" · ")
        is PlaylistItem -> listOfNotNull(tr("Playlist"), author?.name, songCountText).joinToString(" · ")
        is ArtistItem -> subscriberCountText ?: tr("Artista")
        is PodcastItem -> listOfNotNull(tr("Podcast"), author?.name).joinToString(" · ")
        is EpisodeItem -> listOfNotNull(podcast?.name, dateText).joinToString(" · ")
    }

/** Card quadrata per i caroselli (album, playlist, artisti tondi, brani). */
@Composable
fun ItemCard(
    item: YTItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    width: Dp = 168.dp,
) {
    Column(
        modifier = modifier.width(width).clip(MaterialTheme.shapes.medium).clickable(onClick = onClick).padding(4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Artwork(
            item.thumbnail,
            Modifier.size(width - 8.dp),
            shape = if (item is ArtistItem) ArtistShape else androidx.compose.foundation.shape.RoundedCornerShape(com.justlorenzo.archivetunex.ui.LocalAtxStyle.current.corners.radius),
            sizePx = 400,
        )
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                item.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                item.subtitle(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Forma "biscotto" di Material 3 Expressive per i volti degli artisti. */
val ArtistShape: Shape
    @Composable get() = MaterialShapes.Cookie9Sided.toShape()

/** Titolo grande delle schermate principali, con una riga sopra (es. il saluto). */
@Composable
fun ScreenTitle(
    title: String,
    modifier: Modifier = Modifier,
    overline: String? = null,
    trailing: @Composable () -> Unit = {},
) {
    Row(
        modifier.fillMaxWidth().statusBarsPadding().padding(start = 20.dp, end = 8.dp, top = 16.dp, bottom = 4.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        Column(Modifier.weight(1f)) {
            if (overline != null) {
                Text(
                    overline.uppercase(),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Text(
                title,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Black,
                letterSpacing = (-1.5).sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        trailing()
    }
}

/** Caricamento con l'indicatore Expressive. */
@Composable
fun LoadingBox(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
        ContainedLoadingIndicator()
    }
}

/** Errore con pulsante "Riprova". */
@Composable
fun ErrorBox(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(onClick = onRetry) { Text(tr("Riprova")) }
    }
}

/** Spazio in fondo alle liste, per non finire sotto mini player e barra delle schede. */
val BottomContentPadding = PaddingValues(bottom = 210.dp)

@Composable
fun EmptyState(
    text: String,
    modifier: Modifier = Modifier,
    icon: DrawableResource = Res.drawable.download,
) {
    Column(
        modifier.fillMaxWidth().padding(vertical = 64.dp, horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AtxIcon(icon, null, size = 40.dp, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
    }
}
