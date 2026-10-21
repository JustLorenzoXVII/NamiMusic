package com.justlorenzo.archivetunex.ios

import androidx.datastore.preferences.core.edit
import com.justlorenzo.archivetunex.ArchiveTuneXRuntime
import com.justlorenzo.archivetunex.ui.AtxActions
import com.justlorenzo.archivetunex.ui.LiquidGlassKey
import kotlinx.coroutines.CoroutineScope
import com.justlorenzo.archivetunex.insights.MixMaker
import kotlinx.coroutines.withContext
import moe.rukamori.archivetune.innertube.YouTube
import moe.rukamori.archivetune.innertube.models.AlbumItem
import moe.rukamori.archivetune.innertube.models.ArtistItem
import moe.rukamori.archivetune.innertube.models.EpisodeItem
import moe.rukamori.archivetune.innertube.models.PlaylistItem
import moe.rukamori.archivetune.innertube.models.SongItem
import moe.rukamori.archivetune.innertube.models.YTItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import moe.rukamori.archivetune.compat.AppContext
import moe.rukamori.archivetune.compat.media3.Player
import moe.rukamori.archivetune.models.MediaMetadata
import moe.rukamori.archivetune.models.toMediaMetadata
import moe.rukamori.archivetune.ui.utils.resize
import moe.rukamori.archivetune.utils.dataStore
import moe.rukamori.archivetune.compat.time.LocalDateTime
import moe.rukamori.archivetune.utils.toPlaybackAuthState

/** Un brano, come lo mostra l'interfaccia SwiftUI. */
data class NamiTrack(
    val id: String,
    val title: String,
    val artist: String,
    /** Copertina piccola (liste, mini player). */
    val artworkUrl: String?,
    /** Copertina grande (player). */
    val largeArtworkUrl: String?,
    val durationMs: Long,
    val isExplicit: Boolean,
)

/** Lo stato del player. */
data class NamiPlayerState(
    val track: NamiTrack?,
    val isPlaying: Boolean,
    val liked: Boolean,
    val shuffle: Boolean,
    /** 0 = niente, 1 = ripeti brano, 2 = ripeti tutto (come Media3). */
    val repeatMode: Int,
    val queueTitle: String?,
    val canSkipPrevious: Boolean,
    val canSkipNext: Boolean,
)

/** Una playlist della Libreria. */
data class NamiPlaylist(
    val id: String,
    val title: String,
    val songCount: Int,
    val thumbnails: List<String>,
    /** false = playlist di altri salvata da YouTube Music: non si modifica. */
    val isEditable: Boolean,
)

/** Un elemento di YouTube Music (Home, ricerca): kind 0 brano, 1 album, 2 playlist, 3 artista, 4 altro. */
data class NamiItem(
    val key: String,
    val kind: Int,
    val title: String,
    val subtitle: String,
    val artworkUrl: String?,
    val isExplicit: Boolean,
)

data class NamiSection(
    val title: String,
    val items: List<NamiItem>,
    /** Solo brani: si mostrano come elenco (come "Scelte rapide" di Apple Music). */
    val allSongs: Boolean,
)

data class NamiChip(
    val title: String,
    val params: String?,
)

data class NamiHome(
    val chips: List<NamiChip>,
    val sections: List<NamiSection>,
)

/** Un mix "Fatte per te". */
data class NamiMix(
    val id: String,
    val title: String,
    val subtitle: String,
    val covers: List<String>,
    val songCount: Int,
    /** 0 per te, 1 scoperte, 2 riscoperte, 3 artista */
    val kind: Int,
)

/** Album o playlist di YouTube Music aperti dalla Home. */
data class NamiCollection(
    val key: String,
    val title: String,
    val subtitle: String,
    val artworkUrl: String?,
    val tracks: List<NamiTrack>,
)

/**
 * Ponte tra la parte Kotlin di Nami (riproduzione, YouTube Music, database) e l'interfaccia
 * SwiftUI di iPhone (iosApp/Nami/…). Swift lo usa come `NamiBridge.shared`.
 * Le funzioni "observe…" chiamano subito il listener con il valore attuale e poi a ogni cambio,
 * sempre sul thread principale; restituiscono un [NamiSubscription] da cancellare.
 */
