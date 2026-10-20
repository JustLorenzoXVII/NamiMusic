package com.justlorenzo.archivetunex.ui.glass

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.UIKitInteropInteractionMode
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.viewinterop.UIKitViewController
import platform.UIKit.UIViewController

actual val glassChromeAvailable: Boolean = true

/**
 * Ponte tra l'app (Kotlin) e la barra di vetro (SwiftUI, in iosApp/GlassChrome.swift).
 * Swift all'avvio registra [factory] e [listener]; l'app gli passa lo stato con [publish]
 * e Swift chiama le azioni ([selectTab], [openPlayer]…) quando si tocca la barra.
 */
object GlassChromeBridge {
    /** Crea la vista SwiftUI della barra (la registra Swift all'avvio). */
    var factory: (() -> UIViewController)? = null

    /** Swift riceve qui ogni nuovo stato. */
    var listener: ((GlassChromeState) -> Unit)? = null
        set(value) {
            field = value
            last?.let { value?.invoke(it) }
        }

    private var last: GlassChromeState? = null
    internal var actions: GlassChromeActions? = null
    internal var onMiniFrame: ((Double, Double, Double, Double) -> Unit)? = null

    /** L'ultimo stato pubblicato (Swift lo legge appena crea la vista). */
    fun current(): GlassChromeState? = last

    internal fun publish(state: GlassChromeState) {
        if (state == last) return
        last = state
        listener?.invoke(state)
    }

    fun selectTab(index: Int) {
        actions?.onTab?.invoke(index)
    }

    fun openPlayer() {
        actions?.onMini?.invoke()
    }

    fun playPause() {
        actions?.onPlayPause?.invoke()
    }

    fun next() {
        actions?.onNext?.invoke()
    }

    /** Posizione del mini player in punti, rispetto alla finestra. */
    fun reportMiniFrame(
        x: Double,
        y: Double,
        width: Double,
        height: Double,
    ) {
        onMiniFrame?.invoke(x, y, width, height)
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
actual fun GlassBottomChrome(
    state: GlassChromeState,
    actions: GlassChromeActions,
    onMiniBounds: (Rect) -> Unit,
    modifier: Modifier,
) {
    val factory = GlassChromeBridge.factory ?: return
    val density = LocalDensity.current.density
    val currentOnMiniBounds = rememberUpdatedState(onMiniBounds)
    SideEffect {
        GlassChromeBridge.actions = actions
        GlassChromeBridge.publish(state)
    }
    DisposableEffect(density) {
        GlassChromeBridge.onMiniFrame = { x, y, w, h ->
            currentOnMiniBounds.value(
                Rect(
                    left = (x * density).toFloat(),
                    top = (y * density).toFloat(),
                    right = ((x + w) * density).toFloat(),
                    bottom = ((y + h) * density).toFloat(),
                ),
            )
        }
        onDispose { GlassChromeBridge.onMiniFrame = null }
    }
    // Sopra il contenuto (così il vetro vede e rifrange quello che scorre sotto) e con i tocchi
    // gestiti subito da iOS. Quando si apre il player la barra sparisce da sola (lo fa Swift).
    UIKitViewController(
        factory = factory,
        // Solo l'altezza che serve: senza brano in riproduzione c'è solo la riga delle schede
        modifier = modifier.fillMaxWidth().height(((if (state.hasMini) GlassChromeHeight else GlassTabsHeight) + state.bottomPadding).dp),
        properties =
            UIKitInteropProperties(
                interactionMode = UIKitInteropInteractionMode.NonCooperative,
                isNativeAccessibilityEnabled = true,
                placedAsOverlay = true,
            ),
    )
}
