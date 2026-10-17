package moe.rukamori.archivetune.compat

import platform.UIKit.UIApplication

/**
 * Su iPhone lo stile della barra di stato lo decide il controller di Compose (tema chiaro/scuro);
 * qui si gestisce lo schermo sempre acceso. Barre nascoste e blocco screenshot non sono disponibili.
 */
actual object SystemBars {
    actual fun setDarkContent(dark: Boolean) = Unit

    actual fun setStatusBarsHidden(hidden: Boolean) = Unit

    actual fun setImmersive(enabled: Boolean) = Unit

    actual fun setKeepScreenOn(enabled: Boolean) {
        UIApplication.sharedApplication.idleTimerDisabled = enabled
    }

    actual fun setSecure(enabled: Boolean) = Unit
}
