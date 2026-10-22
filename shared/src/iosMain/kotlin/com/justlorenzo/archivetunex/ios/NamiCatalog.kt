package com.justlorenzo.archivetunex.ios

import com.justlorenzo.archivetunex.insights.ArtistResolver
import com.justlorenzo.archivetunex.insights.Insights
import com.justlorenzo.archivetunex.insights.ListeningStats
import com.justlorenzo.archivetunex.insights.StatsPeriod
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import moe.rukamori.archivetune.compat.media3.Download
import moe.rukamori.archivetune.db.entities.LyricsEntity
import moe.rukamori.archivetune.db.entities.SearchHistory
import moe.rukamori.archivetune.extensions.metadata
import moe.rukamori.archivetune.innertube.YouTube
import moe.rukamori.archivetune.innertube.YouTube.SearchFilter
import moe.rukamori.archivetune.innertube.models.ArtistItem
import moe.rukamori.archivetune.innertube.models.BrowseEndpoint
import moe.rukamori.archivetune.innertube.models.SongItem
import moe.rukamori.archivetune.innertube.models.YTItem
import moe.rukamori.archivetune.innertube.pages.ArtistPage
import moe.rukamori.archivetune.innertube.pages.ArtistSectionLayout
import moe.rukamori.archivetune.lyrics.LyricsHelper
import moe.rukamori.archivetune.lyrics.LyricsUtils
import moe.rukamori.archivetune.models.MediaMetadata
import moe.rukamori.archivetune.models.toMediaMetadata
import moe.rukamori.archivetune.ui.utils.resize
import org.koin.mp.KoinPlatform

/** Una pagina fatta di sezioni (artista, mood): intestazione e contenuto. */
data class NamiPage(
    val key: String,
    val title: String,
    val subtitle: String,
    val artworkUrl: String?,
    val description: String?,
    val sections: List<NamiSection>,
    /** Si può far partire (artista: Casuale e Radio). */
    val canPlay: Boolean,
)

data class NamiMood(
    val key: String,
    val title: String,
    /** Colore ARGB della striscia di YouTube Music. */
    val color: Long,
)

data class NamiMoodGroup(
    val title: String,
    val items: List<NamiMood>,
)

data class NamiLyricLine(
    val timeMs: Long,
    val text: String,
)

/** Testo del brano: righe con il tempo se sincronizzato, altrimenti [plain]. */
data class NamiLyrics(
    val songId: String,
    val synced: Boolean,
    val lines: List<NamiLyricLine>,
    val plain: String?,
)

data class NamiQueue(
    val title: String?,
    val tracks: List<NamiTrack>,
    val currentIndex: Int,
)

/** Stato di un download: 0 in coda, 2 in corso, 3 completato, 4 non riuscito (come Media3). */
data class NamiDownload(
    val id: String,
    val state: Int,
    val progress: Float,
)

data class NamiStatRow(
    val key: String,
    val title: String,
    val subtitle: String,
    val artworkUrl: String?,
    val timeMs: Long,
    val plays: Int,
)

data class NamiGenre(
    val title: String,
    val share: Float,
)

data class NamiStats(
    val totalMs: Long,
    val plays: Int,
    val distinctSongs: Int,
    val distinctArtists: Int,
    val topSongs: List<NamiStatRow>,
    val topArtists: List<NamiStatRow>,
    /** Minuti ascoltati per ogni ora del giorno (24 valori). */
    val byHourMinutes: List<Int>,
    val genres: List<NamiGenre>,
)

/**
 * Seconda metà del ponte verso SwiftUI: ricerca, artisti, mood, testi, coda, download e statistiche.
 * Swift lo usa come `NamiCatalog.shared`.
 */
object NamiCatalog {
    private val bridge get() = NamiBridge
    private val scope get() = NamiBridge.scope
    private val database get() = NamiBridge.database
    private val connection get() = NamiBridge.connection

    private fun section(
        title: String,
        items: List<YTItem>,
        forceList: Boolean = false,
    ): NamiSection? {
        val converted = items.mapNotNull { bridge.namiItem(it) }
        if (converted.isEmpty()) return null
        return NamiSection(title = title, items = converted, allSongs = forceList || items.all { it is SongItem })
    }

    private fun failure(error: Throwable): String = friendlyError(error)

    // region Ricerca

    fun searchSuggestions(
        query: String,
        callback: (List<String>) -> Unit,
    ) {
        scope.launch {
            val list =
                withContext(Dispatchers.Default) {
                    YouTube.searchSuggestions(query).getOrNull()?.queries.orEmpty().take(8)
                }
            callback(list)
        }
    }

