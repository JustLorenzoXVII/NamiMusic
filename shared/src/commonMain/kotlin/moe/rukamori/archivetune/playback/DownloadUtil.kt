/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.rukamori.archivetune.playback

import com.justlorenzo.archivetunex.player.StreamResolver
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsBytes
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import moe.rukamori.archivetune.compat.AppContext
import moe.rukamori.archivetune.compat.File
import moe.rukamori.archivetune.compat.System
import moe.rukamori.archivetune.compat.Timber
import moe.rukamori.archivetune.compat.media3.Download
import moe.rukamori.archivetune.compat.media3.DownloadCommandHandler
import moe.rukamori.archivetune.compat.media3.DownloadRequest
import moe.rukamori.archivetune.compat.media3.DownloadService
import moe.rukamori.archivetune.compat.time.LocalDateTime
import moe.rukamori.archivetune.compat.toUri
import moe.rukamori.archivetune.db.MusicDatabase
import moe.rukamori.archivetune.db.entities.FormatEntity
import moe.rukamori.archivetune.db.entities.SongEntity
import moe.rukamori.archivetune.innertube.network.createPlatformHttpClient
import moe.rukamori.archivetune.morideobfuscator.youtubei.YoutubeiResolvedStream

/** Il "servizio" dei download: in ArchiveTuneX i download li esegue [DownloadUtil] dentro l'app. */
object ExoDownloadService

/**
 * Download dei brani per l'ascolto offline, con la stessa interfaccia del DownloadUtil originale
 * (mappa [downloads] con lo stato Media3 di ogni brano). I file finiscono in files/downloads:
 * su iPhone li legge AVPlayer, su Android ExoPlayer.
 */
