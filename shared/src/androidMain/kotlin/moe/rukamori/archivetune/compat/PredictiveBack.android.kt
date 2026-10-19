package moe.rukamori.archivetune.compat

import androidx.activity.BackEventCompat
import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Composable
actual fun PredictiveBackHandler(
    enabled: Boolean,
    onBack: suspend (Flow<BackGesture>) -> Unit,
) {
    androidx.activity.compose.PredictiveBackHandler(enabled = enabled) { events ->
        onBack(events.map { BackGesture(it.progress, it.swipeEdge == BackEventCompat.EDGE_LEFT) })
    }
}
