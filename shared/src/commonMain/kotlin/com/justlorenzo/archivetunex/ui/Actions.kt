package com.justlorenzo.archivetunex.ui

import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import moe.rukamori.archivetune.compat.Timber
import moe.rukamori.archivetune.compat.time.LocalDateTime
import moe.rukamori.archivetune.db.entities.PlaylistEntity
import moe.rukamori.archivetune.db.entities.PlaylistSongMap
import moe.rukamori.archivetune.innertube.YouTube
import moe.rukamori.archivetune.innertube.models.PlaylistItem
import moe.rukamori.archivetune.compat.media3.DownloadRequest
import moe.rukamori.archivetune.compat.toUri
import moe.rukamori.archivetune.db.MusicDatabase
import moe.rukamori.archivetune.extensions.toMediaItem
import moe.rukamori.archivetune.innertube.models.SongItem
import moe.rukamori.archivetune.innertube.models.distinctByPlaylistEntry
import moe.rukamori.archivetune.innertube.models.WatchEndpoint
import moe.rukamori.archivetune.models.MediaMetadata
import moe.rukamori.archivetune.models.toMediaMetadata
import moe.rukamori.archivetune.playback.DownloadUtil
import moe.rukamori.archivetune.playback.PlayerConnection
import moe.rukamori.archivetune.playback.queues.ListQueue
import moe.rukamori.archivetune.playback.queues.YouTubeQueue

/**
 * Le azioni che l'interfaccia chiede al motore: riprodurre, mettere in coda, scaricare.
 * Tutte le schermate passano da qui, così il comportamento è uguale ovunque.
 */