object NamiBridge {
    internal val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    internal var actions: AtxActions? = null

    /** Avvia motore, database e player (una volta sola). */
    fun start() {
        ArchiveTuneXRuntime.start()
        if (actions == null) {
            actions =
                AtxActions(
                    player = ArchiveTuneXRuntime.playerConnection,
                    database = ArchiveTuneXRuntime.database,
                    downloads = ArchiveTuneXRuntime.downloadUtil,
                )
            recoverHiddenPlaylists()
        }
    }

    /**
     * Le playlist create nei giorni scorsi finivano nel database senza "bookmarkedAt" e quindi
     * non comparivano in Libreria: le rende visibili (solo quelle tue, create di recente).
     */
    private fun recoverHiddenPlaylists() {
        scope.launch {
            withContext(Dispatchers.Default) {
                runCatching {
                    val since = LocalDateTime.now().minusDays(7)
                    database
                        .allPlaylistsForDownloads()
                        .first()
                        .map { it.playlist }
                        .filter { it.bookmarkedAt == null && it.isEditable && it.createdAt?.isAfter(since) == true }
                        .forEach { database.update(it.copy(bookmarkedAt = it.createdAt ?: LocalDateTime.now())) }
                }
            }
        }
    }

    internal val connection get() = ArchiveTuneXRuntime.playerConnection
    internal val database get() = ArchiveTuneXRuntime.database

    // region Interfaccia iOS sì/no

    /** L'interruttore "Tema iOS": acceso = interfaccia SwiftUI, spento = quella di sempre (Compose). */
    fun isIosUiEnabled(): Boolean = runBlocking { AppContext.dataStore.data.first()[LiquidGlassKey] ?: false }

    fun setIosUiEnabled(enabled: Boolean) {
        scope.launch { AppContext.dataStore.edit { it[LiquidGlassKey] = enabled } }
    }

    fun observeIosUi(listener: (Boolean) -> Unit): NamiSubscription =
        collect(AppContext.dataStore.data.map { it[LiquidGlassKey] ?: false }.distinctUntilChanged(), listener)

    // endregion

    // region Player

    fun observePlayer(listener: (NamiPlayerState) -> Unit): NamiSubscription {
        val c = connection
        val base =
            combine(c.mediaMetadata, c.isPlaying, c.currentSong, c.shuffleModeEnabled, c.repeatMode) { meta, playing, song, shuffle, repeat ->
                NamiPlayerState(
                    track = meta?.toNamiTrack(),
                    isPlaying = playing,
                    liked = song?.song?.liked == true,
                    shuffle = shuffle,
                    repeatMode = repeat,
                    queueTitle = null,
                    canSkipPrevious = true,
                    canSkipNext = true,
                )
            }
        val full =
            combine(base, c.queueTitle, c.canSkipPrevious, c.canSkipNext) { state, title, prev, next ->
                state.copy(queueTitle = title, canSkipPrevious = prev, canSkipNext = next)
            }.distinctUntilChanged()
        return collect(full, listener)
    }

    /** Posizione attuale in millisecondi (Swift la legge di continuo mentre il player è aperto). */
    fun positionMs(): Long = connection.player.currentPosition.coerceAtLeast(0)

    /** Durata del brano in millisecondi (0 se non si conosce ancora). */
    fun durationMs(): Long = connection.player.duration.coerceAtLeast(0)

    fun togglePlayPause() {
        actions?.togglePlayPause()
    }

    fun next() = connection.seekToNext()

    fun previous() = connection.seekToPrevious()

    fun seekTo(positionMs: Long) = connection.player.seekTo(positionMs)

    fun toggleLike() = connection.toggleLike()

    fun toggleShuffle() {
        connection.player.shuffleModeEnabled = !connection.player.shuffleModeEnabled
    }

    fun cycleRepeat() {
        val p = connection.player
        p.repeatMode =
            when (p.repeatMode) {
                Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                else -> Player.REPEAT_MODE_OFF
            }
    }

    // endregion

    // region Libreria

