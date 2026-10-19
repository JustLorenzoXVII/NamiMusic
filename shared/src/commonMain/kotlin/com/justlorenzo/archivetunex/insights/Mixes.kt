package com.justlorenzo.archivetunex.insights

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import moe.rukamori.archivetune.db.MusicDatabase
import moe.rukamori.archivetune.innertube.YouTube
import moe.rukamori.archivetune.innertube.models.SongItem
import moe.rukamori.archivetune.innertube.models.WatchEndpoint
import moe.rukamori.archivetune.models.MediaMetadata
import moe.rukamori.archivetune.models.toMediaMetadata

@Serializable
data class MixArtist(
    val id: String? = null,
    val name: String,
)

@Serializable
data class MixSong(
    val id: String,
    val title: String,
    val artists: List<MixArtist> = emptyList(),
    val thumbnailUrl: String? = null,
    val durationSec: Int = -1,
    val albumId: String? = null,
    val albumTitle: String? = null,
    val explicit: Boolean = false,
) {
    fun toMediaMetadata(): MediaMetadata =
        MediaMetadata(
            id = id,
            title = title,
            artists = artists.map { MediaMetadata.Artist(it.id, it.name) },
            duration = durationSec,
            thumbnailUrl = thumbnailUrl,
            album = if (albumId != null && albumTitle != null) MediaMetadata.Album(albumId, albumTitle) else null,
            explicit = explicit,
        )
}

@Serializable
enum class MixKind { ForYou, Discover, Rediscover, Artist }

@Serializable
data class Mix(
    val id: String,
    val kind: MixKind,
    val title: String,
    val subtitle: String,
    val songs: List<MixSong>,
    val createdAt: Long,
)

private fun MediaMetadata.toMixSong() =
    MixSong(
        id = id,
        title = title,
        artists = artists.map { MixArtist(it.id, it.name) },
        thumbnailUrl = thumbnailUrl,
        durationSec = duration,
        albumId = album?.id,
        albumTitle = album?.title,
        explicit = explicit,
    )

/**
 * Le playlist "Fatte per te": nascono da cosa ascolti di più, si allargano con i brani simili
 * suggeriti da YouTube Music (come la radio) e lasciano fuori brani e artisti che salti.
 * Si rinnovano da sole ogni settimana.
 */
object MixMaker {
    private const val REFRESH_AFTER_MS = 7L * 24 * 60 * 60 * 1000

    private val store = JsonFile("mixes.json", ListSerializer(Mix.serializer())) { emptyList() }
    private val mutex = Mutex()

    val mixes: StateFlow<List<Mix>> = store.value

    private val _working = MutableStateFlow(false)
    val working: StateFlow<Boolean> = _working.asStateFlow()

    fun mix(id: String): Mix? = mixes.value.firstOrNull { it.id == id }

    /** Rigenera se i mix mancano o hanno più di una settimana (o se [force]). */
    suspend fun refreshIfNeeded(
        database: MusicDatabase,
        force: Boolean = false,
    ) {
        val current = mixes.value
        val stale = current.isEmpty() || current.minOf { it.createdAt } < nowMs() - REFRESH_AFTER_MS
        if (!force && !stale) return
        mutex.withLock {
            _working.value = true
            try {
                val made = runCatching { generate(database) }.getOrNull()
                if (!made.isNullOrEmpty()) store.update { made }
            } finally {
                _working.value = false
            }
        }
    }

    private suspend fun related(seedId: String): List<SongItem> =
        YouTube.next(WatchEndpoint(videoId = seedId)).getOrNull()?.items.orEmpty().filter { it.id != seedId }

