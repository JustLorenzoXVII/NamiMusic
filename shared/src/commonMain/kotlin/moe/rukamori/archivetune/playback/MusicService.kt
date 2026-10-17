/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.rukamori.archivetune.playback

import com.justlorenzo.archivetunex.player.QueuePlayer
import com.justlorenzo.archivetunex.player.ResolvedMedia
import com.justlorenzo.archivetunex.player.StreamResolver
import com.justlorenzo.archivetunex.player.createAudioEngine
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.map
import moe.rukamori.archivetune.constants.ShowLyricsKey
import moe.rukamori.archivetune.lyrics.LyricsHelper
import org.koin.mp.KoinPlatform
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import moe.rukamori.archivetune.compat.AppContext
import moe.rukamori.archivetune.compat.System
import moe.rukamori.archivetune.compat.Timber
import moe.rukamori.archivetune.compat.media3.C
import moe.rukamori.archivetune.compat.media3.MediaItem
import moe.rukamori.archivetune.compat.media3.Player
import moe.rukamori.archivetune.compat.media3.Player.Companion.REPEAT_MODE_OFF
import moe.rukamori.archivetune.compat.media3.Player.Companion.STATE_IDLE
import moe.rukamori.archivetune.compat.media3.ShuffleOrder.DefaultShuffleOrder
import moe.rukamori.archivetune.compat.time.LocalDateTime
import moe.rukamori.archivetune.constants.AutoLoadMoreKey
import moe.rukamori.archivetune.constants.HISTORY_DURATION_DEFAULT
import moe.rukamori.archivetune.constants.HISTORY_DURATION_MAX
import moe.rukamori.archivetune.constants.HISTORY_DURATION_MIN
import moe.rukamori.archivetune.constants.HideExplicitKey
import moe.rukamori.archivetune.constants.HideVideoKey
import moe.rukamori.archivetune.constants.HistoryDuration
import moe.rukamori.archivetune.constants.PauseListenHistoryKey
import moe.rukamori.archivetune.constants.PermanentShuffleKey
import moe.rukamori.archivetune.db.MusicDatabase
import moe.rukamori.archivetune.db.entities.Event
import moe.rukamori.archivetune.extensions.currentMetadata
import moe.rukamori.archivetune.extensions.getQueueWindows
import moe.rukamori.archivetune.extensions.metadata
import moe.rukamori.archivetune.extensions.move
import moe.rukamori.archivetune.extensions.toMediaItem
import moe.rukamori.archivetune.innertube.models.WatchEndpoint
import moe.rukamori.archivetune.models.MediaMetadata
import moe.rukamori.archivetune.playback.queues.EmptyQueue
import moe.rukamori.archivetune.playback.queues.ListQueue
import moe.rukamori.archivetune.playback.queues.Queue
import moe.rukamori.archivetune.playback.queues.YouTubeQueue
import moe.rukamori.archivetune.playback.queues.filterBlockedArtists
import moe.rukamori.archivetune.playback.queues.filterExplicit
import moe.rukamori.archivetune.playback.queues.filterVideo
import moe.rukamori.archivetune.playback.queues.hasBlockedArtist
import moe.rukamori.archivetune.utils.dataStore
import moe.rukamori.archivetune.utils.get
import moe.rukamori.archivetune.utils.isLocalMediaId
import moe.rukamori.archivetune.utils.reportException

/**
 * Il servizio di riproduzione di ArchiveTune, portato in codice comune.
 *
 * Nell'originale è un MediaLibraryService Android di ~8700 righe; qui restano la logica della coda
 * (playQueue, riproduci dopo, aggiungi in coda, radio, coda infinita), la cronologia d'ascolto,
 * il "mi piace" e il timer di spegnimento, con gli stessi nomi così PlayerConnection e le
 * schermate restano uguali. Notifica e schermata di blocco le gestisce il motore della piattaforma.
 */
