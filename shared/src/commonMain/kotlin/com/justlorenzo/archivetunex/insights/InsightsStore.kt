package com.justlorenzo.archivetunex.insights

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import moe.rukamori.archivetune.compat.File
import moe.rukamori.archivetune.compat.platformFilesDirectory
import moe.rukamori.archivetune.models.MediaMetadata
import kotlin.time.Clock

internal val insightsJson =
    Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

internal fun nowMs(): Long = Clock.System.now().toEpochMilliseconds()

/** File JSON nella cartella dell'app (insights/…): piccolo, letto all'avvio e salvato con calma. */
internal class JsonFile<T>(
    name: String,
    private val serializer: KSerializer<T>,
    private val empty: () -> T,
) {
    private val file = File(platformFilesDirectory()).resolve("insights").resolve(name)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var saveJob: Job? = null

    private val _value = MutableStateFlow(read())
    val value: StateFlow<T> = _value.asStateFlow()

    private fun read(): T =
        runCatching { if (file.exists()) insightsJson.decodeFromString(serializer, file.readText()) else null }.getOrNull() ?: empty()

    fun update(transform: (T) -> T) {
        _value.update(transform)
        // Salvataggio raggruppato: tanti cambi ravvicinati = una sola scrittura
        saveJob?.cancel()
        saveJob =
            scope.launch {
                delay(1_500)
                runCatching {
                    file.parentFile?.mkdirs()
                    file.writeText(insightsJson.encodeToString(serializer, _value.value))
                }
            }
    }
}

/** Cosa succede a un brano quando lo ascolti: quante volte arriva in fondo, quante lo salti. */
@Serializable
data class SongSignal(
    val title: String,
    val artists: List<String> = emptyList(),
    val artistIds: List<String?> = emptyList(),
    val thumbnailUrl: String? = null,
    val durationSec: Int = 0,
    val plays: Int = 0,
    val skips: Int = 0,
    val lastPlayed: Long = 0,
    val lastSkipped: Long = 0,
)

/**
 * Registra gli ascolti completi e i brani saltati (la cronologia di ArchiveTune salva solo
 * gli ascolti oltre una certa durata, quindi i salti non li vedeva).
 */
object ListeningSignals {
    private const val MAX_SONGS = 4_000

    private val store =
        JsonFile("signals.json", MapSerializer(String.serializer(), SongSignal.serializer())) { emptyMap() }

    val signals: StateFlow<Map<String, SongSignal>> = store.value

    /**
     * Fine dell'ascolto di un brano. [finished] = è arrivato in fondo da solo.
     * Un salto è quando lo cambi prima di 30 secondi o prima del 40% del brano.
     */
    fun record(
        song: MediaMetadata,
        playedMs: Long,
        finished: Boolean,
        userSkipped: Boolean,
    ) {
        if (song.isPodcast) return
        val durationMs = song.duration.toLong() * 1000L
        val skipped =
            userSkipped && !finished && playedMs < 30_000 && (durationMs <= 0 || playedMs < durationMs * 0.4) && playedMs >= 1_000
        val counted = finished || playedMs >= 30_000 || (durationMs > 0 && playedMs >= durationMs * 0.4)
        if (!skipped && !counted) return
        val now = nowMs()
        store.update { current ->
            val old = current[song.id]
            val base =
                SongSignal(
                    title = song.title,
                    artists = song.artists.map { it.name },
                    artistIds = song.artists.map { it.id },
                    thumbnailUrl = song.thumbnailUrl ?: old?.thumbnailUrl,
                    durationSec = song.duration.takeIf { it > 0 } ?: old?.durationSec ?: 0,
                    plays = old?.plays ?: 0,
                    skips = old?.skips ?: 0,
                    lastPlayed = old?.lastPlayed ?: 0,
                    lastSkipped = old?.lastSkipped ?: 0,
                )
            val next =
                if (skipped) base.copy(skips = base.skips + 1, lastSkipped = now) else base.copy(plays = base.plays + 1, lastPlayed = now)
            val updated = current + (song.id to next)
            // Tiene i più recenti se la lista diventa troppo lunga
            if (updated.size > MAX_SONGS) {
                updated.entries.sortedByDescending { maxOf(it.value.lastPlayed, it.value.lastSkipped) }.take(MAX_SONGS).associate { it.toPair() }
            } else {
                updated
            }
        }
    }

    /** Brani che salti quasi sempre: non finiscono nei mix. */
    fun dislikedSongIds(): Set<String> =
        signals.value.filter { (_, s) -> s.skips >= 2 && s.skips > s.plays * 2 }.keys

    /** Artisti che salti spesso (per nome). */
    fun dislikedArtists(): Set<String> {
        val byArtist = mutableMapOf<String, Pair<Int, Int>>()
        signals.value.values.forEach { s ->
            s.artists.forEach { name ->
                val (p, k) = byArtist[name] ?: (0 to 0)
                byArtist[name] = (p + s.plays) to (k + s.skips)
            }
        }
        return byArtist.filter { (_, v) -> v.second >= 4 && v.second > v.first * 2 }.keys
    }
}
