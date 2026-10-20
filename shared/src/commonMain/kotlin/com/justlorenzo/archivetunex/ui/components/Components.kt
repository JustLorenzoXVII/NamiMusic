package com.justlorenzo.archivetunex.ui.components

import androidx.compose.foundation.basicMarquee
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
import androidx.compose.ui.graphics.graphicsLayer
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
import archivetunex.shared.generated.resources.more_horiz
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
    // Tema iOS: la stessa icona, ma la SF Symbol di Apple disegnata dal sistema
    if (com.justlorenzo.archivetunex.ui.LocalAtxStyle.current.liquidGlass) {
        val symbol = com.justlorenzo.archivetunex.ui.ios.iosSymbolFor(icon)
        val image = symbol?.let { com.justlorenzo.archivetunex.ui.ios.rememberSystemSymbol(it, size.value * 0.82f) }
        if (image != null) {
            Icon(image, contentDescription, modifier.size(size), tint = tint)
            return
        }
    }
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
    previewSizePx: Int? = null,
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
            // Le copertine delle playlist automatiche di YouTube (Mi piace, Episodi…) hanno gli angoli già
            // smussati nell'immagine: ingrandite un po', quegli angoli restano fuori dalla cornice
            val imageModifier =
                if (url.contains("gstatic.com") || url.contains("ytm/images")) {
                    Modifier.fillMaxSize().graphicsLayer {
                        scaleX = 1.12f
                        scaleY = 1.12f
                    }
                } else {
                    Modifier.fillMaxSize()
                }
            // Anteprima piccola (già in memoria, la usa il mini player): niente vuoti mentre arriva quella grande
            if (previewSizePx != null && previewSizePx != sizePx) {
                AsyncImage(
                    model =
                        remember(url, previewSizePx) {
                            coil3.request.ImageRequest
                                .Builder(context)
                                .data(url.resize(previewSizePx, previewSizePx))
                                .size(previewSizePx, previewSizePx)
                                .build()
                        },
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    colorFilter = colorFilter,
                    modifier = imageModifier,
                )
            }
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
                modifier = imageModifier,
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
    if (com.justlorenzo.archivetunex.ui.ios.iosStyle) {
        // Apple Music: titolo di sezione in grassetto con la freccetta grigia accanto, tutto toccabile
        Row(
            modifier = modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                Modifier.then(if (onMore != null) Modifier.clickable(interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, indication = null, onClick = onMore) else Modifier),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(title, style = MaterialTheme.typography.headlineSmall, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                if (onMore != null) AtxIcon(Res.drawable.navigate_next, tr("Altro"), size = 18.dp, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        return
    }
    Row(
        modifier = modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 20.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = com.justlorenzo.archivetunex.ui.ios.extraBoldWeight,
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
            if (com.justlorenzo.archivetunex.ui.ios.iosStyle) {
                androidx.compose.material3.CircularProgressIndicator(
                    progress = { ((download?.percentDownloaded ?: 0f) / 100f).coerceIn(0f, 1f) },
                    modifier = modifier.size(14.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = com.justlorenzo.archivetunex.ui.ios.iosFill,
                    gapSize = 0.dp,
                    strokeCap = androidx.compose.ui.graphics.StrokeCap.Round,
                )
            } else {
            CircularWavyProgressIndicator(
                progress = { ((download?.percentDownloaded ?: 0f) / 100f).coerceIn(0f, 1f) },
                modifier = modifier.size(16.dp),
            )
            }
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
    if (com.justlorenzo.archivetunex.ui.ios.iosStyle) {
        IosSongRow(songId, title, subtitle, artworkUrl, onClick, modifier, explicit, isCurrent, leadingNumber, onMore)
        return
    }
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

/** Riga brano di Apple Music: copertina piccola, titolo normale, artista grigio, separatore sottile. */
@Composable
private fun IosSongRow(
    songId: String,
    title: String,
    subtitle: String,
    artworkUrl: String?,
    onClick: () -> Unit,
    modifier: Modifier,
    explicit: Boolean,
    isCurrent: Boolean,
    leadingNumber: Int?,
    onMore: (() -> Unit)?,
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = modifier.fillMaxWidth().height(60.dp).clickable(onClick = onClick).padding(start = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leadingNumber != null) {
            Text(
                leadingNumber.toString(),
                style = MaterialTheme.typography.bodyLarge,
                color = if (isCurrent) scheme.primary else scheme.onSurfaceVariant,
                modifier = Modifier.width(28.dp),
            )
        } else {
            Artwork(artworkUrl, Modifier.size(48.dp), androidx.compose.foundation.shape.RoundedCornerShape(6.dp), sizePx = 226)
        }
        Spacer(Modifier.width(12.dp))
        Box(Modifier.weight(1f).fillMaxSize()) {
            Row(Modifier.fillMaxSize().padding(end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text(
                        title,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (isCurrent) scheme.primary else scheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (explicit) AtxIcon(Res.drawable.explicit, tr("Esplicito"), size = 13.dp, tint = scheme.onSurfaceVariant)
                        DownloadBadge(songId)
                        Text(
                            subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = scheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                if (onMore != null) {
                    Box(
                        Modifier.size(44.dp).clickable(interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, indication = null, onClick = onMore),
                        contentAlignment = Alignment.Center,
                    ) { AtxIcon(Res.drawable.more_horiz, tr("Altre opzioni"), size = 20.dp, tint = scheme.onSurface) }
                }
            }
            // Separatore sottile sotto il testo (non sotto la copertina), come nelle liste di iOS
            Box(Modifier.align(Alignment.BottomStart).fillMaxWidth().height(0.5.dp).background(scheme.outlineVariant))
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
        modifier =
            if (com.justlorenzo.archivetunex.ui.ios.iosStyle) {
                // iOS: niente riquadro che si accende, si vela solo la copertina al tocco
                modifier.width(width).clickable(interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, indication = null, onClick = onClick).padding(4.dp)
            } else {
                modifier.width(width).clip(MaterialTheme.shapes.medium).clickable(onClick = onClick).padding(4.dp)
            },
        verticalArrangement = Arrangement.spacedBy(if (com.justlorenzo.archivetunex.ui.ios.iosStyle) 6.dp else 8.dp),
    ) {
        Artwork(
            item.thumbnail,
            Modifier.size(width - 8.dp),
            shape = if (item is ArtistItem) ArtistShape else androidx.compose.foundation.shape.RoundedCornerShape(com.justlorenzo.archivetunex.ui.LocalAtxStyle.current.corners.radius),
            sizePx = 400,
        )
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            val ios = com.justlorenzo.archivetunex.ui.ios.iosStyle
            Text(
                item.title,
                style = if (ios) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.titleSmall,
                fontWeight = if (ios) FontWeight.Medium else FontWeight.SemiBold,
                maxLines = if (ios) 1 else 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = if (ios && item is ArtistItem) androidx.compose.ui.text.style.TextAlign.Center else androidx.compose.ui.text.style.TextAlign.Unspecified,
                modifier = if (ios && item is ArtistItem) Modifier.fillMaxWidth() else Modifier,
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
    @Composable get() = if (com.justlorenzo.archivetunex.ui.ios.iosStyle) CircleShape else MaterialShapes.Cookie9Sided.toShape()

/** Titolo grande delle schermate principali, con una riga sopra (es. il saluto). */
@Composable
fun ScreenTitle(
    title: String,
    modifier: Modifier = Modifier,
    overline: String? = null,
    trailing: @Composable () -> Unit = {},
) {
    if (com.justlorenzo.archivetunex.ui.ios.iosStyle) {
        // Titolo grande di iOS (Large Title): niente riga sopra, 34 punti in grassetto
        Row(
            modifier.fillMaxWidth().statusBarsPadding().padding(start = 20.dp, end = 16.dp, top = 10.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                title,
                style = MaterialTheme.typography.displaySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            trailing()
        }
        return
    }
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
                fontWeight = com.justlorenzo.archivetunex.ui.ios.heavyWeight,
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
        com.justlorenzo.archivetunex.ui.ios.AtxLoading(contained = true)
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
        com.justlorenzo.archivetunex.ui.ios.AtxButton(onClick = onRetry) { Text(tr("Riprova")) }
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

/**
 * Testo su una riga che, se non ci sta, scorre fino alla fine, torna all'inizio, si ferma un attimo
 * e ricomincia (come nelle app di musica). Se il testo ci sta, resta fermo.
 */
fun Modifier.scrollingText(): Modifier =
    basicMarquee(
        iterations = Int.MAX_VALUE,
        initialDelayMillis = 2000,
        repeatDelayMillis = 2000,
        velocity = 40.dp,
    )
