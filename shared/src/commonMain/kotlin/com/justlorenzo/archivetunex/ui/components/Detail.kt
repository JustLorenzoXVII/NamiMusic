package com.justlorenzo.archivetunex.ui.components

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
import androidx.compose.ui.unit.dp
import archivetunex.shared.generated.resources.Res
import archivetunex.shared.generated.resources.arrow_back
import archivetunex.shared.generated.resources.download
import archivetunex.shared.generated.resources.play
import archivetunex.shared.generated.resources.shuffle
import com.justlorenzo.archivetunex.ui.LocalNavigator

/** "Riproduci" pieno e "Casuale" tonale, uno accanto all'altro (stile Expressive). */
@Composable
fun PlayShuffleButtons(
    onPlay: () -> Unit,
    onShuffle: () -> Unit,
    modifier: Modifier = Modifier,
    onDownload: (() -> Unit)? = null,
) {
    Row(
        modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Button(
            onClick = onPlay,
            modifier = Modifier.weight(1f),
            contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
        ) {
            AtxIcon(Res.drawable.play, null, size = 20.dp)
            Text("Riproduci", modifier = Modifier.padding(start = 8.dp))
        }
        FilledTonalButton(
            onClick = onShuffle,
            modifier = Modifier.weight(1f),
            contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
        ) {
            AtxIcon(Res.drawable.shuffle, null, size = 20.dp)
            Text("Casuale", modifier = Modifier.padding(start = 8.dp))
        }
        if (onDownload != null) {
            FilledTonalIconButton(onClick = onDownload) {
                AtxIcon(Res.drawable.download, "Scarica tutto")
            }
        }
    }
}

/** Freccia indietro in alto a sinistra delle pagine aperte. */
@Composable
fun BackButton(modifier: Modifier = Modifier) {
    val navigator = LocalNavigator.current
    Box(modifier.statusBarsPadding().padding(start = 4.dp, top = 4.dp)) {
        IconButton(onClick = { navigator.back() }) {
            AtxIcon(Res.drawable.arrow_back, "Indietro")
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
    artworkShape: Shape = MaterialTheme.shapes.extraLarge,
    buttons: @Composable () -> Unit,
) {
    Column(
        modifier.fillMaxWidth().padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Artwork(
            artworkUrl,
            Modifier.size(240.dp),
            shape = if (artworkShape == CircleShape) CircleShape else artworkShape,
            sizePx = 720,
        )
        Text(
            title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 12.dp),
        )
        if (!subtitle.isNullOrBlank()) {
            Text(
                subtitle,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
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
        Box(Modifier.padding(top = 10.dp, bottom = 8.dp)) { buttons() }
    }
}
