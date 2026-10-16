/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 *
 * ArchiveTuneX: versione Kotlin Multiplatform (Android + iOS) del modulo morideobfuscator.
 */
package moe.rukamori.archivetune.morideobfuscator.youtubei

import moe.rukamori.archivetune.compat.getOrDefault

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readByteArray
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import moe.rukamori.archivetune.innertube.utils.sha1
import kotlin.io.encoding.Base64

/**
 * Cache su disco di youtubei.js (es. il JavaScript del player già analizzato).
 * Nell'originale usava java.io.File; qui kotlinx-io, che funziona su Android e iPhone.
 */
internal class YoutubeiDiskCache {
    private val directory: Path by lazy {
        Path(youtubeiCacheDirectory(), CACHE_DIRECTORY).also { dir ->
            runCatching { SystemFileSystem.createDirectories(dir) }
        }
    }
    private val mutex = Mutex()

    suspend fun read(key: String): String? =
        withContext(Dispatchers.IO) {
            mutex.withLock {
                val file = fileForKey(key)
                val size = SystemFileSystem.metadataOrNull(file)?.takeIf { it.isRegularFile }?.size ?: return@withLock null
                if (size !in 0L..MAX_ENTRY_BYTES) return@withLock null
                val bytes =
                    runCatching { SystemFileSystem.source(file).buffered().use { it.readByteArray() } }.getOrElse {
                        runCatching { SystemFileSystem.delete(file, mustExist = false) }
                        return@withLock null
                    }
                Base64.Default.encode(bytes)
            }
        }

    suspend fun write(requestJson: String) {
        withContext(Dispatchers.IO) {
            mutex.withLock {
                val request = YoutubeiJson.parseToJsonElement(requestJson).jsonObject
                val key = requireNotNull(request["key"]?.jsonPrimitive?.contentOrNull)
                val bytes = LenientBase64.decode(requireNotNull(request["value"]?.jsonPrimitive?.contentOrNull))
                require(bytes.size <= MAX_ENTRY_BYTES)
                val destination = fileForKey(key)
                val temporary = Path(directory, "${destination.name}.tmp")
                try {
                    SystemFileSystem.sink(temporary).buffered().use { it.write(bytes) }
                    SystemFileSystem.atomicMove(temporary, destination)
                    prune(keep = destination)
                } finally {
                    runCatching { SystemFileSystem.delete(temporary, mustExist = false) }
                }
            }
        }
    }

    suspend fun remove(key: String) {
        withContext(Dispatchers.IO) {
            mutex.withLock {
                runCatching { SystemFileSystem.delete(fileForKey(key), mustExist = false) }
            }
        }
    }

    private fun fileForKey(key: String): Path = Path(directory, sha1(key))

    /** kotlinx-io non dà la data di modifica: oltre il limite si tiene solo l'ultimo file scritto. */
    private fun prune(keep: Path) {
        val files =
            runCatching { SystemFileSystem.list(directory) }
                .getOrDefault(emptyList())
                .mapNotNull { path -> SystemFileSystem.metadataOrNull(path)?.takeIf { it.isRegularFile }?.let { path to it.size } }
        var totalBytes = files.sumOf { it.second }
        if (totalBytes <= MAX_TOTAL_BYTES) return
        files.filter { it.first != keep }.sortedByDescending { it.second }.forEach { (path, size) ->
            if (totalBytes <= MAX_TOTAL_BYTES) return
            if (runCatching { SystemFileSystem.delete(path, mustExist = false) }.isSuccess) totalBytes -= size
        }
    }

    private companion object {
        const val CACHE_DIRECTORY = "youtubei-18.0.0"
        const val MAX_ENTRY_BYTES = 8L * 1024L * 1024L
        const val MAX_TOTAL_BYTES = 24L * 1024L * 1024L
    }
}
