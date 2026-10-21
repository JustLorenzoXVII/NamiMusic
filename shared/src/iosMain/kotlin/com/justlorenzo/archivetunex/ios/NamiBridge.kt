package com.justlorenzo.archivetunex.ios

import androidx.datastore.preferences.core.edit
import com.justlorenzo.archivetunex.ArchiveTuneXRuntime
import com.justlorenzo.archivetunex.ui.AtxActions
import com.justlorenzo.archivetunex.ui.LiquidGlassKey
import kotlinx.coroutines.CoroutineScope
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
    val explicit: Boolean,
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
)

/**
 * Ponte tra la parte Kotlin di Nami (riproduzione, YouTube Music, database) e l'interfaccia
 * SwiftUI di iPhone (iosApp/Nami/…). Swift lo usa come `NamiBridge.shared`.
 * Le funzioni "observe…" chiamano subito il listener con il valore attuale e poi a ogni cambio,
 * sempre sul thread principale; restituiscono un [NamiSubscription] da cancellare.
 */
object NamiBridge {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var actions: AtxActions? = null

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
        }
    }

    private val connection get() = ArchiveTuneXRuntime.playerConnection
    private val database get() = ArchiveTuneXRuntime.database

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

    private fun <T> collect(
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

private fun MediaMetadata.toNamiTrack(): NamiTrack =
    NamiTrack(
        id = id,
        title = title,
        artist = artists.joinToString(", ") { it.name },
        artworkUrl = thumbnailUrl?.resize(240, 240),
        largeArtworkUrl = thumbnailUrl?.resize(1080, 1080),
        durationMs = duration * 1000L,
        explicit = explicit,
    )
