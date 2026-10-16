package moe.rukamori.archivetune.compat

/** Stato della rete (al posto di ConnectivityManager). */
expect object NetworkState {
    fun isConnected(): Boolean

    /** Rete a consumo (dati cellulare o hotspot). */
    fun isMetered(): Boolean

    fun isCellular(): Boolean

    /** Avvisa a ogni cambio di rete; restituisce la funzione per smettere. */
    fun observe(onChange: () -> Unit): () -> Unit
}

/** Memoria scarsa (ActivityManager.isLowRamDevice). */
expect fun platformIsLowRamDevice(): Boolean
