package moe.rukamori.archivetune.compat.media3

import moe.rukamori.archivetune.compat.Context
import moe.rukamori.archivetune.compat.Uri
import kotlin.reflect.KClass

/** Richiesta di download (androidx.media3.exoplayer.offline.DownloadRequest). */
class DownloadRequest(
    val id: String,
    val uri: Uri,
    val customCacheKey: String?,
    val data: ByteArray,
    val mimeType: String? = null,
) {
    class Builder(
        private val id: String,
        private val uri: Uri,
    ) {
        private var customCacheKey: String? = null
        private var data: ByteArray = ByteArray(0)
        private var mimeType: String? = null

        fun setCustomCacheKey(value: String?) = apply { customCacheKey = value }

        fun setData(value: ByteArray?) = apply { data = value ?: ByteArray(0) }

        fun setMimeType(value: String?) = apply { mimeType = value }

        fun build(): DownloadRequest = DownloadRequest(id, uri, customCacheKey, data, mimeType)
    }
}

/** Stato di un download (androidx.media3.exoplayer.offline.Download). */
class Download(
    val request: DownloadRequest,
    val state: Int,
    val startTimeMs: Long,
    val updateTimeMs: Long,
    val contentLength: Long,
    val stopReason: Int,
    val failureReason: Int,
    val bytesDownloaded: Long,
    val percentDownloaded: Float,
) {
    val isTerminalState: Boolean
        get() = state == STATE_COMPLETED || state == STATE_FAILED

    companion object {
        const val STATE_QUEUED = 0
        const val STATE_STOPPED = 1
        const val STATE_DOWNLOADING = 2
        const val STATE_COMPLETED = 3
        const val STATE_FAILED = 4
        const val STATE_REMOVING = 5
        const val STATE_RESTARTING = 7

        const val FAILURE_REASON_NONE = 0
        const val FAILURE_REASON_UNKNOWN = 1

        const val STOP_REASON_NONE = 0
    }
}

/** Chi esegue davvero i comandi di [DownloadService] (in ArchiveTuneX: DownloadUtil). */
interface DownloadCommandHandler {
    fun addDownload(request: DownloadRequest)

    fun removeDownload(id: String)

    fun removeAllDownloads()

    fun pauseDownloads()

    fun resumeDownloads()

    fun setStopReason(id: String?, stopReason: Int)
}

/**
 * Come androidx.media3.exoplayer.offline.DownloadService: le schermate mandano i comandi
 * (aggiungi, rimuovi, pausa) e il gestore dei download li esegue.
 */
object DownloadService {
    var handler: DownloadCommandHandler? = null

    fun sendAddDownload(
        context: Context,
        clazz: KClass<*>,
        downloadRequest: DownloadRequest,
        foreground: Boolean,
    ) {
        handler?.addDownload(downloadRequest)
    }

    fun sendAddDownload(
        context: Context,
        clazz: KClass<*>,
        downloadRequest: DownloadRequest,
        stopReason: Int,
        foreground: Boolean,
    ) {
        handler?.addDownload(downloadRequest)
    }

    fun sendRemoveDownload(
        context: Context,
        clazz: KClass<*>,
        id: String,
        foreground: Boolean,
    ) {
        handler?.removeDownload(id)
    }

    fun sendRemoveAllDownloads(
        context: Context,
        clazz: KClass<*>,
        foreground: Boolean,
    ) {
        handler?.removeAllDownloads()
    }

    fun sendPauseDownloads(
        context: Context,
        clazz: KClass<*>,
        foreground: Boolean,
    ) {
        handler?.pauseDownloads()
    }

    fun sendResumeDownloads(
        context: Context,
        clazz: KClass<*>,
        foreground: Boolean,
    ) {
        handler?.resumeDownloads()
    }

    fun sendSetStopReason(
        context: Context,
        clazz: KClass<*>,
        id: String?,
        stopReason: Int,
        foreground: Boolean,
    ) {
        handler?.setStopReason(id, stopReason)
    }
}
