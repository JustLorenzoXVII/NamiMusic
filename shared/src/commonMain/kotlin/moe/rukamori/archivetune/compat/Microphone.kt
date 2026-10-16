package moe.rukamori.archivetune.compat

/** Registra dal microfono: PCM 16 bit mono a [sampleRate] Hz per [durationMs] millisecondi. */
expect suspend fun platformRecordMicrophone(
    sampleRate: Int,
    durationMs: Long,
): ShortArray