    private suspend fun generate(database: MusicDatabase): List<Mix> =
        withContext(Dispatchers.IO) {
            val now = nowMs()
            val dislikedSongs = ListeningSignals.dislikedSongIds()
            val dislikedArtists = ListeningSignals.dislikedArtists()
            fun MediaMetadata.allowed() = id !in dislikedSongs && artists.none { it.name in dislikedArtists } && !isPodcast

            // Cosa ascolti: ultimi 60 giorni, o tutto se è poco
            var events = Insights.events(database, now - 60L * 86_400_000L)
            if (events.size < 30) events = Insights.events(database, null)
            if (events.isEmpty()) return@withContext emptyList()
            val knownIds = events.map { it.song.id }.toSet()

            val songTime = mutableMapOf<String, Long>()
            val songMeta = mutableMapOf<String, MediaMetadata>()
            val artistTime = mutableMapOf<String, Long>()
            val artistSongs = mutableMapOf<String, MutableSet<String>>()
            for (e in events) {
                if (e.song.song.isPodcast) continue
                // Gli ascolti recenti contano di più
                val ageDays = ((now - e.epochMs()) / 86_400_000L).coerceAtLeast(0)
                val weight = (e.event.playTime.coerceAtLeast(0) * (1.0 / (1 + ageDays / 14.0))).toLong()
                songTime[e.song.id] = (songTime[e.song.id] ?: 0) + weight
                songMeta.getOrPut(e.song.id) { e.song.toMediaMetadata() }
                e.song.artists.forEach { a ->
                    artistTime[a.name] = (artistTime[a.name] ?: 0) + weight
                    artistSongs.getOrPut(a.name) { mutableSetOf() } += e.song.id
                }
            }
            val favorites = songTime.entries.sortedByDescending { it.value }.mapNotNull { songMeta[it.key] }.filter { it.allowed() }
            val result = mutableListOf<Mix>()

            // Brani simili ai preferiti (un giro di "radio" per ognuno dei primi 8)
            val seeds = favorites.take(8)
            val relatedBySeed = seeds.map { seed -> related(seed.id).map { it.toMediaMetadata() }.filter { it.allowed() } }

            // 1) Il tuo mix: preferiti alternati a brani simili
            run {
                val familiar = favorites.take(14).shuffled()
                val fresh = relatedBySeed.flatten().distinctBy { it.id }.shuffled().take(20)
                val songs = interleave(familiar, fresh).distinctBy { it.id }.take(30)
                if (songs.size >= 8) {
                    result += Mix("for-you", MixKind.ForYou, "Il tuo mix", "I tuoi preferiti e brani simili", songs.map { it.toMixSong() }, now)
                }
            }

            // 2) Scoperte: solo brani che non hai mai ascoltato, presi a turno da ogni preferito
            run {
                val lists = relatedBySeed.map { list -> list.filter { it.id !in knownIds } }
                val songs = roundRobin(lists).distinctBy { it.id }.take(25)
                if (songs.size >= 8) {
                    result += Mix("discover", MixKind.Discover, "Scoperte", "Brani nuovi simili ai tuoi gusti", songs.map { it.toMixSong() }, now)
                }
            }

            // 3) Riscoperte: brani che ascoltavi tanto e non senti da un po'
            run {
                val forgotten = runCatching { database.forgottenFavorites().first() }.getOrNull().orEmpty()
                val songs = forgotten.map { it.toMediaMetadata() }.filter { it.allowed() }.take(25)
                if (songs.size >= 5) {
                    result += Mix("rediscover", MixKind.Rediscover, "Riscoperte", "Li ascoltavi tanto, ritrovali", songs.map { it.toMixSong() }, now)
                }
            }

            // 4) Un mix per ognuno dei tuoi 3 artisti preferiti
            artistTime.entries
                .sortedByDescending { it.value }
                .map { it.key }
                .filter { it !in dislikedArtists }
                .take(3)
                .forEach { artist ->
                    val own = favorites.filter { s -> s.artists.any { it.name == artist } }.take(8)
                    val seed = own.firstOrNull() ?: return@forEach
                    val similar = related(seed.id).map { it.toMediaMetadata() }.filter { it.allowed() }.take(20)
                    val songs = interleave(own.shuffled(), similar).distinctBy { it.id }.take(25)
                    if (songs.size >= 8) {
                        result +=
                            Mix(
                                id = "artist-" + artist.lowercase().filter { it.isLetterOrDigit() },
                                kind = MixKind.Artist,
                                title = "Mix $artist",
                                subtitle = "$artist e artisti simili",
                                songs = songs.map { it.toMixSong() },
                                createdAt = now,
                            )
                    }
                }
            result
        }

    /** Alterna due liste: 1 da a, 1 da b, … */
    private fun <T> interleave(
        a: List<T>,
        b: List<T>,
    ): List<T> {
        val out = mutableListOf<T>()
        val max = maxOf(a.size, b.size)
        for (i in 0 until max) {
            a.getOrNull(i)?.let(out::add)
            b.getOrNull(i)?.let(out::add)
        }
        return out
    }

    /** Un elemento a testa da ogni lista, a giro. */
    private fun <T> roundRobin(lists: List<List<T>>): List<T> {
        val out = mutableListOf<T>()
        val max = lists.maxOfOrNull { it.size } ?: 0
        for (i in 0 until max) lists.forEach { list -> list.getOrNull(i)?.let(out::add) }
        return out
    }
}
