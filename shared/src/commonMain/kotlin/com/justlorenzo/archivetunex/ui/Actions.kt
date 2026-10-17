package com.justlorenzo.archivetunex.ui

import androidx.compose.runtime.staticCompositionLocalOf
import moe.rukamori.archivetune.compat.media3.DownloadRequest
import moe.rukamori.archivetune.compat.toUri
import moe.rukamori.archivetune.db.MusicDatabase
import moe.rukamori.archivetune.extensions.toMediaItem
import moe.rukamori.archivetune.innertube.models.SongItem
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