    fun observeSearchHistory(listener: (List<String>) -> Unit): NamiSubscription =
        bridge.collect(
            database.searchHistory().map { list -> list.map { it.query }.distinct().take(15) }.distinctUntilChanged(),
            listener,
        )

    fun addSearchHistory(query: String) {
        val q = query.trim()
        if (q.isEmpty()) return
        database.query { insert(SearchHistory(query = q)) }
    }

    fun clearSearchHistory() {
        database.query { clearSearchHistory() }
    }

    /**
     * Cerca [query]. [kind]: 0 migliori risultati, 1 brani, 2 album, 3 artisti, 4 playlist, 5 playlist utenti, 6 video.
     */
    fun search(
        query: String,
        kind: Int,
        callback: (List<NamiSection>?, String?) -> Unit,
    ) {
        scope.launch {
            val result =
                withContext(Dispatchers.Default) {
                    runCatching {
                        if (kind == 0) {
                            coroutineScope {
                                val summary = async { YouTube.searchSummary(query) }
                                val featured = async { YouTube.search(query, SearchFilter.FILTER_FEATURED_PLAYLIST).getOrNull()?.items.orEmpty() }
                                val community = async { YouTube.search(query, SearchFilter.FILTER_COMMUNITY_PLAYLIST).getOrNull()?.items.orEmpty() }
                                val page = summary.await().getOrThrow()
                                val seen = mutableSetOf<String>()
                                val sections = mutableListOf<Pair<String, List<YTItem>>>()
                                page.summaries.forEach { s ->
                                    val items = s.items.filter { seen.add(it.id) }
                                    if (items.isNotEmpty()) sections += s.title to items
                                }
                                val official = featured.await().filter { seen.add(it.id) }.take(10)
                                if (official.isNotEmpty()) sections += "Playlist in evidenza" to official
                                val users = community.await().filter { seen.add(it.id) }.take(10)
                                if (users.isNotEmpty()) sections += "Playlist degli utenti" to users
                                sections
                            }
                        } else {
                            val filter =
                                when (kind) {
                                    1 -> SearchFilter.FILTER_SONG
                                    2 -> SearchFilter.FILTER_ALBUM
                                    3 -> SearchFilter.FILTER_ARTIST
                                    4 -> SearchFilter.FILTER_FEATURED_PLAYLIST
                                    5 -> SearchFilter.FILTER_COMMUNITY_PLAYLIST
                                    else -> SearchFilter.FILTER_VIDEO
                                }
                            listOf("" to YouTube.search(query, filter).getOrThrow().items)
                        }
                    }
                }
            result.fold(
                onSuccess = { list ->
                    // Le sezioni dei filtri sono sempre elenchi (una riga per elemento)
                    callback(list.mapNotNull { (title, items) -> section(title, items, forceList = kind != 0) }, null)
                },
                onFailure = { callback(null, failure(it)) },
            )
        }
    }

    // endregion

    // region Mood e generi

    private val moods = HashMap<String, Pair<String, BrowseEndpoint>>()

    fun loadMoods(callback: (List<NamiMoodGroup>?, String?) -> Unit) {
        scope.launch {
            val result = withContext(Dispatchers.Default) { YouTube.moodAndGenres() }
            result.fold(
                onSuccess = { groups ->
                    callback(
                        groups.map { group ->
                            NamiMoodGroup(
                                title = group.title,
                                items =
                                    group.items.map { mood ->
                                        val key = "mood:${mood.endpoint.browseId}:${mood.endpoint.params}"
                                        moods[key] = mood.title to mood.endpoint
                                        NamiMood(key, mood.title, mood.stripeColor)
                                    },
                            )
                        },
                        null,
                    )
                },
                onFailure = { callback(null, failure(it)) },
            )
        }
    }

    fun loadMood(
        key: String,
        callback: (NamiPage?, String?) -> Unit,
    ) {
        val (title, endpoint) = moods[key] ?: return callback(null, "Non disponibile")
        scope.launch {
            val result = withContext(Dispatchers.Default) { YouTube.browse(endpoint.browseId, endpoint.params) }
            result.fold(
                onSuccess = { page ->
                    val sections = page.items.mapNotNull { section(it.title.orEmpty(), it.items) }
                    callback(NamiPage(key, page.title ?: title, "", page.thumbnail?.resize(1080, 1080), null, sections, false), null)
                },
                onFailure = { callback(null, failure(it)) },
            )
        }
    }