class DownloadUtil(
    val database: MusicDatabase,
) : DownloadCommandHandler {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val directory = File(AppContext.filesDir, "downloads")
    private val indexFile = File(directory, "index.json")
    private val indexMutex = Mutex()
    private val json = Json { ignoreUnknownKeys = true }
    private val httpClient: HttpClient by lazy { createPlatformHttpClient(proxy = null, dnsOverHttpsUrl = null) {} }

    val downloads = MutableStateFlow<Map<String, Download>>(emptyMap())

    private val queue = ArrayDeque<String>()
    private var worker: Job? = null
    private var currentJob: Job? = null
    private var currentId: String? = null
    private var paused = false

    init {
        DownloadService.handler = this
        scope.launch {
            val restored = readIndex()
            downloads.value =
                restored.associate { entry ->
                    val state =
                        when {
                            entry.state == Download.STATE_COMPLETED && fileFor(entry.id) == null -> Download.STATE_QUEUED
                            entry.state == Download.STATE_DOWNLOADING -> Download.STATE_QUEUED
                            else -> entry.state
                        }
                    entry.id to entry.toDownload(state)
                }
            downloads.value.values
                .filter { it.state == Download.STATE_QUEUED }
                .forEach { enqueue(it.request.id) }
        }
    }

    fun getDownload(songId: String): Flow<Download?> = downloads.map { it[songId] }

    /** Il file scaricato del brano, se il download è completo. */
    fun fileFor(mediaId: String): File? =
        directory
            .listFiles { it.name.startsWith("$mediaId.") && !it.name.endsWith(".part") && it.name != "index.json" }
            ?.firstOrNull()
            ?.takeIf { it.length() > 0 }

    fun isDownloaded(mediaId: String): Boolean = downloads.value[mediaId]?.state == Download.STATE_COMPLETED && fileFor(mediaId) != null

    // region Comandi (DownloadService)

    override fun addDownload(request: DownloadRequest) {
        val existing = downloads.value[request.id]
        if (existing?.state == Download.STATE_COMPLETED && fileFor(request.id) != null) return
        updateDownload(request, Download.STATE_QUEUED, bytes = 0, length = existing?.contentLength ?: -1)
        enqueue(request.id)
    }

    override fun removeDownload(id: String) {
        if (currentId == id) currentJob?.cancel()
        queue.remove(id)
        directory.listFiles { it.name.startsWith("$id.") && it.name != "index.json" }?.forEach { it.delete() }
        downloads.update { it - id }
        persistIndex()
        scope.launch {
            runCatching {
                database.getSongById(id)?.song?.let { song ->
                    database.upsert(song.copy(dateDownload = null))
                }
            }
        }
    }

    override fun removeAllDownloads() {
        downloads.value.keys.toList().forEach(::removeDownload)
    }

    override fun pauseDownloads() {
        paused = true
        currentJob?.cancel()
    }

    override fun resumeDownloads() {
        paused = false
        downloads.value.values
            .filter { it.state == Download.STATE_QUEUED || it.state == Download.STATE_STOPPED }
            .forEach { enqueue(it.request.id) }
    }

    override fun setStopReason(id: String?, stopReason: Int) {
        if (stopReason == Download.STOP_REASON_NONE) resumeDownloads() else pauseDownloads()
    }

    // endregion

    private fun enqueue(id: String) {
        if (id !in queue && id != currentId) queue.addLast(id)
        if (worker?.isActive == true) return
        worker =
            scope.launch {
                while (!paused) {
                    val next = queue.removeFirstOrNull() ?: break
                    currentId = next
                    val job = launch { runDownload(next) }
                    currentJob = job
                    job.join()
                    currentId = null
                    currentJob = null
                }
            }
    }

    private suspend fun runDownload(mediaId: String) {
        val request = downloads.value[mediaId]?.request ?: return
        try {
            val stream = StreamResolver.resolve(mediaId).getOrThrow()
            persistPlaybackMetadata(database, mediaId, stream, downloaded = true)
            val extension = if (stream.mimeType.contains("webm")) "webm" else "m4a"
            val partFile = File(directory, "$mediaId.$extension.part")
            partFile.delete()
            directory.mkdirs()
            val total = stream.contentLength.takeIf { it > 0 }
            var downloaded = 0L
            updateDownload(request, Download.STATE_DOWNLOADING, bytes = 0, length = total ?: -1)
            // YouTube rallenta le richieste lunghe: si scarica a blocchi con l'header Range.
            while (total == null || downloaded < total) {
                val end = if (total != null) minOf(downloaded + CHUNK_SIZE, total) - 1 else downloaded + CHUNK_SIZE - 1
                val response =
                    httpClient.get(stream.url) {
                        stream.requestHeaders.forEach { (key, value) ->
                            if (!key.equals("Range", ignoreCase = true)) header(key, value)
                        }
                        header("Range", "bytes=$downloaded-$end")
                    }
                if (!response.status.isSuccess()) error("HTTP ${response.status.value}")
                val bytes = response.bodyAsBytes()
                if (bytes.isEmpty()) break
                partFile.appendBytes(bytes)
                downloaded += bytes.size
                updateDownload(request, Download.STATE_DOWNLOADING, bytes = downloaded, length = total ?: -1)
                if (total == null && bytes.size < CHUNK_SIZE) break
            }
            val finalFile = File(directory, "$mediaId.$extension")
            finalFile.delete()
            partFile.renameTo(finalFile)
            updateDownload(request, Download.STATE_COMPLETED, bytes = downloaded, length = downloaded)
        } catch (cancellation: CancellationException) {
            val current = downloads.value[mediaId]
            if (current != null) updateDownload(request, Download.STATE_QUEUED, bytes = 0, length = current.contentLength)
            throw cancellation
        } catch (failure: Throwable) {
            Timber.e(failure, "Download non riuscito: %s", mediaId)
            updateDownload(request, Download.STATE_FAILED, bytes = 0, length = -1, failureReason = Download.FAILURE_REASON_UNKNOWN)
        }
    }

    private fun updateDownload(
        request: DownloadRequest,
        state: Int,
        bytes: Long,
        length: Long,
        failureReason: Int = Download.FAILURE_REASON_NONE,
    ) {
        val now = System.currentTimeMillis()
        downloads.update { map ->
            val previous = map[request.id]
            map +
                (
                    request.id to
                        Download(
                            request = request,
                            state = state,
                            startTimeMs = previous?.startTimeMs ?: now,
                            updateTimeMs = now,
                            contentLength = length,
                            stopReason = Download.STOP_REASON_NONE,
                            failureReason = failureReason,
                            bytesDownloaded = bytes,
                            percentDownloaded = if (length > 0) (bytes * 100f / length).coerceIn(0f, 100f) else if (state == Download.STATE_COMPLETED) 100f else 0f,
                        )
                )
        }
        if (state != Download.STATE_DOWNLOADING) persistIndex()
    }

    // region Indice su disco

    @Serializable
    private data class IndexEntry(
        val id: String,
        val title: String,
        val state: Int,
        val contentLength: Long,
        val startTimeMs: Long,
    ) {
        fun toDownload(state: Int): Download =
            Download(
                request = DownloadRequest.Builder(id, id.toUri()).setCustomCacheKey(id).setData(title.encodeToByteArray()).build(),
                state = state,
                startTimeMs = startTimeMs,
                updateTimeMs = startTimeMs,
                contentLength = contentLength,
                stopReason = Download.STOP_REASON_NONE,
                failureReason = Download.FAILURE_REASON_NONE,
                bytesDownloaded = if (state == Download.STATE_COMPLETED) contentLength else 0,
                percentDownloaded = if (state == Download.STATE_COMPLETED) 100f else 0f,
            )
    }

    private suspend fun readIndex(): List<IndexEntry> =
        indexMutex.withLock {
            runCatching { json.decodeFromString<List<IndexEntry>>(indexFile.readText()) }.getOrDefault(emptyList())
        }

    private fun persistIndex() {
        val snapshot =
            downloads.value.values.map {
                IndexEntry(
                    id = it.request.id,
                    title = it.request.data.decodeToString(),
                    state = it.state,
                    contentLength = it.contentLength,
                    startTimeMs = it.startTimeMs,
                )
            }
        scope.launch {
            indexMutex.withLock {
                runCatching {
                    directory.mkdirs()
                    indexFile.writeText(json.encodeToString(snapshot))
                }
            }
        }
    }

    // endregion

    companion object {
        private const val CHUNK_SIZE = 1_048_576L
    }
}

