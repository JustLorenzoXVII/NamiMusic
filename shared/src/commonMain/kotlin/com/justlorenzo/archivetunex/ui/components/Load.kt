package com.justlorenzo.archivetunex.ui.components

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
 * Carica [block] quando cambia [key] (o con reload). Gli errori finiscono nei Debug Logs e
 * diventano un messaggio semplice per l'utente.
 */
@Composable
fun <T> rememberLoadable(
    key: Any?,
    block: suspend () -> Result<T>,
): Loadable<T> {
    val state = remember(key) { mutableStateOf<LoadState<T>>(LoadState.Loading) }
    val counter = remember(key) { mutableStateOf(0) }
    LaunchedEffect(key, counter.value) {
        if (counter.value > 0 || state.value !is LoadState.Ready) state.value = LoadState.Loading
        state.value =
            try {
                withContext(Dispatchers.Default) { block() }.fold(
                    onSuccess = { LoadState.Ready(it) },
                    onFailure = { error ->
                        if (error is CancellationException) throw error
                        Timber.tag("ArchiveTuneX").e(error, "Caricamento non riuscito")
                        LoadState.Failed("Impossibile caricare i contenuti. Controlla la connessione.")
                    },
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.tag("ArchiveTuneX").e(e, "Caricamento non riuscito")
                LoadState.Failed("Impossibile caricare i contenuti. Controlla la connessione.")
            }
    }
    return remember(key) { Loadable(state, counter) }
}
