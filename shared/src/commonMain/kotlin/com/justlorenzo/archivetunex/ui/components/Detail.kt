@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.justlorenzo.archivetunex.ui.components

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
 * Barra delle azioni (stile Expressive): al centro il grande Play a biscotto, ai lati Casuale e Scarica,
 * alle estremità Salva/Segui e "Altro". Ogni pulsante ha il suo nome sotto.
 */
@Composable
fun PlayShuffleButtons(
    onPlay: (() -> Unit)?,
    onShuffle: (() -> Unit)?,
    modifier: Modifier = Modifier,
    onDownload: (() -> Unit)? = null,
    toggle: DetailToggle? = null,
    menu: List<DetailMenuItem> = emptyList(),
    playLabel: String = tr("Riproduci"),
    shuffleLabel: String = tr("Casuale"),
    shuffleIcon: DrawableResource = Res.drawable.shuffle,
) {
    Row(
        modifier.fillMaxWidth().padding(top = 4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Top,
    ) {
        if (toggle != null) {
            ActionButton(
                icon = if (toggle.active) toggle.iconOn else toggle.iconOff,
                label = if (toggle.active) toggle.labelOn else toggle.labelOff,
                size = 48.dp,
                container = if (toggle.active) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceContainerHigh,
                content = if (toggle.active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                onClick = toggle.onClick,
            )
        }
        if (onShuffle != null) {
            ActionButton(shuffleIcon, shuffleLabel, 58.dp, MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer, onClick = onShuffle)
        }
        if (onPlay != null) {
            ActionButton(
                Res.drawable.play,
                playLabel,
                76.dp,
                MaterialTheme.colorScheme.primary,
                MaterialTheme.colorScheme.onPrimary,
                shape = MaterialShapes.Cookie9Sided.toShape(),
                iconSize = 34.dp,
                onClick = onPlay,
            )
        }
        if (onDownload != null) {
            ActionButton(Res.drawable.download, tr("Scarica"), 58.dp, MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer, onClick = onDownload)
        }
        if (menu.isNotEmpty()) {
            var open by remember { mutableStateOf(false) }
            Box {
                ActionButton(Res.drawable.more_vert, tr("Altro"), 48.dp, MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.colorScheme.onSurface) { open = true }
                DropdownMenu(expanded = open, onDismissRequest = { open = false }, shape = RoundedCornerShape(20.dp)) {
                    menu.forEach { item ->
                        val color = if (item.danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                        DropdownMenuItem(
                            text = { Text(item.label, color = color, fontWeight = FontWeight.SemiBold) },
                            leadingIcon = { AtxIcon(item.icon, null, size = 20.dp, tint = color) },
                            onClick = {
                                open = false
                                item.onClick()
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionButton(
    icon: DrawableResource,
    label: String,
    size: Dp,
    container: Color,
    content: Color,
    shape: Shape = CircleShape,
    iconSize: Dp = 24.dp,
    onClick: () -> Unit,
) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.88f else 1f, spring(dampingRatio = 0.45f, stiffness = 700f), label = "actionScale")
    Column(
        Modifier.width(76.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        // I pulsanti piccoli restano centrati sull'altezza del Play
        Box(Modifier.height(76.dp), contentAlignment = Alignment.Center) {
            Box(
                Modifier
                    .size(size)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }.clip(shape)
                    .background(container)
                    .clickable(interactionSource = source, indication = LocalIndication.current, onClick = onClick),
                contentAlignment = Alignment.Center,
            ) { AtxIcon(icon, label, size = iconSize, tint = content) }
        }
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

/** Freccia indietro in alto a sinistra delle pagine aperte. */
@Composable
fun BackButton(modifier: Modifier = Modifier) {
    val navigator = LocalNavigator.current
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
    buttons: @Composable () -> Unit,
) {
    Column(
        modifier.fillMaxWidth().padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Artwork(
            artworkUrl,
            Modifier.size(250.dp).shadow(28.dp, artworkShape),
            shape = artworkShape,
            sizePx = 720,
        )
        Text(
            title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black,
            letterSpacing = (-0.5).sp,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 12.dp),
        )
        if (!subtitle.isNullOrBlank()) {
            Text(
                subtitle,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (!detail.isNullOrBlank()) {
            Text(
                detail,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        Box(Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 8.dp)) { buttons() }
    }
}
