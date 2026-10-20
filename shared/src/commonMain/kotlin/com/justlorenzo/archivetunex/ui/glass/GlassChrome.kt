package com.justlorenzo.archivetunex.ui.glass

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect

/**
 * Barra in basso di vetro (tema iOS): mini player, schede e ricerca disegnati con i controlli
 * nativi di iOS 26 (Liquid Glass). Su Android non esiste e l'app usa la barra di sempre.
 */
expect val glassChromeAvailable: Boolean

/** Quello che la barra di vetro mostra. Le schede seguono l'ordine di [com.justlorenzo.archivetunex.ui.Tab]. */
@Immutable
data class GlassChromeState(
    val selectedTab: Int,
    val hasMini: Boolean,
    val title: String,
    val artist: String,
    val artworkUrl: String?,
    val isPlaying: Boolean,
    val dark: Boolean,
    val visible: Boolean,
    /** Colore d'accento (ARGB). */
    val accent: Long,
    /** Spazio sotto la barra, in dp (= punti su iPhone). */
    val bottomPadding: Float,
    val homeLabel: String,
    val statsLabel: String,
    val libraryLabel: String,
    val searchLabel: String,
)

/** Cosa succede toccando la barra di vetro. */
class GlassChromeActions(
    val onTab: (Int) -> Unit,
    val onMini: () -> Unit,
    val onPlayPause: () -> Unit,
    val onNext: () -> Unit,
)

/** Altezza della barra senza lo spazio sotto: mini player (50) + spazio (10) + schede (62). */
const val GlassChromeHeight = 122f

/** Altezza della sola riga delle schede (senza mini player). */
const val GlassTabsHeight = 62f

/**
 * La barra di vetro nativa, sopra il contenuto dell'app. [onMiniBounds] riceve la posizione del
 * mini player (in pixel, rispetto alla finestra): il player si apre da lì e lì si richiude.
 */
@Composable
expect fun GlassBottomChrome(
    state: GlassChromeState,
    actions: GlassChromeActions,
    onMiniBounds: (Rect) -> Unit,
    modifier: Modifier = Modifier,
)
