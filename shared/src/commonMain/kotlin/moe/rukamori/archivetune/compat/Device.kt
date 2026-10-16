package moe.rukamori.archivetune.compat

/** Batteria del dispositivo (al posto di Intent.ACTION_BATTERY_CHANGED). */
expect object BatteryState {
    /** Percentuale 0..100, oppure -1 se sconosciuta. */
    fun level(): Int

    fun isCharging(): Boolean
}

/** Sensore di prossimità: [onCovered] riceve true quando il telefono è coperto. Restituisce la funzione per smettere. */
expect fun observeProximity(onCovered: (Boolean) -> Unit): () -> Unit

/** Scuotimento del telefono (accelerometro). Restituisce la funzione per smettere. */
expect fun observeShake(onShake: () -> Unit): () -> Unit