class MusicService(
    val database: MusicDatabase,
    val downloadUtil: DownloadUtil,
) : Player.Listener {
    private val dataStore = AppContext.dataStore
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val ioScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val silentHandler = CoroutineExceptionHandler { _, throwable -> reportException(throwable) }

    /** Il lettore: implementazione comune dell'interfaccia Player di Media3. */
    val localPlayer: QueuePlayer = QueuePlayer(createAudioEngine(), ::resolveMedia)
    val player: Player = localPlayer

    val sleepTimer = SleepTimer(scope, player, this)

    val currentMediaMetadata = MutableStateFlow<MediaMetadata?>(null)
    val queueRestoreCompleted = MutableStateFlow(true)
    val infiniteQueueLoading = MutableStateFlow(false)
    val waitingForNetworkConnection = MutableStateFlow(false)
    val playerVolume = MutableStateFlow(1f)

    private var currentQueue: Queue = EmptyQueue
    var queueTitle: String? = null
    private var blockedArtistIds: Set<String> = emptySet()
    private var hideMusicVideos = false
    private var suppressAutoPlayback = false
    private var infiniteQueueJob: Job? = null
    private var infiniteQueueGeneration = 0L
    private val toggleLikeMutex = Mutex()
    val autoAddedMediaIds: MutableSet<String> = mutableSetOf()

    init {
        player.addListener(this)
        player.addListener(sleepTimer)
        ioScope.launch {
            database.blockedArtistIds().collect { ids -> blockedArtistIds = ids.toSet() }
        }
        ioScope.launch {
            dataStore.data.collect { prefs -> hideMusicVideos = prefs[HideVideoKey] ?: false }
        }
        // Come l'originale: con "Mostra testi" attivo, il testo del brano si scarica appena cambia
        // il brano e si salva nel database, così la schermata dei testi lo trova già pronto.
        ioScope.launch {
            combine(
                currentMediaMetadata.distinctUntilChangedBy { it?.id },
                dataStore.data.map { it[ShowLyricsKey] ?: false }.distinctUntilChanged(),
            ) { mediaMetadata, showLyrics ->
                mediaMetadata to showLyrics
            }.collectLatest { (mediaMetadata, showLyrics) ->
                if (showLyrics && mediaMetadata != null && !mediaMetadata.isPodcast &&
                    database.lyrics(mediaMetadata.id).first() == null
                ) {
                    try {
                        val lyrics = KoinPlatform.getKoin().get<LyricsHelper>().getLyrics(mediaMetadata)
                        database.query {
                            insertLyricsIfAbsent(
                                id = mediaMetadata.id,
                                lyrics = lyrics,
                            )
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Timber.tag("MusicService").w(e, "Download dei testi non riuscito")
                    }
                }
            }
        }
    }

    // region Stream

    private suspend fun resolveMedia(item: MediaItem): ResolvedMedia {
        val mediaId = item.mediaId
        // Brano scaricato: si riproduce il file, senza rete.
        downloadUtil.fileFor(mediaId)?.let { file ->
            return ResolvedMedia(
                url = "file://" + file.absolutePath,
                durationMs = item.metadata?.duration?.takeIf { it > 0 }?.let { it * 1000L },
            )
        }
        val stream = withContext(Dispatchers.Default) { StreamResolver.resolve(mediaId) }.getOrThrow()
        ioScope.launch { persistPlaybackMetadata(database, mediaId, stream) }
        return ResolvedMedia(
            url = stream.url,
            headers = stream.requestHeaders,
            durationMs = (stream.durationSeconds ?: item.metadata?.duration?.takeIf { it > 0 })?.let { it * 1000L },
        )
    }

    // endregion

    // region Coda

    private suspend fun Queue.Status.filterPlaybackContent(
        hideExplicit: Boolean,
        hideVideo: Boolean,
    ): Queue.Status =
        filterExplicit(hideExplicit)
            .filterVideo(hideVideo)
            .filterBlockedArtists(loadBlockedArtistIds())

    private suspend fun List<MediaItem>.filterPlaybackContent(
        hideExplicit: Boolean,
        hideVideo: Boolean,
    ): List<MediaItem> =
        filterExplicit(hideExplicit)
            .filterVideo(hideVideo)
            .filterBlockedArtists(loadBlockedArtistIds())

    private suspend fun loadBlockedArtistIds(): Set<String> =
        withContext(Dispatchers.IO) { database.blockedArtistIds().first().toSet() }.also { blockedArtistIds = it }

    fun playQueue(
        queue: Queue,
        playWhenReady: Boolean = true,
    ) {
        cancelInfiniteQueueBootstrap()
        suppressAutoPlayback = false
        currentQueue = queue
        queueTitle = null
        val permanentShuffle = dataStore.get(PermanentShuffleKey, false)
        if (!permanentShuffle) {
            player.shuffleModeEnabled = false
        }

        clearAutomix()
        scope.launch(silentHandler) {
            val hideExplicit = dataStore.get(HideExplicitKey, false)
            val hideVideo = dataStore.get(HideVideoKey, false)
            val autoLoadMoreEnabled = dataStore.get(AutoLoadMoreKey, true)
            val preloadItem =
                queue.preloadItem
                    ?.toMediaItem()
                    ?.takeUnless { item ->
                        item.hasBlockedArtist(loadBlockedArtistIds())
                    }
            if (preloadItem != null) {
                player.setMediaItem(preloadItem)
                player.prepare()
                player.playWhenReady = playWhenReady
            }
            var initialStatus =
                withContext(Dispatchers.IO) {
                    queue
                        .getInitialStatus()
                        .filterPlaybackContent(hideExplicit, hideVideo)
                }
            if (!autoLoadMoreEnabled && queue.shouldExpandToFullQueueWhenAutoLoadMoreDisabled() && queue.hasNextPage()) {
                val expandedItems = initialStatus.items.toMutableList()
                var pagesLoaded = 0
                while (queue.hasNextPage() && pagesLoaded < 200) {
                    pagesLoaded++
                    val nextItems =
                        withContext(Dispatchers.IO) {
                            queue
                                .nextPage()
                                .filterPlaybackContent(hideExplicit, hideVideo)
                        }
                    if (nextItems.isNotEmpty()) {
                        expandedItems += nextItems
                    }
                }
                initialStatus = initialStatus.copy(items = expandedItems)
            }
            if (initialStatus.title != null) {
                queueTitle = initialStatus.title
            }
            if (initialStatus.items.isEmpty()) return@launch
            if (preloadItem != null) {
                val preloadMediaId = preloadItem.mediaId.trim()
                val insertionIndex =
                    initialStatus.mediaItemIndex.coerceIn(0, initialStatus.items.size)
                val itemsBeforeCurrent =
                    initialStatus.items
                        .subList(0, insertionIndex)
                        .filterNot { preloadMediaId.isNotEmpty() && it.mediaId.trim() == preloadMediaId }
                val itemsAfterCurrent =
                    initialStatus.items
                        .subList(insertionIndex, initialStatus.items.size)
                        .filterNot { preloadMediaId.isNotEmpty() && it.mediaId.trim() == preloadMediaId }

                player.addMediaItems(0, itemsBeforeCurrent)
                player.addMediaItems(itemsAfterCurrent)
                if (player.shuffleModeEnabled) {
                    applyCurrentFirstShuffleOrder()
                }
            } else {
                val items = initialStatus.items
                val index = initialStatus.mediaItemIndex

                player.setMediaItems(items, index, initialStatus.position)
                player.prepare()
                player.playWhenReady = playWhenReady
                if (player.shuffleModeEnabled) {
                    applyCurrentFirstShuffleOrder()
                }
            }

            if (
                autoLoadMoreEnabled &&
                player.currentMetadata?.isPodcast != true &&
                !queue.hasNextPage() &&
                player.mediaItemCount - player.currentMediaItemIndex <= 3
            ) {
                onInfiniteQueueEnabled()
            }
        }
    }

    private fun applyCurrentFirstShuffleOrder() {
        val count = player.mediaItemCount
        if (count <= 1) return
        val currentIndex = player.currentMediaItemIndex.coerceIn(0, count - 1)
        val shuffledIndices = IntArray(count) { it }
        shuffledIndices.shuffle()
        val currentPos = shuffledIndices.indexOf(currentIndex)
        if (currentPos >= 0) {
            shuffledIndices[currentPos] = shuffledIndices[0]
        }
        shuffledIndices[0] = currentIndex
        localPlayer.setShuffleOrder(DefaultShuffleOrder(shuffledIndices, System.currentTimeMillis()))
    }

    private fun buildPlayNextShuffleOrder(
        currentIndex: Int,
        insertionIndex: Int,
        insertionCount: Int,
    ): DefaultShuffleOrder? {
        if (insertionCount <= 0 || player.currentTimeline.isEmpty) return null

        fun adjustedIndex(index: Int): Int =
            if (index >= insertionIndex) {
                index + insertionCount
            } else {
                index
            }

        val timeline = player.currentTimeline
        val previousIndices = ArrayDeque<Int>()
        var traversalIndex = currentIndex
        while (true) {
            traversalIndex = timeline.getPreviousWindowIndex(traversalIndex, REPEAT_MODE_OFF, true)
            if (traversalIndex == C.INDEX_UNSET) {
                break
            }
            previousIndices.addFirst(adjustedIndex(traversalIndex))
        }

        val nextIndices = mutableListOf<Int>()
        traversalIndex = currentIndex
        while (true) {
            traversalIndex = timeline.getNextWindowIndex(traversalIndex, REPEAT_MODE_OFF, true)
            if (traversalIndex == C.INDEX_UNSET) {
                break
            }
            nextIndices += adjustedIndex(traversalIndex)
        }

        val shuffledIndices =
            buildList(player.mediaItemCount + insertionCount) {
                addAll(previousIndices)
                add(adjustedIndex(currentIndex))
                repeat(insertionCount) { offset ->
                    add(insertionIndex + offset)
                }
                addAll(nextIndices)
            }.toIntArray()

        return DefaultShuffleOrder(shuffledIndices, System.currentTimeMillis())
    }

    fun startRadioSeamlessly() {
        cancelInfiniteQueueBootstrap()
        suppressAutoPlayback = false
        val currentMediaMetadata = player.currentMetadata ?: return

        val currentIndex = player.currentMediaItemIndex
        val currentMediaId = currentMediaMetadata.id
        if (currentMediaId.isLocalMediaId()) {
            return
        }

        scope.launch(silentHandler) {
            val radioQueue =
                YouTubeQueue(
                    endpoint = WatchEndpoint(videoId = currentMediaId),
                    followAutomixPreview = true,
                )
            val initialStatus =
                withContext(Dispatchers.IO) {
                    radioQueue
                        .getInitialStatus()
                        .filterPlaybackContent(
                            hideExplicit = dataStore.get(HideExplicitKey, false),
                            hideVideo = dataStore.get(HideVideoKey, false),
                        )
                }

            if (initialStatus.title != null) {
                queueTitle = initialStatus.title
            }

            val radioItems =
                initialStatus.items.filter { item ->
                    item.mediaId != currentMediaId
                }

            if (radioItems.isNotEmpty()) {
                val itemCount = player.mediaItemCount

                if (itemCount > currentIndex + 1) {
                    player.removeMediaItems(currentIndex + 1, itemCount)
                }

                player.addMediaItems(currentIndex + 1, radioItems)
            }

            currentQueue = radioQueue
        }
    }

    fun toggleStartRadio() {
        startRadioSeamlessly()
    }

    fun clearAutomix() {
        autoAddedMediaIds.clear()
    }

    fun onInfiniteQueueDisabled() {
        cancelInfiniteQueueBootstrap()
        val currentIndex = player.currentMediaItemIndex
        val idsToRemove = autoAddedMediaIds.toSet()
        if (idsToRemove.isEmpty()) {
            return
        }
        for (i in player.mediaItemCount - 1 downTo 0) {
            if (i == currentIndex) continue
            val item = player.getMediaItemAt(i)
            if (item.mediaId in idsToRemove) {
                player.removeMediaItem(i)
            }
        }
        autoAddedMediaIds.clear()
        currentQueue = EmptyQueue
    }

    fun onInfiniteQueueEnabled() {
        val currentMeta = player.currentMetadata ?: return
        if (currentMeta.isPodcast) return
        if (currentMeta.id.trim().isLocalMediaId()) return
        if (infiniteQueueJob?.isActive == true) return

        val seedMediaId = currentMeta.id.trim().ifBlank { return }
        val generation = ++infiniteQueueGeneration
        infiniteQueueLoading.value = true

        infiniteQueueJob =
            scope.launch(silentHandler) {
                try {
                    val hideExplicit = dataStore.get(HideExplicitKey, false)
                    val hideVideo = dataStore.get(HideVideoKey, false)
                    val radioQueue = YouTubeQueue(WatchEndpoint(videoId = seedMediaId), followAutomixPreview = true)
                    val status =
                        withContext(Dispatchers.IO) {
                            radioQueue
                                .getInitialStatus()
                                .filterPlaybackContent(hideExplicit, hideVideo)
                        }
                    val knownIds =
                        (0 until player.mediaItemCount)
                            .mapTo(mutableSetOf()) { player.getMediaItemAt(it).mediaId }
                    val newItems = status.items.filter { knownIds.add(it.mediaId) }.toMutableList()
                    var loadedPageCount = 1

                    while (
                        newItems.isEmpty() &&
                        radioQueue.hasNextPage() &&
                        loadedPageCount < INFINITE_QUEUE_MAX_BOOTSTRAP_PAGES
                    ) {
                        loadedPageCount++
                        val page =
                            withContext(Dispatchers.IO) {
                                radioQueue
                                    .nextPage()
                                    .filterPlaybackContent(hideExplicit, hideVideo)
                            }
                        newItems += page.filter { knownIds.add(it.mediaId) }
                    }

                    if (generation != infiniteQueueGeneration) return@launch

                    if (newItems.isNotEmpty()) {
                        player.addMediaItems(newItems)
                        newItems.forEach { autoAddedMediaIds.add(it.mediaId) }
                    }

                    currentQueue = radioQueue

                    if (player.playbackState == Player.STATE_ENDED ||
                        player.mediaItemCount == player.currentMediaItemIndex + 1
                    ) {
                        if (player.playbackState == Player.STATE_ENDED) {
                            player.seekToNext()
                            player.play()
                        }
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Timber.e(e, "Failed to bootstrap auto-queue")
                } finally {
                    if (generation == infiniteQueueGeneration) {
                        infiniteQueueJob = null
                        infiniteQueueLoading.value = false
                    }
                }
            }
    }

    private fun cancelInfiniteQueueBootstrap() {
        infiniteQueueGeneration++
        infiniteQueueJob?.cancel()
        infiniteQueueJob = null
        infiniteQueueLoading.value = false
    }

    fun stopAndClearPlayback(clearPersistentState: Boolean = false) {
        cancelInfiniteQueueBootstrap()
        suppressAutoPlayback = true
        clearAutomix()
        currentQueue = EmptyQueue
        queueTitle = null
        waitingForNetworkConnection.value = false
        currentMediaMetadata.value = null
        player.playWhenReady = false
        player.stop()
        player.clearMediaItems()
    }

    fun playNext(items: List<MediaItem>) {
        val allowedItems =
            items
                .filterBlockedArtists(blockedArtistIds)
                .filterVideo(hideMusicVideos)
        if (allowedItems.isEmpty()) return
        suppressAutoPlayback = false
        val insertionIndex = if (player.mediaItemCount == 0) 0 else player.currentMediaItemIndex + 1
        val playNextShuffleOrder =
            if (player.shuffleModeEnabled && player.mediaItemCount > 0) {
                buildPlayNextShuffleOrder(
                    currentIndex = player.currentMediaItemIndex,
                    insertionIndex = insertionIndex,
                    insertionCount = allowedItems.size,
                )
            } else {
                null
            }

        player.addMediaItems(insertionIndex, allowedItems)
        playNextShuffleOrder?.let(localPlayer::setShuffleOrder)
        player.prepare()
    }

    fun moveQueueItemToNext(mediaItemIndex: Int) {
        val currentIndex = player.currentMediaItemIndex
        if (
            player.mediaItemCount < 2 ||
            currentIndex == C.INDEX_UNSET ||
            mediaItemIndex !in 0 until player.mediaItemCount ||
            mediaItemIndex == currentIndex
        ) {
            return
        }

        if (player.shuffleModeEnabled) {
            val shuffledIndices = player.getQueueWindows().map { it.firstPeriodIndex }.toMutableList()
            val sourcePosition = shuffledIndices.indexOf(mediaItemIndex)
            val currentPosition = shuffledIndices.indexOf(currentIndex)
            if (sourcePosition == -1 || currentPosition == -1) return

            val destinationPosition = (currentPosition + 1).coerceAtMost(shuffledIndices.lastIndex)
            if (sourcePosition == destinationPosition) return

            shuffledIndices.move(sourcePosition, destinationPosition)
            localPlayer.setShuffleOrder(
                DefaultShuffleOrder(
                    shuffledIndices.toIntArray(),
                    System.currentTimeMillis(),
                ),
            )
            return
        }

        val destinationIndex = (currentIndex + 1).coerceAtMost(player.mediaItemCount - 1)
        if (mediaItemIndex != destinationIndex) {
            player.moveMediaItem(mediaItemIndex, destinationIndex)
        }
    }

    fun addToQueue(items: List<MediaItem>) {
        val allowedItems =
            items
                .filterBlockedArtists(blockedArtistIds)
                .filterVideo(hideMusicVideos)
        if (allowedItems.isEmpty()) return
        suppressAutoPlayback = false
        player.addMediaItems(allowedItems)
        player.prepare()
    }

    fun playFromVoiceSearch(query: String) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return
        scope.launch(silentHandler) {
            val songs =
                withContext(Dispatchers.IO) {
                    moe.rukamori.archivetune.innertube.YouTube
                        .search(trimmed, moe.rukamori.archivetune.innertube.YouTube.SearchFilter.FILTER_SONG)
                        .getOrNull()
                        ?.items
                        ?.filterIsInstance<moe.rukamori.archivetune.innertube.models.SongItem>()
                        .orEmpty()
                }
            if (songs.isEmpty()) return@launch
            playQueue(ListQueue(items = songs.map { it.toMediaItem() }))
        }
    }

    fun pauseFromSleepTimer() {
        sleepTimer.clear()
        player.pause()
        player.playWhenReady = false
    }

    // endregion

    // region Mi piace

    fun toggleLike() {
        val mediaMetadata = currentMediaMetadata.value ?: return
        ioScope.launch {
            try {
                toggleLikeMutex.withLock {
                    database.withTransaction {
                        val currentSongEntity =
                            getSongById(mediaMetadata.id)
                                ?: run {
                                    insert(mediaMetadata) {
                                        it.copy(isLocal = mediaMetadata.id.isLocalMediaId())
                                    }
                                    getSongById(mediaMetadata.id)
                                }
                                ?: return@withTransaction
                        update(currentSongEntity.song.toggleLike())
                    }
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                reportException(error)
            }
        }
    }

    // endregion

    // region Eventi del lettore

    override fun onMediaItemTransition(
        mediaItem: MediaItem?,
        reason: Int,
    ) {
        if (sleepTimer.pauseWhenSongEnd) {
            pauseFromSleepTimer()
            return
        }

        beginHistorySession(mediaItem?.mediaId, forceNew = true)

        val timelineEmpty = player.currentTimeline.isEmpty || player.mediaItemCount == 0 || player.currentMediaItem == null
        currentMediaMetadata.value = if (timelineEmpty) null else (mediaItem?.metadata ?: player.currentMetadata)

        // Carica altri brani dalla coda quando sta per finire
        if (!suppressAutoPlayback &&
            !timelineEmpty &&
            currentMediaMetadata.value?.isPodcast != true &&
            dataStore.get(AutoLoadMoreKey, true) &&
            reason != Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT &&
            player.mediaItemCount - player.currentMediaItemIndex <= 5 &&
            currentQueue.hasNextPage() &&
            player.repeatMode == REPEAT_MODE_OFF
        ) {
            scope.launch(silentHandler) {
                val mediaItems =
                    currentQueue
                        .nextPage()
                        .filterPlaybackContent(
                            hideExplicit = dataStore.get(HideExplicitKey, false),
                            hideVideo = dataStore.get(HideVideoKey, false),
                        )
                if (player.playbackState != STATE_IDLE) {
                    player.addMediaItems(mediaItems.drop(1))
                }
            }
        }

        if (!suppressAutoPlayback &&
            !timelineEmpty &&
            dataStore.get(AutoLoadMoreKey, true) &&
            reason != Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT &&
            player.repeatMode == REPEAT_MODE_OFF &&
            player.mediaItemCount - player.currentMediaItemIndex <= 3 &&
            !currentQueue.hasNextPage()
        ) {
            onInfiniteQueueEnabled()
        }
    }

    override fun onEvents(
        player: Player,
        events: Player.Events,
    ) {
        val currentMediaId = player.currentMediaItem?.mediaId
        if (currentMediaId == null && currentHistoryMediaId != null) {
            beginHistorySession(null, forceNew = true)
        } else if (currentHistoryMediaId == null && currentMediaId != null) {
            beginHistorySession(currentMediaId)
        }
        if (events.containsAny(
                Player.EVENT_PLAYBACK_STATE_CHANGED,
                Player.EVENT_PLAY_WHEN_READY_CHANGED,
                Player.EVENT_IS_PLAYING_CHANGED,
            )
        ) {
            updateHistoryTrackingPlaybackState()
        }
        if (events.contains(Player.EVENT_TIMELINE_CHANGED) && player.mediaItemCount > 0 && currentMediaMetadata.value == null) {
            currentMediaMetadata.value = player.currentMetadata
        }
    }

    // endregion

    // region Cronologia d'ascolto (come l'originale: un evento dopo "HistoryDuration" secondi di ascolto)

    private var currentHistoryMediaId: String? = null
    private var currentHistoryAccumulatedPlayMs = 0L
    private var currentHistoryStartedAtMs: Long? = null
    private var currentHistoryRecorded = false
    private var historyThresholdJob: Job? = null

    private fun historyThresholdMs(): Long =
        (runCatching { dataStore.get(HistoryDuration, HISTORY_DURATION_DEFAULT) }.getOrNull() ?: HISTORY_DURATION_DEFAULT)
            .coerceIn(HISTORY_DURATION_MIN, HISTORY_DURATION_MAX)
            .toLong() * 1000L

    private fun currentHistoryPlayedMs(now: Long = System.currentTimeMillis()): Long =
        currentHistoryAccumulatedPlayMs + (currentHistoryStartedAtMs?.let { (now - it).coerceAtLeast(0L) } ?: 0L)

    private fun beginHistorySession(
        mediaId: String?,
        forceNew: Boolean = false,
    ) {
        val normalizedMediaId = mediaId?.trim()?.takeIf { it.isNotEmpty() }
        if (!forceNew && currentHistoryMediaId == normalizedMediaId) {
            updateHistoryTrackingPlaybackState()
            return
        }
        historyThresholdJob?.cancel()
        historyThresholdJob = null
        currentHistoryMediaId = normalizedMediaId
        currentHistoryAccumulatedPlayMs = 0L
        currentHistoryStartedAtMs = null
        currentHistoryRecorded = false
        updateHistoryTrackingPlaybackState()
    }

    private fun updateHistoryTrackingPlaybackState() {
        if (currentHistoryMediaId == null || currentHistoryRecorded) {
            historyThresholdJob?.cancel()
            historyThresholdJob = null
            currentHistoryStartedAtMs = null
            return
        }
        if (player.isPlaying) {
            if (currentHistoryStartedAtMs == null) currentHistoryStartedAtMs = System.currentTimeMillis()
        } else {
            currentHistoryAccumulatedPlayMs = currentHistoryPlayedMs()
            currentHistoryStartedAtMs = null
        }
        historyThresholdJob?.cancel()
        historyThresholdJob = null
        if (!player.isPlaying) return
        val remaining = historyThresholdMs() - currentHistoryPlayedMs()
        historyThresholdJob =
            scope.launch {
                delay(remaining.coerceAtLeast(0L))
                recordCurrentPlaybackHistory()
            }
    }

    private fun recordCurrentPlaybackHistory() {
        val mediaId = currentHistoryMediaId ?: return
        if (currentHistoryRecorded) return
        if (dataStore.get(PauseListenHistoryKey, false)) return
        currentHistoryRecorded = true
        val playedMs = currentHistoryPlayedMs()
        val metadata = player.currentMetadata?.takeIf { it.id == mediaId }
        ioScope.launch {
            try {
                database.withTransaction {
                    if (song(mediaId).first() == null && metadata != null) {
                        insert(metadata)
                    }
                    insert(
                        Event(
                            songId = mediaId,
                            timestamp = LocalDateTime.now(),
                            playTime = playedMs,
                        ),
                    )
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (throwable: Throwable) {
                reportException(throwable)
            }
        }
    }

    // region Funzioni dell'originale non ancora portate
    //
    // ArchiveTuneX: queste parti di MusicService (Music Together, Discord, equalizzatore di sistema,
    // dispositivo audio, cache Media3) hanno qui la stessa firma dell'originale, così l'interfaccia
    // compila invariata. Per ora non fanno nulla o danno valori neutri; vanno portate una alla volta.

    /** Cache dello streaming: in ArchiveTuneX lo streaming non salva nulla su disco. */
    val playerCache: moe.rukamori.archivetune.compat.media3.Cache = moe.rukamori.archivetune.compat.media3.EmptyCache

    /** Cache dei download (i file scaricati). */
    val downloadCache: moe.rukamori.archivetune.compat.media3.Cache by lazy {
        moe.rukamori.archivetune.playback.DownloadsCache(downloadUtil)
    }

    private val _activeAudioDevice =
        MutableStateFlow(
            moe.rukamori.archivetune.models.ActiveOutputDevice(
                moe.rukamori.archivetune.models.PlayerOutputDevice.Unknown,
                moe.rukamori.archivetune.models.PlayerOutputDevice.Unknown.defaultName,
            ),
        )
    val activeAudioDevice: kotlinx.coroutines.flow.StateFlow<moe.rukamori.archivetune.models.ActiveOutputDevice> =
        _activeAudioDevice

    fun refreshActiveDevice() = Unit

    val eqCapabilities = MutableStateFlow<moe.rukamori.archivetune.playback.EqCapabilities?>(null)

    fun applyEqFlatPreset() = Unit

    fun applySystemEqPreset(presetIndex: Int) = Unit

    fun forceDiscordSync(reason: String) = Unit

    suspend fun refreshDiscordNow(): Boolean = false

    val togetherSessionState =
        MutableStateFlow<moe.rukamori.archivetune.together.TogetherSessionState>(
            moe.rukamori.archivetune.together.TogetherSessionState.Idle,
        )

    fun startTogetherHost(
        port: Int,
        displayName: String,
        settings: moe.rukamori.archivetune.together.TogetherRoomSettings,
    ) = Unit

    fun startTogetherOnlineHost(
        displayName: String,
        settings: moe.rukamori.archivetune.together.TogetherRoomSettings,
    ) = Unit

    fun joinTogether(
        rawLink: String,
        displayName: String,
    ) = Unit

    fun joinTogetherOnline(
        code: String,
        displayName: String,
    ) = Unit

    fun leaveTogether() {
        togetherSessionState.value = moe.rukamori.archivetune.together.TogetherSessionState.Idle
    }

    fun updateTogetherSettings(settings: moe.rukamori.archivetune.together.TogetherRoomSettings) = Unit

    fun approveTogetherParticipant(
        participantId: String,
        approved: Boolean,
    ) = Unit

    fun kickTogetherParticipant(
        participantId: String,
        reason: String? = null,
    ) = Unit

    fun banTogetherParticipant(
        participantId: String,
        reason: String? = null,
    ) = Unit

    fun transferTogetherHostOwnership(participantId: String) = Unit

    fun requestTogetherControl(action: moe.rukamori.archivetune.together.ControlAction) = Unit

    // endregion

    // endregion

    companion object {
        // Nomi dei file della coda salvata, come nell'originale (usati dal backup)
        const val PERSISTENT_QUEUE_FILE = "persistent_queue.data"
        const val PERSISTENT_AUTOMIX_FILE = "persistent_automix.data"
        const val PERSISTENT_PLAYER_STATE_FILE = "persistent_player_state.data"

        private const val INFINITE_QUEUE_MAX_BOOTSTRAP_PAGES = 3
    }
}
