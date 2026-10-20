package com.justlorenzo.archivetunex.ui.glass

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect

/** Il Liquid Glass è solo di iOS: su Android resta la barra di sempre. */
actual val glassChromeAvailable: Boolean = false

@Composable
actual fun GlassBottomChrome(
    state: GlassChromeState,
    actions: GlassChromeActions,
    onMiniBounds: (Rect) -> Unit,
    modifier: Modifier,
) {
}
