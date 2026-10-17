package moe.rukamori.archivetune.compat

/**
 * Le barre di sistema e la finestra (nell'originale: WindowCompat / WindowInsetsController di MainActivity).
 * Android: la finestra dell'Activity. iPhone: stile della barra di stato e blocco dello schermo.
 */
expect object SystemBars {
    /** true = icone scure (sfondo chiaro). */
    fun setDarkContent(dark: Boolean)

    fun setStatusBarsHidden(hidden: Boolean)

    /** Barre nascoste e mostrate solo con uno swipe (schermo AOD). */
    fun setImmersive(enabled: Boolean)

    fun setKeepScreenOn(enabled: Boolean)

    /** Blocca gli screenshot (FLAG_SECURE su Android). */
    fun setSecure(enabled: Boolean)
}