/** Come l'originale: salva formato e dati del brano nel database quando si ottiene lo stream. */
suspend fun persistPlaybackMetadata(
    database: MusicDatabase,
    mediaId: String,
    resolved: YoutubeiResolvedStream,
    downloaded: Boolean = false,
) {
    try {
        database.withTransaction {
            val existingFormat = getFormatByIdBlocking(mediaId)
            upsert(
                FormatEntity(
                    id = mediaId,
                    itag = resolved.formatId.takeIf { it > 0 } ?: existingFormat?.itag ?: -1,
                    mimeType = resolved.mimeType.ifBlank { existingFormat?.mimeType.orEmpty() },
                    codecs = resolved.codecs.ifBlank { existingFormat?.codecs.orEmpty() },
                    bitrate = resolved.bitrate.takeIf { it > 0 } ?: existingFormat?.bitrate ?: 0,
                    sampleRate = resolved.sampleRate ?: existingFormat?.sampleRate,
                    contentLength = resolved.contentLength.takeIf { it > 0L } ?: existingFormat?.contentLength ?: 0L,
                    loudnessDb = resolved.loudnessDb ?: existingFormat?.loudnessDb,
                    perceptualLoudnessDb = resolved.perceptualLoudnessDb ?: existingFormat?.perceptualLoudnessDb,
                    playbackUrl = resolved.playbackTrackingUrl ?: existingFormat?.playbackUrl,
                ),
            )
            if (downloaded) {
                val now = LocalDateTime.now()
                val existing = getSongByIdBlocking(mediaId)?.song
                val resolvedThumbnailUrl = resolved.thumbnailUrl?.takeIf { it.isNotBlank() }
                upsert(
                    existing?.copy(
                        thumbnailUrl = existing.thumbnailUrl?.takeIf { it.isNotBlank() } ?: resolvedThumbnailUrl,
                        dateDownload = existing.dateDownload ?: now,
                    ) ?: SongEntity(
                        id = mediaId,
                        title = resolved.title ?: mediaId,
                        duration = resolved.durationSeconds ?: 0,
                        thumbnailUrl = resolvedThumbnailUrl,
                        dateDownload = now,
                    ),
                )
            }
        }
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (failure: Exception) {
        Timber.w(failure, "Failed to persist playback metadata")
    }
}
