package com.justlorenzo.archivetunex.insights

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import moe.rukamori.archivetune.compat.time.ZoneOffset
import moe.rukamori.archivetune.db.MusicDatabase
import moe.rukamori.archivetune.db.entities.EventWithSong
import moe.rukamori.archivetune.models.MediaMetadata
import moe.rukamori.archivetune.models.toMediaMetadata

/** Periodo delle statistiche. */
enum class StatsPeriod(
    val rawLabel: String,
    val days: Int?,
) {
    Week("7 giorni", 7),
    Month("30 giorni", 30),
    Year("Anno", 365),
    All("Sempre", null),
}

data class SongStat(
    val song: MediaMetadata,
    val timeMs: Long,
    val plays: Int,
)

data class ArtistStat(
    val id: String?,
    val name: String,
    val thumbnailUrl: String?,
    val timeMs: Long,
    val plays: Int,
)

data class SkipStat(
    val id: String,
    val title: String,
    val artists: String,
    val thumbnailUrl: String?,
    val skips: Int,
    val plays: Int,
)

data class GenreShare(
    val genre: Genre,
    val share: Float,
)

data class ListeningStats(
    val totalMs: Long = 0,
    val plays: Int = 0,
    val distinctSongs: Int = 0,
    val distinctArtists: Int = 0,
    val topSongs: List<SongStat> = emptyList(),
    val topArtists: List<ArtistStat> = emptyList(),
    val byHour: List<Long> = List(24) { 0L },
    val skippedSongs: List<SkipStat> = emptyList(),
    val skippedArtists: List<Pair<String, Int>> = emptyList(),
    val genres: List<GenreShare> = emptyList(),
    val neverGenres: List<Genre> = emptyList(),
    /** Quanto ascolto è coperto da artisti di cui si conoscono i generi (0…1). */
    val genreCoverage: Float = 0f,
    /** Artisti di cui servono ancora i generi. */
    val artistsForGenres: List<String> = emptyList(),
    /** Tempo d'ascolto per artista e inizio del periodo: servono per salti e generi. */
    val artistTimes: Map<String, Long> = emptyMap(),
    val fromMs: Long? = null,
)

internal fun EventWithSong.epochMs(): Long = event.timestamp.toInstant(ZoneOffset.UTC).toEpochMilli()

/** Analisi degli ascolti: tutto calcolato sul telefono dalla cronologia e dai salti registrati. */
object Insights {
    private const val PAGE = 500
    private const val MAX_EVENTS = 20_000

    /** Gli ascolti dal più recente, fino a [fromMs] (escluso). */
    suspend fun events(
        database: MusicDatabase,
        fromMs: Long?,
    ): List<EventWithSong> =
        withContext(Dispatchers.IO) {
            val result = mutableListOf<EventWithSong>()
            var offset = 0
            while (result.size < MAX_EVENTS) {
                val page = database.events(PAGE, offset)
                if (page.isEmpty()) break
                for (e in page) {
                    if (fromMs != null && e.epochMs() < fromMs) return@withContext result
                    result += e
                }
                offset += PAGE
            }
            result
        }

    /**
     * La parte pesante (tutta la cronologia del periodo), in background: si rifà solo cambiando periodo.
     * Salti e generi si aggiungono dopo con [withLiveData], che è leggero.
     */
    suspend fun compute(
        database: MusicDatabase,
        period: StatsPeriod,
    ): ListeningStats = withContext(Dispatchers.Default) { computeBase(database, period) }

    /** Aggiunge salti e generi (cambiano spesso: a ogni brano e a ogni genere trovato). */
    suspend fun withLiveData(base: ListeningStats): ListeningStats = withContext(Dispatchers.Default) { live(base) }