    fun observeLikedSongs(listener: (List<NamiTrack>) -> Unit): NamiSubscription =
        collect(database.likedSongsByCreateDateAsc().map { songs -> songs.reversed().map { it.toMediaMetadata().toNamiTrack() } }, listener)

    fun observePlaylists(listener: (List<NamiPlaylist>) -> Unit): NamiSubscription =
        collect(
            database.playlistsByCreateDateAsc().map { list ->
                list.reversed().map { p ->
                    NamiPlaylist(
                        id = p.id,
                        title = p.title,
                        songCount = p.songCount,
                        thumbnails = p.thumbnails.take(4).map { it.resize(300, 300) },
                        isEditable = p.playlist.isEditable,
                    )
                }
            },
            listener,
        )

    fun observePlaylistSongs(
        playlistId: String,
        listener: (List<NamiTrack>) -> Unit,
    ): NamiSubscription =
        collect(database.playlistSongs(playlistId).map { songs -> songs.map { it.song.toMediaMetadata().toNamiTrack() } }, listener)

    /** Fa partire i brani preferiti dal brano [startIndex] (o in ordine casuale). */
    fun playLikedSongs(
        startIndex: Int,
        shuffle: Boolean,
    ) {
        scope.launch {
            val songs = database.likedSongsByCreateDateAsc().first().reversed().map { it.toMediaMetadata() }
            actions?.playList(songs, startIndex = startIndex, title = "Preferiti", shuffle = shuffle)
        }
    }

    /** Fa partire una playlist della Libreria dal brano [startIndex] (o in ordine casuale). */
    fun playPlaylist(
        playlistId: String,
        title: String,
        startIndex: Int,
        shuffle: Boolean,
    ) {
        scope.launch {
            val songs = database.playlistSongs(playlistId).first().map { it.song.toMediaMetadata() }
            actions?.playList(songs, startIndex = startIndex, title = title, shuffle = shuffle)
        }
    }

    // endregion

    // region Modifica della Libreria e menu dei brani

    /** Tutti i brani passati a Swift, per poterli poi far partire, aggiungere o mettere tra i preferiti dal loro id. */
    internal val knownSongs = HashMap<String, MediaMetadata>()

    /** true se l'account di YouTube Music è collegato (le playlist nuove possono nascere anche lì). */
    fun isLoggedIn(): Boolean =
        runBlocking { AppContext.dataStore.data.first().toPlaybackAuthState().hasLoginCookie }

    fun createPlaylist(
        name: String,
        onYouTube: Boolean,
        firstSongId: String?,
    ) {
        actions?.createPlaylist(name, onYouTube = onYouTube && isLoggedIn(), firstSong = firstSongId?.let { knownSongs[it] })
    }

    fun renamePlaylist(
        playlistId: String,
        name: String,
    ) {
        scope.launch {
            val playlist = database.playlist(playlistId).first()?.playlist ?: return@launch
            actions?.renamePlaylist(playlist, name)
        }
    }

    fun deletePlaylist(playlistId: String) {
        scope.launch {
            val playlist = database.playlist(playlistId).first()?.playlist ?: return@launch
            actions?.deletePlaylist(playlist)
        }
    }

    /** Toglie il brano in posizione [index] (ordine dell'elenco mostrato). */
    fun removeFromPlaylist(
        playlistId: String,
        index: Int,
    ) {
        scope.launch {
            val playlist = database.playlist(playlistId).first()?.playlist ?: return@launch
            val entry = database.playlistSongs(playlistId).first().getOrNull(index) ?: return@launch
            actions?.removeFromPlaylist(playlist, entry.map)
        }
    }

    /** Sposta un brano da [from] a [to] (anche su YouTube Music, se la playlist è lì). */
    fun moveInPlaylist(
        playlistId: String,
        from: Int,
        to: Int,
    ) {
        if (from == to) return
        scope.launch {
            val playlist = database.playlist(playlistId).first()?.playlist ?: return@launch
            val entries = database.playlistSongs(playlistId).first()
            val moved = entries.getOrNull(from) ?: return@launch
            if (to !in entries.indices) return@launch
            val successor = entries.getOrNull(if (from > to) to else to + 1)
            actions?.moveInPlaylist(playlist, from, to, moved.map.setVideoId, successor?.map?.setVideoId)
        }
    }

