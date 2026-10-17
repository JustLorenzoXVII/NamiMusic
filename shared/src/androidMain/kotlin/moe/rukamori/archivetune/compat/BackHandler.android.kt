package moe.rukamori.archivetune.compat

import androidx.compose.runtime.Composable

@Composable
actual fun BackHandler(
    enabled: Boolean,
    onBack: () -> Unit,
) {
    androidx.activity.compose.BackHandler(enabled = enabled, onBack = onBack)
}
