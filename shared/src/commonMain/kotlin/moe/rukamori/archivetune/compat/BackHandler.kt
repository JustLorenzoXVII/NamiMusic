package moe.rukamori.archivetune.compat

import androidx.compose.runtime.Composable

/** Come androidx.activity.compose.BackHandler: tasto/gesto "indietro" (su iPhone lo swipe dal bordo). */
@Composable
expect fun BackHandler(
    enabled: Boolean = true,
    onBack: () -> Unit,
)
