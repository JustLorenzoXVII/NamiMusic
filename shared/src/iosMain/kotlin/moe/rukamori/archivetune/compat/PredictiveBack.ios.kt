package moe.rukamori.archivetune.compat

import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Su iPhone il gesto è lo swipe dal bordo sinistro: Compose ne riporta l'avanzamento. */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
actual fun PredictiveBackHandler(
    enabled: Boolean,
    onBack: suspend (Flow<BackGesture>) -> Unit,
) {
    androidx.compose.ui.backhandler.PredictiveBackHandler(enabled = enabled) { events ->
        onBack(events.map { BackGesture(it.progress, it.swipeEdge == androidx.compose.ui.backhandler.BackEventCompat.EDGE_LEFT) })
    }
}
