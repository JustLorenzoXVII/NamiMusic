package com.justlorenzo.archivetunex.player

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import moe.rukamori.archivetune.innertube.models.SongItem

data class PlayerState(
    val queue: List<SongItem> = emptyList(),
    val index: Int = -1,
    val isPlaying: Boolean = false,
    val isLoading: Boolean = false,
    val positionMs: Long = 0,
    val durationMs: Long = 0,
    val error: String? = null,
) {
    val current: SongItem?
        get() = queue.getOrNull(index)
    val hasNext: Boolean
        get() = index in 0 until queue.lastIndex
}

/**
 * Lettore comune ad Android e iPhone: gestisce la coda, chiede gli URL a YouTube
 * e comanda il motore audio della piattaforma.
 */
class PlayerController(
    private val engine: AudioEngine,
) : AudioEngine.Listener {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val _state = MutableStateFlow(PlayerState())
    val state: StateFlow<PlayerState> = _state.asStateFlow()

    private var loadJob: Job? = null
    private var progressJob: Job? = null

    init {
        engine.listener = this
    }

    /** Riproduce [songs] a partire da [startIndex]. */
    fun playQueue(
        songs: List<SongItem>,
        startIndex: Int,
    ) {
        if (songs.isEmpty()) return
        _state.update { it.copy(queue = songs, index = startIndex.coerceIn(songs.indices), error = null) }
        loadCurrent(playWhenReady = true)
    }

    fun togglePlayPause() {
        if (_state.value.current == null) return
        if (_state.value.isPlaying) engine.pause() else engine.play()
    }

    fun next() {
        val state = _state.value
        if (!state.hasNext) return
        _state.update { it.copy(index = it.index + 1, error = null) }
        loadCurrent(playWhenReady = true)
    }

    fun previous() {
        val state = _state.value
        if (state.current == null) return
        // Come le app musicali: dopo 3 secondi "indietro" riparte dall'inizio del brano.
        if (engine.positionMs > 3_000 || state.index == 0) {
            seekTo(0)
            return
        }
        _state.update { it.copy(index = it.index - 1, error = null) }
        loadCurrent(playWhenReady = true)
    }

    fun seekTo(positionMs: Long) {
        engine.seekTo(positionMs)
        _state.update { it.copy(positionMs = positionMs) }
    }

    private fun loadCurrent(playWhenReady: Boolean) {
        val song = _state.value.current ?: return
        loadJob?.cancel()
        engine.stop()
        _state.update { it.copy(isLoading = true, isPlaying = false, positionMs = 0, durationMs = (song.duration ?: 0) * 1000L) }

        loadJob =
            scope.launch {
                StreamResolver
                    .resolve(song.id)
                    .onSuccess { stream ->
                        val durationMs = stream.durationMs ?: song.duration?.let { it * 1000L }
                        _state.update { it.copy(durationMs = durationMs ?: 0) }
                        engine.load(
                            url = stream.url,
                            headers = mapOf("User-Agent" to stream.userAgent),
                            info =
                                NowPlayingInfo(
                                    title = song.title,
                                    artist = song.artists.joinToString(", ") { it.name },
                                    artworkUrl = song.thumbnail.withThumbnailSize(544),
                                    durationMs = durationMs,
                                ),
                            playWhenReady = playWhenReady,
                        )
                        startProgressUpdates()
                    }.onFailure { error ->
                        _state.update { it.copy(isLoading = false, error = error.message ?: error.toString()) }
                    }
            }
    }

    private fun startProgressUpdates() {
        if (progressJob?.isActive == true) return
        progressJob =
            scope.launch {
                while (isActive) {
                    _state.update { state ->
                        state.copy(
                            positionMs = engine.positionMs,
                            durationMs = engine.durationMs?.takeIf { it > 0 } ?: state.durationMs,
                        )
                    }
                    delay(500)
                }
            }
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        _state.update { it.copy(isPlaying = isPlaying, isLoading = if (isPlaying) false else it.isLoading) }
    }

    override fun onBufferingChanged(isBuffering: Boolean) {
        _state.update { it.copy(isLoading = isBuffering) }
    }

    override fun onEnded() {
        if (_state.value.hasNext) {
            next()
        } else {
            _state.update { it.copy(isPlaying = false, positionMs = it.durationMs) }
        }
    }

    override fun onError(message: String) {
        _state.update { it.copy(isLoading = false, isPlaying = false, error = message) }
    }

    override fun onRemoteNext() = next()

    override fun onRemotePrevious() = previous()
}

/** Il lettore unico dell'app. */
object Player {
    val controller: PlayerController by lazy { PlayerController(createAudioEngine()) }
}

/** Le copertine di YouTube Music arrivano piccole (es. =w60-h60): chiede la versione grande. */
internal fun String.withThumbnailSize(size: Int): String =
    replace(Regex("=w\\d+-h\\d+"), "=w$size-h$size")