    fun addToPlaylist(
        playlistId: String,
        songId: String,
    ) {
        val song = knownSongs[songId] ?: return
        actions?.addToPlaylist(playlistId, song)
    }

    fun playSong(songId: String) {
        val song = knownSongs[songId] ?: return
        actions?.playSong(song)
    }

    fun playNext(songId: String) {
        val song = knownSongs[songId] ?: return
        actions?.playNext(song)
    }

    fun addToQueue(songId: String) {
        val song = knownSongs[songId] ?: return
        actions?.addToQueue(song)
    }

    /** Mette o toglie il brano dai preferiti (anche se non sta suonando). */
    fun toggleLikeSong(songId: String) {
        val song = knownSongs[songId] ?: return
        scope.launch {
            withContext(Dispatchers.Default) {
                database.withTransaction {
                    val entity =
                        getSongById(song.id) ?: run {
                            insert(song)
                            getSongById(song.id)
                        } ?: return@withTransaction
                    update(entity.song.toggleLike())
                }
            }
        }
    }

    // endregion

    // region Home

    /** Gli ultimi elementi caricati, per farli partire o aprirli dal loro [NamiItem.key]. */
    internal val items = HashMap<String, YTItem>()
    internal val collections = HashMap<String, Pair<String, List<MediaMetadata>>>()

    /** Carica la Home di YouTube Music (con il mood [params], o null per tutti). */
    fun loadHome(
        params: String?,
        callback: (NamiHome?, String?) -> Unit,
    ) {
        scope.launch {
            val result = withContext(Dispatchers.Default) { YouTube.home(params = params) }
            result.fold(
                onSuccess = { page ->
                    val sections =
                        page.sections.filter { it.items.isNotEmpty() }.map { section ->
                            NamiSection(
                                title = section.title,
                                items = section.items.mapNotNull { it.toNamiItem() },
                                allSongs = section.items.all { it is SongItem },
                            )
                        }
                    val chips = page.chips.orEmpty().map { NamiChip(it.title, it.endpoint?.params) }
                    callback(NamiHome(chips, sections), null)
                },
                onFailure = { callback(null, it.message ?: "Errore di rete") },
            )
        }
    }

    /** Tocco su un elemento: un brano parte (con la radio dopo). Album e playlist si aprono da Swift. */
    fun playItem(key: String) {
        when (val item = items[key]) {
            is SongItem -> actions?.playSong(item)
            is EpisodeItem -> actions?.playEndpoint(item.endpoint)
            else -> Unit
        }
    }

    fun observeRecent(listener: (List<NamiTrack>) -> Unit): NamiSubscription =
        collect(database.recentSongs(20).map { songs -> songs.map { it.toMediaMetadata().toNamiTrack() } }, listener)

    /** Un brano ascoltato di recente: parte con la sua radio. */
    fun playRecent(index: Int) {
        scope.launch {
            val song = database.recentSongs(20).first().getOrNull(index) ?: return@launch
            actions?.playSong(song.toMediaMetadata())
        }
    }

    fun observeMixes(listener: (List<NamiMix>) -> Unit): NamiSubscription {
        scope.launch { runCatching { MixMaker.refreshIfNeeded(database) } }
        return collect(
            MixMaker.mixes.map { list ->
                list.map { mix ->
                    NamiMix(
                        id = mix.id,
                        title = mix.title,
                        subtitle = mix.subtitle,
                        covers = mix.songs.mapNotNull { it.thumbnailUrl?.resize(300, 300) }.distinct().take(4),
                        songCount = mix.songs.size,
                        kind = mix.kind.ordinal,
                    )
                }
            },
            listener,
        )
    }

    fun refreshMixes() {
        scope.launch { runCatching { MixMaker.refreshIfNeeded(database, force = true) } }
    }

    fun mixTracks(id: String): List<NamiTrack> =
        MixMaker.mixes.value.firstOrNull { it.id == id }?.songs.orEmpty().map { it.toMediaMetadata().toNamiTrack() }

