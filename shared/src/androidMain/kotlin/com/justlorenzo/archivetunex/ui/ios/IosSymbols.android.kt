package com.justlorenzo.archivetunex.ui.ios

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap

/** Su Android non ci sono le SF Symbols: restano le icone dell'app. */
@Composable
actual fun rememberSystemSymbol(
    name: String,
    pointSize: Float,
    bold: Boolean,
): ImageBitmap? = null
