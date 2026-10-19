package moe.rukamori.archivetune.compat

import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.Flow

/** Un istante del gesto "indietro": quanto è avanzato (0…1) e da quale bordo parte. */
data class BackGesture(
    val progress: Float,
    val fromLeft: Boolean,
)

/**
 * Gesto "indietro" che segue il dito (predictive back). [onBack] riceve il flusso del gesto:
 * se finisce normalmente il gesto è confermato, se viene annullato arriva una CancellationException.
 * Con il tasto indietro classico il flusso finisce subito, senza valori.
 */
@Composable
expect fun PredictiveBackHandler(
    enabled: Boolean = true,
    onBack: suspend (Flow<BackGesture>) -> Unit,
)