    fun playMix(
        id: String,
        startIndex: Int,
        shuffle: Boolean,
    ) {
        val mix = MixMaker.mixes.value.firstOrNull { it.id == id } ?: return
        actions?.playList(mix.songs.map { it.toMediaMetadata() }, startIndex = startIndex, title = mix.title, shuffle = shuffle)
    }

    /** Apre un album o una playlist di YouTube Music (dalla chiave di un [NamiItem]). */
    fun loadCollection(
        key: String,
        callback: (NamiCollection?, String?) -> Unit,
    ) {
        val item = items[key]
        scope.launch {
            val result =
                withContext(Dispatchers.Default) {
                    runCatching {
                        when (item) {
                            is AlbumItem -> {
                                val page = YouTube.album(item.browseId).getOrThrow()
                                Triple(page.album.title, page.album.artists?.joinToString(", ") { it.name }.orEmpty(), page.songs to page.album.thumbnail)
                            }
                            is PlaylistItem -> {
                                val first = YouTube.playlist(item.id).getOrThrow()
                                val songs = first.songs.toMutableList()
                                var continuation = first.songsContinuation ?: first.continuation
                                var pages = 0
                                while (continuation != null && pages < 20) {
                                    val next = YouTube.playlistContinuation(continuation, item.id).getOrNull() ?: break
                                    songs += next.songs
                                    continuation = next.continuation
                                    pages++
                                }
                                Triple(first.playlist.title, first.playlist.author?.name.orEmpty(), songs.distinctBy { it.id } to first.playlist.thumbnail)
                            }
                            else -> error("Non disponibile")
                        }
                    }
                }
            result.fold(
                onSuccess = { (title, subtitle, data) ->
                    val metadata = data.first.map { it.toMediaMetadata() }
                    collections[key] = title to metadata
                    callback(NamiCollection(key, title, subtitle, data.second?.resize(1080, 1080), metadata.map { it.toNamiTrack() }), null)
                },
                onFailure = { callback(null, it.message ?: "Errore di rete") },
            )
        }
    }

    fun playCollection(
        key: String,
        startIndex: Int,
        shuffle: Boolean,
    ) {
        val (title, songs) = collections[key] ?: return
        actions?.playList(songs, startIndex = startIndex, title = title, shuffle = shuffle)
    }

    internal fun namiItem(item: YTItem): NamiItem? = item.toNamiItem()

    private fun YTItem.toNamiItem(): NamiItem? {
        val (kind, subtitle) =
            when (this) {
                is SongItem -> 0 to artists.joinToString(", ") { it.name }
                is AlbumItem -> 1 to (artists?.joinToString(", ") { it.name } ?: year?.toString().orEmpty())
                is PlaylistItem -> 2 to (author?.name ?: songCountText.orEmpty())
                is ArtistItem -> 3 to ""
                is EpisodeItem -> 4 to ""
                else -> return null
            }
        val key = "$kind:$id"
        items[key] = this
        return NamiItem(
            key = key,
            kind = kind,
            title = title,
            subtitle = subtitle,
            artworkUrl = thumbnail?.resize(400, 400),
            isExplicit = explicit,
        )
    }

    // endregion

    internal fun <T> collect(
        flow: kotlinx.coroutines.flow.Flow<T>,
        listener: (T) -> Unit,
    ): NamiSubscription {
        val job = scope.launch { flow.collect { listener(it) } }
        return NamiSubscription(job)
    }
}

/** Un'osservazione in corso: Swift chiama [cancel] quando la vista sparisce. */
class NamiSubscription internal constructor(
    private val job: Job,
) {
    fun cancel() = job.cancel()
}

internal fun MediaMetadata.toNamiTrack(): NamiTrack {
    NamiBridge.knownSongs[id] = this
    return NamiTrack(
        id = id,
        title = title,
        artist = artists.joinToString(", ") { it.name },
        artworkUrl = thumbnailUrl?.resize(240, 240),
        largeArtworkUrl = thumbnailUrl?.resize(1080, 1080),
        durationMs = duration * 1000L,
        isExplicit = explicit,
    )
}
