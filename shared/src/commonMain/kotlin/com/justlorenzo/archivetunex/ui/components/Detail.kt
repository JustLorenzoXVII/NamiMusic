@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.justlorenzo.archivetunex.ui.components

import com.justlorenzo.archivetunex.ui.ios.iosGlass
import androidx.compose.foundation.layout.Spacer
import org.jetbrains.compose.resources.DrawableResource
import archivetunex.shared.generated.resources.more_vert
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.material3.toShape
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenu
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.LocalIndication
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.animateFloatAsState
import com.justlorenzo.archivetunex.ui.tr
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import archivetunex.shared.generated.resources.Res
import archivetunex.shared.generated.resources.delete
import archivetunex.shared.generated.resources.arrow_back
import archivetunex.shared.generated.resources.download
import archivetunex.shared.generated.resources.play
import archivetunex.shared.generated.resources.shuffle
import com.justlorenzo.archivetunex.ui.LocalNavigator

/** Un'azione del menu "Altro" (⋮) delle pagine di album, playlist e artisti. */
class DetailMenuItem(
    val label: String,
    val icon: DrawableResource,
    val danger: Boolean = false,
    val onClick: () -> Unit,
)

/** Pulsante a due stati (Salva in libreria, Segui). */
class DetailToggle(
    val active: Boolean,
    val labelOn: String,
    val labelOff: String,
    val iconOn: DrawableResource,
    val iconOff: DrawableResource,
    val onClick: () -> Unit,
)

/**
 * Barra delle azioni: a sinistra le azioni secondarie come icone semplici (Salva, Scarica, Altro),
 * a destra Casuale in un cerchio tonale e il grande Play pieno, che si "schiaccia" quando lo premi.
 */
