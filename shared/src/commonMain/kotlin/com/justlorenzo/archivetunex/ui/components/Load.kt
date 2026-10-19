package com.justlorenzo.archivetunex.ui.components

import com.justlorenzo.archivetunex.ui.tr
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import moe.rukamori.archivetune.compat.Timber

/** Stato di un contenuto scaricato da YouTube Music. */
sealed interface LoadState<out T> {
    data object Loading : LoadState<Nothing>

    data class Ready<T>(val value: T) : LoadState<T>

    data class Failed(val message: String) : LoadState<Nothing>
}

/** Un contenuto caricato in background, con [reload] per riprovare o aggiornare. */
class Loadable<T>(
    private val state: MutableState<LoadState<T>>,
    private val reloadCounter: MutableState<Int>,
) {
    val value: LoadState<T>
        get() = state.value

    fun reload() {
        reloadCounter.value += 1
    }
}

/**
 * Contenuti già scaricati, tenuti in memoria finché l'app è aperta: tornando indietro
 * (es. dalla pagina di un album alla Home) la pagina compare subito com'era, senza ricaricare.
 */
private object LoadCache {
    private const val MAX_ENTRIES = 40

    private class Entry(
        val value: Any?,
        val atMs: Long,
    )

    private val entries = LinkedHashMap<String, Entry>()

    fun get(id: String): Pair<Any?, Long>? = entries[id]?.let { it.value to it.atMs }

    fun put(
        id: String,
        value: Any?,
    ) {
        entries.remove(id)
        entries[id] = Entry(value, nowMs())
        // Tiene solo i più recenti
        while (entries.size > MAX_ENTRIES) entries.remove(entries.keys.first())
    }
}

private fun nowMs(): Long = kotlin.time.Clock.System.now().toEpochMilliseconds()

/**
 * Carica [block] quando cambia [key] (o con reload). Gli errori finiscono nei Debug Logs e
 * diventano un messaggio semplice per l'utente.
 *
 * Con [cache] (un nome per il tipo di pagina, es. "home") il risultato resta in memoria:
 * tornando sulla pagina compare subito, e si riscarica in silenzio solo se è più vecchio di [maxAgeMs].
 */
@Composable
fun <T> rememberLoadable(
    key: Any?,
    cache: String? = null,
    maxAgeMs: Long = 15 * 60 * 1000L,
    block: suspend () -> Result<T>,
): Loadable<T> {
    val cacheId = cache?.let { "$it|$key" }
    val cached = remember(key) { cacheId?.let { LoadCache.get(it) } }
    @Suppress("UNCHECKED_CAST")
    val state =
        remember(key) {
            mutableStateOf<LoadState<T>>(if (cached != null) LoadState.Ready(cached.first as T) else LoadState.Loading)
        }
    val counter = remember(key) { mutableStateOf(0) }
    LaunchedEffect(key, counter.value) {
        val fresh = cached != null && nowMs() - cached.second < maxAgeMs
        // Già in memoria e recente: niente da fare
        if (counter.value == 0 && fresh) return@LaunchedEffect
        // Un "aggiorna" voluto mostra il caricamento; un aggiornamento in silenzio tiene quello che c'è
        if (counter.value > 0 || state.value !is LoadState.Ready) state.value = LoadState.Loading
        val result =
            try {
                withContext(Dispatchers.Default) { block() }.fold(
                    onSuccess = { LoadState.Ready(it) },
                    onFailure = { error ->
                        if (error is CancellationException) throw error
                        Timber.tag("ArchiveTuneX").e(error, "Caricamento non riuscito")
                        LoadState.Failed(tr("Impossibile caricare i contenuti. Controlla la connessione."))
                    },
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.tag("ArchiveTuneX").e(e, "Caricamento non riuscito")
                LoadState.Failed(tr("Impossibile caricare i contenuti. Controlla la connessione."))
            }
        if (result is LoadState.Ready) {
            cacheId?.let { LoadCache.put(it, result.value) }
            state.value = result
        } else if (state.value !is LoadState.Ready || counter.value > 0) {
            // Se l'aggiornamento in silenzio fallisce, resta la versione in memoria
            state.value = result
        }
    }
    return remember(key) { Loadable(state, counter) }
}