    private suspend fun computeBase(
        database: MusicDatabase,
        period: StatsPeriod,
    ): ListeningStats {
        val fromMs = period.days?.let { nowMs() - it * 86_400_000L }
        val events = events(database, fromMs).filter { !it.song.song.isPodcast }

        val songTime = mutableMapOf<String, Long>()
        val songPlays = mutableMapOf<String, Int>()
        val songById = mutableMapOf<String, EventWithSong>()
        val artistTime = mutableMapOf<String, Long>()
        val artistPlays = mutableMapOf<String, Int>()
        val artistInfo = mutableMapOf<String, Pair<String?, String?>>()
        val byHour = LongArray(24)
        var total = 0L

        for (e in events) {
            val id = e.song.id
            val time = e.event.playTime.coerceAtLeast(0)
            total += time
            songTime[id] = (songTime[id] ?: 0) + time
            songPlays[id] = (songPlays[id] ?: 0) + 1
            songById.getOrPut(id) { e }
            byHour[e.event.timestamp.hour.coerceIn(0, 23)] += time
            e.song.artists.forEach { artist ->
                val name = artist.name
                artistTime[name] = (artistTime[name] ?: 0) + time
                artistPlays[name] = (artistPlays[name] ?: 0) + 1
                if (artistInfo[name]?.second == null) artistInfo[name] = artist.id to artist.thumbnailUrl
            }
        }

        val topSongs =
            songTime.entries
                .sortedByDescending { it.value }
                .take(10)
                .map { (id, time) -> SongStat(songById.getValue(id).song.toMediaMetadata(), time, songPlays[id] ?: 0) }
        val topArtists =
            artistTime.entries
                .sortedByDescending { it.value }
                .take(10)
                .map { (name, time) ->
                    val (id, thumb) = artistInfo[name] ?: (null to null)
                    ArtistStat(id, name, thumb, time, artistPlays[name] ?: 0)
                }

        return ListeningStats(
            totalMs = total,
            plays = events.size,
            distinctSongs = songTime.size,
            distinctArtists = artistTime.size,
            topSongs = topSongs,
            topArtists = topArtists,
            byHour = byHour.toList(),
            artistTimes = artistTime.toMap(),
            fromMs = fromMs,
        )
    }

    private fun live(base: ListeningStats): ListeningStats {
        val fromMs = base.fromMs
        val artistTime = base.artistTimes
        // Salti del periodo
        val signals = ListeningSignals.signals.value
        val skippedSongs =
            signals.entries
                .filter { (_, s) -> s.skips > 0 && (fromMs == null || s.lastSkipped >= fromMs) }
                .sortedWith(compareByDescending<Map.Entry<String, SongSignal>> { it.value.skips }.thenBy { it.value.plays })
                .take(10)
                .map { (id, s) -> SkipStat(id, s.title, s.artists.joinToString(), s.thumbnailUrl, s.skips, s.plays) }
        val artistSkips = mutableMapOf<String, Int>()
        signals.values
            .filter { s -> s.skips > 0 && (fromMs == null || s.lastSkipped >= fromMs) }
            .forEach { s -> s.artists.forEach { artistSkips[it] = (artistSkips[it] ?: 0) + s.skips } }
        val skippedArtists = artistSkips.entries.filter { it.value >= 2 }.sortedByDescending { it.value }.take(6).map { it.key to it.value }

        // Generi: il tempo di ogni artista diviso tra i suoi generi
        val genreTime = mutableMapOf<Genre, Double>()
        var covered = 0L
        val missing = mutableListOf<String>()
        artistTime.entries.sortedByDescending { it.value }.forEach { (name, time) ->
            val tags = GenreService.cached(name)
            if (tags == null) {
                missing += name
                return@forEach
            }
            val genres = tags.flatMap { Genre.fromTag(it) }.distinct()
            if (genres.isEmpty()) return@forEach
            covered += time
            // I primi generi pesano di più
            val weights = genres.mapIndexed { i, g -> g to 1.0 / (i + 1) }
            val sum = weights.sumOf { it.second }
            weights.forEach { (g, w) -> genreTime[g] = (genreTime[g] ?: 0.0) + time * w / sum }
        }
        val genreTotal = genreTime.values.sum()
        val genres =
            if (genreTotal <= 0) {
                emptyList()
            } else {
                genreTime.entries.sortedByDescending { it.value }.map { GenreShare(it.key, (it.value / genreTotal).toFloat()) }.filter { it.share >= 0.02f }
            }
        val artistTotal = artistTime.values.sum().coerceAtLeast(1)
        val never = if (genres.isEmpty()) emptyList() else Genre.entries.filter { g -> genres.none { it.genre == g } }

        return base.copy(
            skippedSongs = skippedSongs,
            skippedArtists = skippedArtists,
            genres = genres,
            neverGenres = never,
            genreCoverage = (covered.toDouble() / artistTotal).toFloat(),
            artistsForGenres = missing.take(25),
        )
    }
}