    // endregion

    // region Artisti

    private val artists = HashMap<String, ArtistPage>()

    /** [key] è la chiave di un elemento (Home, ricerca) oppure direttamente l'id del canale dell'artista. */
    fun loadArtist(
        key: String,
        callback: (NamiPage?, String?) -> Unit,
    ) {
        val browseId = (bridge.items[key] as? ArtistItem)?.id ?: key.removePrefix("3:")
        scope.launch {
            val result = withContext(Dispatchers.Default) { YouTube.artist(browseId) }
            result.fold(
                onSuccess = { page ->
                    artists[key] = page
                    val sections =
                        page.sections.mapNotNull { s ->
                            section(s.title, s.items, forceList = s.layout == ArtistSectionLayout.LIST && s.items.all { it is SongItem })
                        }
                    val subtitle = page.artist.monthlyListenerCountText ?: page.artist.subscriberCountText ?: ""
                    callback(
                        NamiPage(
                            key = key,
                            title = page.artist.title,
                            subtitle = subtitle,
                            artworkUrl = page.artist.thumbnail?.resize(1200, 1200),
                            description = page.description,
                            sections = sections,
                            canPlay = page.artist.shuffleEndpoint != null || page.artist.radioEndpoint != null,
                        ),
                        null,
                    )
                },
                onFailure = { callback(null, failure(it)) },
            )
        }
    }

    /** Casuale (o Radio) dell'artista caricato con [loadArtist]. */
    fun playArtist(
        key: String,
        shuffle: Boolean,
    ) {
        val artist = artists[key]?.artist ?: return
        val endpoint = (if (shuffle) artist.shuffleEndpoint else artist.radioEndpoint) ?: artist.shuffleEndpoint ?: artist.radioEndpoint ?: return
        bridge.actions?.playEndpoint(endpoint)
    }

    /** Il brano di un elemento (Home, ricerca), per il menu "tieni premuto". */
    fun trackForItem(key: String): NamiTrack? = (bridge.items[key] as? SongItem)?.toMediaMetadata()?.toNamiTrack()

    // endregion

    // region Testi