@Composable
fun PlayShuffleButtons(
    onPlay: (() -> Unit)?,
    onShuffle: (() -> Unit)?,
    modifier: Modifier = Modifier,
    onDownload: (() -> Unit)? = null,
    toggle: DetailToggle? = null,
    menu: List<DetailMenuItem> = emptyList(),
    onDelete: (() -> Unit)? = null,
    playLabel: String = tr("Riproduci"),
    shuffleLabel: String = tr("Casuale"),
    shuffleIcon: DrawableResource = Res.drawable.shuffle,
) {
    val ios = glassEnabled
    androidx.compose.foundation.layout.Column(modifier.fillMaxWidth()) {
    // Tema iOS: Riproduci e Casuale sono due capsule grigie larghe, come in Apple Music
    if (ios && (onPlay != null || onShuffle != null)) {
        Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            if (onPlay != null) IosActionCapsule(Res.drawable.play, playLabel, onPlay, Modifier.weight(1f))
            if (onShuffle != null) IosActionCapsule(shuffleIcon, shuffleLabel, onShuffle, Modifier.weight(1f))
        }
    }
    Row(
        Modifier.fillMaxWidth().padding(top = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (ios) Arrangement.Center else Arrangement.Start,
    ) {
        // Azioni secondarie: solo icone, discrete
        if (toggle != null) {
            IconButton(onClick = toggle.onClick) {
                AtxIcon(
                    if (toggle.active) toggle.iconOn else toggle.iconOff,
                    if (toggle.active) toggle.labelOn else toggle.labelOff,
                    size = 26.dp,
                    tint = if (toggle.active) MaterialTheme.colorScheme.primary else if (ios) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (onDownload != null) {
            IconButton(onClick = onDownload) {
                AtxIcon(Res.drawable.download, tr("Scarica tutto"), size = if (ios) 22.dp else 26.dp, tint = if (ios) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (menu.isNotEmpty()) {
            var open by remember { mutableStateOf(false) }
            Box {
                IconButton(onClick = { open = true }) {
                    AtxIcon(Res.drawable.more_vert, tr("Altro"), size = if (ios) 22.dp else 26.dp, tint = if (ios) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                }
                com.justlorenzo.archivetunex.ui.ios.AtxDropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                    menu.forEach { item ->
                        com.justlorenzo.archivetunex.ui.ios.AtxMenuItem(
                            text = item.label,
                            icon = item.icon,
                            danger = item.danger,
                            onClick = {
                                open = false
                                item.onClick()
                            },
                        )
                    }
                }
            }
        }
        if (onDelete != null) {
            IconButton(onClick = onDelete) {
                AtxIcon(Res.drawable.delete, tr("Elimina"), size = if (ios) 22.dp else 26.dp, tint = if (ios) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (!ios) {
        Spacer(Modifier.weight(1f))
        // Azioni principali: Casuale e Play
        if (onShuffle != null) {
            PressableCircle(
                size = 52.dp,
                container = MaterialTheme.colorScheme.secondaryContainer,
                onClick = onShuffle,
            ) { AtxIcon(shuffleIcon, shuffleLabel, size = 24.dp, tint = MaterialTheme.colorScheme.onSecondaryContainer) }
            Spacer(Modifier.width(12.dp))
        }
        if (onPlay != null) {
            PressableCircle(
                size = 64.dp,
                container = MaterialTheme.colorScheme.primary,
                onClick = onPlay,
            ) { AtxIcon(Res.drawable.play, playLabel, size = 32.dp, tint = MaterialTheme.colorScheme.onPrimary) }
        }
        }
    }
    }
}

/** Capsula grigia con icona e testo nel colore d'accento (pulsanti di Apple Music). */
@Composable
private fun IosActionCapsule(
    icon: DrawableResource,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.96f else 1f, spring(dampingRatio = 0.6f, stiffness = 700f), label = "capsule")
    Row(
        modifier
            .height(50.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = if (pressed) 0.7f else 1f
            }.iosGlass(CircleShape)
            .clickable(interactionSource = source, indication = null, onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AtxIcon(icon, null, size = 20.dp, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(8.dp))
        Text(label, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
    }
}

/** Pulsante tondo che, premuto, diventa un quadrato arrotondato e si rimpicciolisce un po' (Expressive). */
@Composable
private fun PressableCircle(
    size: Dp,
    container: Color,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val radius by androidx.compose.animation.core.animateDpAsState(
        if (pressed) size * 0.28f else size / 2,
        spring(dampingRatio = 0.5f, stiffness = 600f),
        label = "pressRadius",
    )
    val scale by animateFloatAsState(if (pressed) 0.92f else 1f, spring(dampingRatio = 0.5f, stiffness = 600f), label = "pressScale")
    Box(
        Modifier
            .size(size)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }.clip(RoundedCornerShape(radius))
            .background(container)
            .clickable(interactionSource = source, indication = LocalIndication.current, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { content() }
}

/** Freccia indietro in alto a sinistra delle pagine aperte. */
@Composable
fun BackButton(modifier: Modifier = Modifier) {
    val navigator = LocalNavigator.current
    if (glassEnabled) {
        // Tema iOS: tondo di vetro che galleggia sopra la copertina, come in Apple Music
        GlassIconButton(
            Res.drawable.arrow_back,
            tr("Indietro"),
            onClick = { navigator.back() },
            modifier = modifier.statusBarsPadding().padding(start = 16.dp, top = 6.dp),
        )
        return
    }
    Box(modifier.statusBarsPadding().padding(start = 4.dp, top = 4.dp)) {
        IconButton(onClick = { navigator.back() }) {
            AtxIcon(Res.drawable.arrow_back, tr("Indietro"))
        }
    }
}

/** Intestazione di album, playlist e artisti: copertina grande, titolo, dettagli e pulsanti. */
@Composable
fun DetailHeader(
    artworkUrl: String?,
    title: String,
    subtitle: String?,
    detail: String?,
    modifier: Modifier = Modifier,
    artworkShape: Shape = androidx.compose.foundation.shape.RoundedCornerShape(com.justlorenzo.archivetunex.ui.LocalAtxStyle.current.corners.radius),
    artwork: (@Composable () -> Unit)? = null,
    buttons: @Composable () -> Unit,
) {
    Column(
        modifier.fillMaxWidth().padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (artwork != null) {
            artwork()
        } else {
            // L'ombra si calcola su forme semplici: su quelle con rientranze (il "biscotto" degli artisti)
            // è pesantissima per il sistema grafico e bloccava l'app. Lì l'ombra è un cerchio sotto la foto.
            val simpleShape = artworkShape is androidx.compose.foundation.shape.CornerBasedShape
            Box(Modifier.size(250.dp), contentAlignment = Alignment.Center) {
                if (!simpleShape) {
                    Box(Modifier.size(214.dp).shadow(28.dp, androidx.compose.foundation.shape.CircleShape))
                }
                Artwork(
                    artworkUrl,
                    Modifier.size(250.dp).then(if (simpleShape) Modifier.shadow(28.dp, artworkShape) else Modifier),
                    shape = artworkShape,
                    sizePx = 720,
                )
            }
        }
        val ios = com.justlorenzo.archivetunex.ui.ios.iosStyle
        Text(
            title,
            style = if (ios) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.headlineMedium,
            fontWeight = if (ios) FontWeight.Bold else FontWeight.Black,
            letterSpacing = if (ios) MaterialTheme.typography.headlineSmall.letterSpacing else (-0.5).sp,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 12.dp),
        )
        if (!subtitle.isNullOrBlank()) {
            // Apple Music: l'artista sotto il titolo è nel colore d'accento
            Text(
                subtitle,
                style = if (ios) MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Normal) else MaterialTheme.typography.titleMedium,
                fontWeight = if (ios) FontWeight.Normal else FontWeight.SemiBold,
                color = if (ios) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (!detail.isNullOrBlank()) {
            Text(
                detail,
                style = if (ios) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        Box(Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 8.dp)) { buttons() }
    }
}