class AtxActions(
    val player: PlayerConnection,
    val database: MusicDatabase,
    val downloads: DownloadUtil,
) {
    /**
     * Playlist eliminate da YouTube Music in questa sessione: la Libreria le nasconde subito,
     * anche se l'elenco del tuo account era già stato caricato prima.
     */
    val deletedPlaylistIds = androidx.compose.runtime.mutableStateListOf<String>()

    /** Un brano singolo (da ricerca o Home): parte il brano e poi la radio di YouTube Music. */
    fun playSong(song: SongItem) {
        player.player.shuffleModeEnabled = false
        player.playQueue(YouTubeQueue.radio(song.toMediaMetadata()))
    }

    fun playSong(song: MediaMetadata) {
        player.player.shuffleModeEnabled = false
        player.playQueue(YouTubeQueue.radio(song))
    }

    /** Una lista (album, playlist, scaricati): parte dal brano scelto e segue l'ordine. */
    fun playList(
        songs: List<MediaMetadata>,
        startIndex: Int = 0,
        title: String? = null,
        shuffle: Boolean = false,
    ) {
        if (songs.isEmpty()) return
        player.player.shuffleModeEnabled = shuffle
        val start = if (shuffle) songs.indices.random() else startIndex
        player.playQueue(ListQueue(title = title, items = songs.map { it.toMediaItem() }, startIndex = start))
    }

    /** Playlist o mix di YouTube Music (es. il pulsante "Radio" di un artista). */
    fun playEndpoint(endpoint: WatchEndpoint) {
        player.player.shuffleModeEnabled = false
        player.playQueue(YouTubeQueue(endpoint))
    }

    fun playNext(song: MediaMetadata) = player.playNext(song.toMediaItem())

    fun addToQueue(song: MediaMetadata) = player.addToQueue(song.toMediaItem())

    fun togglePlayPause() {
        val p = player.player
        if (p.isPlaying) {
            p.pause()
        } else {
            if (p.playbackState == moe.rukamori.archivetune.compat.media3.Player.STATE_ENDED) p.seekTo(p.currentMediaItemIndex, 0L)
            p.prepare()
            p.play()
        }
    }

    /** Scarica un brano: lo salva nel database e lo passa al gestore dei download. */
    fun download(song: MediaMetadata) {
        database.transaction { insert(song) }
        downloads.addDownload(
            DownloadRequest
                .Builder(song.id, song.id.toUri())
                .setCustomCacheKey(song.id)
                .setData(song.title.encodeToByteArray())
                .build(),
        )
    }

    fun downloadAll(songs: List<MediaMetadata>) = songs.forEach(::download)

    fun removeDownload(songId: String) = downloads.removeDownload(songId)

    // region Playlist

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private fun launchSafely(
        what: String,
        block: suspend () -> Unit,
    ) {
        scope.launch {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.tag("Playlist").e(e, what)
            }
        }
    }

    /**
     * Nuova playlist. Con [onYouTube] (account collegato) viene creata anche su YouTube Music,
     * così compare anche nelle altre app; altrimenti resta solo sul telefono.
     */
    fun createPlaylist(
        name: String,
        onYouTube: Boolean,
        firstSong: MediaMetadata? = null,
    ) = launchSafely(tr("Creazione playlist non riuscita")) {
        val browseId = if (onYouTube) YouTube.createPlaylist(name).getOrNull() else null
        val entity = PlaylistEntity(name = name, browseId = browseId, isEditable = true)
        database.insert(entity)
        if (firstSong != null) addToPlaylistNow(entity.id, firstSong)
    }

    /** Salva un mix come playlist (anche su YouTube Music se l'account è collegato). */
    fun saveAsPlaylist(
        name: String,
        songs: List<MediaMetadata>,
    ) = launchSafely(tr("Creazione playlist non riuscita")) {
        val browseId = YouTube.createPlaylist(name).getOrNull()
        val entity = PlaylistEntity(name = name, browseId = browseId, isEditable = true)
        database.insert(entity)
        songs.forEach { database.insert(it) }
        val playlist = database.playlist(entity.id).first() ?: return@launchSafely
        database.addSongToPlaylist(playlist, songs.map { it.id })
        if (browseId != null) {
            YouTube.addSongsToPlaylist(browseId, songs.map { it.id })
            // Ordine e id delle voci di YouTube, per poterla poi modificare
            refreshOwnPlaylist(entity)
        }
    }

    fun addToPlaylist(
        playlistId: String,
        song: MediaMetadata,
    ) = launchSafely(tr("Aggiunta alla playlist non riuscita")) { addToPlaylistNow(playlistId, song) }

    private suspend fun addToPlaylistNow(
        playlistId: String,
        song: MediaMetadata,
    ) {
        database.insert(song)
        val playlist = database.playlist(playlistId).first() ?: return
        database.addSongToPlaylist(playlist, listOf(song.id))
        val browseId = playlist.playlist.browseId
        if (browseId != null && playlist.playlist.isEditable) {
            // YouTube restituisce l'id della voce: serve per poterla poi togliere o spostare anche lì
            val setVideoId = YouTube.addToPlaylist(browseId, song.id).getOrThrow()
            database
                .getPlaylistSongs(playlistId)
                .lastOrNull { it.map.songId == song.id && it.map.setVideoId == null }
                ?.let { database.update(it.map.copy(setVideoId = setVideoId)) }
        }
    }

    fun removeFromPlaylist(
        playlist: PlaylistEntity,
        map: PlaylistSongMap,
    ) = launchSafely(tr("Rimozione dalla playlist non riuscita")) {
        database.delete(map)
        val browseId = playlist.browseId
        val setVideoId = map.setVideoId
        if (browseId != null && playlist.isEditable && setVideoId != null) {
            YouTube.removeFromPlaylist(browseId, map.songId, setVideoId)
        }
    }

    fun renamePlaylist(
        playlist: PlaylistEntity,
        name: String,
    ) = launchSafely(tr("Rinomina playlist non riuscita")) {
        database.update(playlist.copy(name = name))
        val browseId = playlist.browseId
        if (browseId != null && playlist.isEditable) YouTube.renamePlaylist(browseId, name)
    }

    /**
     * Elimina la playlist. Se è tua e collegata a YouTube Music, viene eliminata anche lì;
     * se è una playlist di altri salvata in libreria, viene solo tolta dalla libreria.
     */
    fun deletePlaylist(playlist: PlaylistEntity) =
        launchSafely(tr("Eliminazione playlist non riuscita")) {
            val browseId = playlist.browseId
            if (browseId != null && playlist.isEditable) {
                deletedPlaylistIds += browseId
                YouTube.deletePlaylist(browseId).onFailure { deletedPlaylistIds -= browseId }.getOrThrow()
            }
            database.delete(playlist)
        }

    /**
     * Sposta un brano nella playlist (posizioni dell'elenco ordinato). Se la playlist è su YouTube Music,
     * lo sposta anche lì: [movedSetVideoId] è il brano spostato, [successorSetVideoId] quello che gli starà dopo.
     */
    fun moveInPlaylist(
        playlist: PlaylistEntity,
        from: Int,
        to: Int,
        movedSetVideoId: String?,
        successorSetVideoId: String?,
    ) = launchSafely(tr("Spostamento non riuscito")) {
        database.withTransaction {
            // Dopo le rimozioni le posizioni possono avere dei buchi: prima le rinumera 0, 1, 2…
            getPlaylistSongs(playlist.id).forEachIndexed { index, entry ->
                if (entry.map.position != index) update(entry.map.copy(position = index))
            }
            move(playlist.id, from, to)
        }
        val browseId = playlist.browseId
        if (browseId != null && playlist.isEditable && movedSetVideoId != null) {
            YouTube.moveSongPlaylist(browseId, movedSetVideoId, successorSetVideoId).getOrThrow()
        }
    }

    /** Salva in libreria una playlist di YouTube Music, con i suoi brani. */
    fun saveYouTubePlaylist(
        item: PlaylistItem,
        songs: List<MediaMetadata>,
    ) = launchSafely(tr("Salvataggio playlist non riuscito")) {
        if (database.playlistByBrowseId(item.id).first() != null) return@launchSafely
        val entity =
            PlaylistEntity(
                name = item.title,
                browseId = item.id,
                isEditable = item.isEditable,
                thumbnailUrl = item.thumbnail,
                remoteSongCount = songs.size,
                bookmarkedAt = LocalDateTime.now(),
            )
        database.insert(entity)
        songs.forEach { database.insert(it) }
        val playlist = database.playlist(entity.id).first() ?: return@launchSafely
        database.addSongToPlaylist(playlist, songs.map { it.id })
    }

    /** Scarica tutti i brani di una playlist di YouTube Music (a pagine, fino a un limite). */
    suspend fun fetchFullPlaylist(browseId: String): Result<Pair<PlaylistItem, List<SongItem>>> =
        YouTube.playlist(browseId).mapCatching { first ->
            val songs = first.songs.toMutableList()
            var continuation = first.songsContinuation ?: first.continuation
            var pages = 0
            while (continuation != null && pages < 20) {
                val next = YouTube.playlistContinuation(continuation, browseId).getOrNull() ?: break
                songs += next.songs
                continuation = next.continuation
                pages++
            }
            first.playlist to songs.distinctByPlaylistEntry()
        }

    /**
     * Copia nel telefono una TUA playlist di YouTube Music, con l'ordine e gli id delle voci,
     * così si può modificare e ogni modifica va anche su YouTube Music. Restituisce l'id locale.
     */
    suspend fun syncOwnPlaylist(
        item: PlaylistItem,
        songs: List<SongItem>,
    ): String {
        val existing = database.playlistByBrowseId(item.id).first()?.playlist
        val entity =
            existing?.copy(isEditable = true, remoteSongCount = songs.size)
                ?: PlaylistEntity(
                    name = item.title,
                    browseId = item.id,
                    isEditable = true,
                    thumbnailUrl = item.thumbnail,
                    remoteSongCount = songs.size,
                    bookmarkedAt = LocalDateTime.now(),
                )
        if (existing == null) database.insert(entity) else database.update(entity)
        val metadata = songs.map { it.toMediaMetadata() }
        metadata.forEach { database.insert(it) }
        database.withTransaction {
            clearPlaylist(entity.id)
            songs.forEachIndexed { index, song ->
                insert(PlaylistSongMap(playlistId = entity.id, songId = song.id, position = index, setVideoId = song.setVideoId))
            }
        }
        return entity.id
    }

    /** Riallinea una tua playlist sincronizzata con quello che c'è su YouTube Music. */
    suspend fun refreshOwnPlaylist(playlist: PlaylistEntity) {
        val browseId = playlist.browseId ?: return
        if (!playlist.isEditable) return
        val (item, songs) = fetchFullPlaylist(browseId).getOrNull() ?: return
        syncOwnPlaylist(item, songs)
    }

    fun unsaveYouTubePlaylist(browseId: String) =
        launchSafely(tr("Rimozione playlist non riuscita")) {
            database.playlistByBrowseId(browseId).first()?.let { database.delete(it.playlist) }
        }

    // endregion
}

val LocalActions = staticCompositionLocalOf<AtxActions> { error("AtxActions non disponibile") }

/** Cosa succede toccando un elemento di YouTube Music: brani ed episodi partono, il resto si apre. */
fun openItem(
    item: moe.rukamori.archivetune.innertube.models.YTItem,
    actions: AtxActions,
    navigator: AtxNavigator,
) {
    when (item) {
        is SongItem -> actions.playSong(item)
        is moe.rukamori.archivetune.innertube.models.AlbumItem -> navigator.open(Route.Album(item.browseId))
        is moe.rukamori.archivetune.innertube.models.ArtistItem -> navigator.open(Route.Artist(item.id))
        is moe.rukamori.archivetune.innertube.models.PlaylistItem -> navigator.open(Route.Playlist(item.id))
        is moe.rukamori.archivetune.innertube.models.PodcastItem -> item.playlistId?.let { navigator.open(Route.Playlist(it)) }
        is moe.rukamori.archivetune.innertube.models.EpisodeItem -> actions.playEndpoint(item.endpoint)
    }
}