    fun loadLyrics(
        songId: String,
        callback: (NamiLyrics?) -> Unit,
    ) {
        scope.launch {
            val raw =
                withContext(Dispatchers.Default) {
                    try {
                        database.lyrics(songId).first()?.lyrics ?: run {
                            val song = bridge.knownSongs[songId] ?: connection.mediaMetadata.value?.takeIf { it.id == songId } ?: return@run null
                            val text = KoinPlatform.getKoin().get<LyricsHelper>().getLyrics(song)
                            database.query { insertLyricsIfAbsent(id = songId, lyrics = text) }
                            text
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        null
                    }
                }
            if (raw == null || raw == LyricsEntity.LYRICS_NOT_FOUND || raw.isBlank()) {
                callback(null)
                return@launch
            }
            val entries = withContext(Dispatchers.Default) { runCatching { LyricsUtils.parseLyrics(raw) }.getOrDefault(emptyList()) }
            val lines = entries.filter { it.text.isNotBlank() }.map { NamiLyricLine(it.time, it.text.trim()) }
            if (lines.size >= 2) {
                callback(NamiLyrics(songId, true, lines, null))
            } else {
                val plain = raw.lines().map { it.replace(Regex("^\\[[^]]*]\\s*"), "") }.joinToString("\n").trim()
                callback(NamiLyrics(songId, false, emptyList(), plain))
            }
        }
    }

    // endregion

    // region Coda

    fun observeQueue(listener: (NamiQueue) -> Unit): NamiSubscription =
        bridge.collect(
            combine(connection.queueWindows, connection.currentWindowIndex, connection.queueTitle) { windows, index, title ->
                NamiQueue(
                    title = title,
                    tracks = windows.mapNotNull { it.mediaItem.metadata?.toNamiTrack() },
                    currentIndex = index,
                )
            },
            listener,
        )

    fun playQueueItem(index: Int) {
        val window = connection.queueWindows.value.getOrNull(index) ?: return
        val player = connection.player
        player.seekToDefaultPosition(window.firstPeriodIndex)
        player.prepare()
        player.playWhenReady = true
    }

    fun removeFromQueue(index: Int) {
        if (index == connection.currentWindowIndex.value) return
        if (index in 0 until connection.player.mediaItemCount) connection.player.removeMediaItem(index)
    }

    fun moveInQueue(
        from: Int,
        to: Int,
    ) {
        val count = connection.player.mediaItemCount
        if (from == to || from !in 0 until count || to !in 0 until count) return
        connection.player.moveMediaItem(from, to)
    }

    /** Toglie tutto quello che viene dopo il brano in riproduzione. */
    fun clearUpNext() {
        val player = connection.player
        val current = connection.currentWindowIndex.value
        if (current >= 0 && current + 1 < player.mediaItemCount) player.removeMediaItems(current + 1, player.mediaItemCount)
    }

    // endregion

    // region Download

    fun observeDownloads(listener: (List<NamiDownload>) -> Unit): NamiSubscription =
        bridge.collect(
            bridge.actions!!
                .downloads.downloads
                .map { map -> map.values.map { NamiDownload(it.request.id, it.state, (it.percentDownloaded / 100f).coerceIn(0f, 1f)) } },
            listener,
        )

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    fun observeDownloadedSongs(listener: (List<NamiTrack>) -> Unit): NamiSubscription =
        bridge.collect(
            bridge.actions!!
                .downloads.downloads
                .map { map -> map.filterValues { it.state != Download.STATE_FAILED }.keys.toList() }
                .distinctUntilChanged()
                .flatMapLatest { ids -> database.songsByIds(ids) }
                .map { songs -> songs.map { it.toMediaMetadata().toNamiTrack() } },
            listener,
        )

    fun download(songId: String) {
        val song = bridge.knownSongs[songId] ?: return
        bridge.actions?.download(song)
    }

    fun downloadAll(songIds: List<String>) {
        songIds.mapNotNull { bridge.knownSongs[it] }.forEach { bridge.actions?.download(it) }
    }

    fun removeDownload(songId: String) {
        bridge.actions?.removeDownload(songId)
    }

    fun playSongs(
        songIds: List<String>,
        title: String,
        startIndex: Int,
        shuffle: Boolean,
    ) {
        val songs: List<MediaMetadata> = songIds.mapNotNull { bridge.knownSongs[it] }
        if (songs.isEmpty()) return
        bridge.actions?.playList(songs, startIndex = startIndex.coerceIn(0, songs.size - 1), title = title, shuffle = shuffle)
    }

    // endregion

    // region Statistiche

    /** [period]: 0 sette giorni, 1 trenta giorni, 2 anno, 3 sempre. */
    fun loadStats(
        period: Int,
        callback: (NamiStats?) -> Unit,
    ) {
        val p = StatsPeriod.entries.getOrElse(period) { StatsPeriod.Month }
        scope.launch {
            val stats =
                runCatching {
                    val base = Insights.compute(database, p)
                    runCatching { Insights.withLiveData(base) }.getOrDefault(base)
                }.getOrNull()
            if (stats == null) {
                callback(null)
                return@launch
            }
            callback(stats.toNami())
            // Foto e pagina vera degli artisti (nella cronologia hanno spesso un codice interno che
            // YouTube non conosce): si cercano una volta sola, poi si aggiorna la schermata
            val missing = stats.topArtists.take(25).filter { ArtistResolver.get(it.name)?.browseId == null && !ArtistResolver.isYouTubeId(it.id) }
            if (missing.isNotEmpty()) {
                runCatching { ArtistResolver.resolve(stats.topArtists.take(25).map { it.name to it.id }) }
                callback(stats.toNami())
            }
        }
    }

    private fun ListeningStats.toNami(): NamiStats =
        NamiStats(
            totalMs = totalMs,
            plays = plays,
            distinctSongs = distinctSongs,
            distinctArtists = distinctArtists,
            topSongs =
                topSongs.take(25).map {
                    val track = it.song.toNamiTrack()
                    NamiStatRow(track.id, track.title, track.artist, track.artworkUrl, it.timeMs, it.plays)
                },
            topArtists =
                topArtists.take(25).map {
                    val found = ArtistResolver.get(it.name)
                    // Si apre solo un artista che esiste su YouTube Music
                    val pageId = found?.browseId ?: it.id?.takeIf { id -> ArtistResolver.isYouTubeId(id) }
                    val photo = it.thumbnailUrl ?: found?.thumbnailUrl
                    NamiStatRow(pageId ?: "", it.name, "", photo?.resize(400, 400), it.timeMs, it.plays)
                },
            byHourMinutes = byHour.map { (it / 60_000L).toInt() },
            genres = genres.take(8).map { NamiGenre(it.genre.label, it.share) },
        )

    // endregion
}
