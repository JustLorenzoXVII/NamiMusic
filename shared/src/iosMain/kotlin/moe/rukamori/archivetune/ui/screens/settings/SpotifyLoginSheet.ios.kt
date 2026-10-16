package moe.rukamori.archivetune.ui.screens.settings

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

@Composable
internal actual fun SpotifyLoginSheet(
    onDismiss: () -> Unit,
    onCookiesCaptured: (spDc: String, spKey: String) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("OK") } },
        title = { Text("Spotify") },
        text = { Text("Il collegamento a Spotify non è ancora disponibile su iPhone.") },
    )
}
