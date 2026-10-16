package moe.rukamori.archivetune.compat

import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi

/** Come androidx.activity.compose.BackHandler: tasto/gesto "indietro" (su iPhone lo swipe dal bordo). */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun BackHandler(
    enabled: Boolean = true,
    onBack: () -> Unit,
) {
    androidx.compose.ui.backhandler.BackHandler(enabled = enabled, onBack = onBack)
}
