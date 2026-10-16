/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.rukamori.archivetune.backup

import moe.rukamori.archivetune.compat.resolveSibling

import moe.rukamori.archivetune.compat.getDatabasePath

import moe.rukamori.archivetune.compat.contentResolver

import moe.rukamori.archivetune.compat.Charsets

import kotlinx.coroutines.IO

import moe.rukamori.archivetune.compat.Context
import moe.rukamori.archivetune.compat.Uri
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import moe.rukamori.archivetune.db.InternalDatabase
import moe.rukamori.archivetune.db.MusicDatabase
import moe.rukamori.archivetune.extensions.zipOutputStream
import moe.rukamori.archivetune.utils.dataStore
import moe.rukamori.archivetune.compat.FileInputStream
import moe.rukamori.archivetune.compat.OutputStream
import moe.rukamori.archivetune.compat.ZipEntry
import kotlin.math.roundToInt

enum class BackupArchiveStep {
    EXPORT_SETTINGS,
    CHECKPOINT_DATABASE,
    COPY_DATABASE_FILE,
}

data class BackupArchiveProgress(
    val step: BackupArchiveStep,
    val fileName: String? = null,
    val percent: Int,
    val indeterminate: Boolean,
)

class BackupArchiveRepository constructor(
        private val context: Context,
        private val database: MusicDatabase,
        private val operationCoordinator: BackupOperationCoordinator,
    ) {
        suspend fun createBackup(
            uri: Uri,
            categories: Set<BackupArchiveCategory>,
            onProgress: (BackupArchiveProgress) -> Unit = {},
        ) = withContext(Dispatchers.IO) {
            operationCoordinator.withLock {
                require(categories.isNotEmpty()) { "At least one backup category is required" }

                val includeSettings = BackupArchiveCategory.SETTINGS in categories
                val includeAccount = BackupArchiveCategory.ACCOUNT in categories
                val includeLibrary = BackupArchiveCategory.LIBRARY in categories
                val settingsExcludedKeys = if (includeAccount) emptySet() else ACCOUNT_PREFERENCE_KEYS
                val dbFile = context.getDatabasePath(MusicDatabase.DB_NAME)
                val dbFiles =
                    if (includeLibrary) {
                        listOf(
                            dbFile,
                            dbFile.resolveSibling("${MusicDatabase.DB_NAME}-wal"),
                        ).filter { it.exists() }
                    } else {
                        emptyList()
                    }

                val totalUnits = (if (includeSettings) 1 else 0) + (if (includeLibrary) 1 else 0) + dbFiles.size
                val unitSpan = 100f / totalUnits.coerceAtLeast(1)
                var completedUnits = 0
                var lastProgress: BackupArchiveProgress? = null

                fun emit(
                    step: BackupArchiveStep,
                    fileName: String? = null,
                    unitFraction: Float = 0f,
                    indeterminate: Boolean = false,
                ) {
                    val progress =
                        BackupArchiveProgress(
                            step = step,
                            fileName = fileName,
                            percent =
                                ((completedUnits + unitFraction.coerceIn(0f, 1f)) * unitSpan)
                                    .roundToInt()
                                    .coerceIn(0, 100),
                            indeterminate = indeterminate,
                        )
                    if (progress != lastProgress) {
                        lastProgress = progress
                        onProgress(progress)
                    }
                }

                val output =
                    context.contentResolver.openOutputStream(uri, "wt")
                        ?: throw IllegalStateException("Failed to open backup destination")
                output.buffered().zipOutputStream().use { zipStream ->
                    if (includeSettings) {
                        emit(BackupArchiveStep.EXPORT_SETTINGS, indeterminate = true)
                        zipStream.putNextEntry(ZipEntry(SETTINGS_XML_FILENAME))
                        writeSettingsToXml(zipStream, settingsExcludedKeys)
                        zipStream.closeEntry()
                        completedUnits++
                    }

                    if (includeLibrary) {
                        emit(BackupArchiveStep.CHECKPOINT_DATABASE, indeterminate = true)
                        database.awaitIdle()
                        database.checkpoint()
                        completedUnits++

                        val buffer = ByteArray(BUFFER_SIZE)
                        run {
                            listOf(dbFile, dbFile.resolveSibling("${MusicDatabase.DB_NAME}-wal"))
                                .filter { it.exists() }
                                .forEach { file ->
                                    val fileSize = file.length().coerceAtLeast(1L)
                                    var bytesCopied = 0L
                                    emit(BackupArchiveStep.COPY_DATABASE_FILE, file.name)
                                    zipStream.putNextEntry(ZipEntry(file.name))
                                    FileInputStream(file).use { input ->
                                        while (true) {
                                            currentCoroutineContext().ensureActive()
                                            val read = input.read(buffer)
                                            if (read <= 0) break
                                            zipStream.write(buffer, 0, read)
                                            bytesCopied += read
                                            emit(
                                                step = BackupArchiveStep.COPY_DATABASE_FILE,
                                                fileName = file.name,
                                                unitFraction = bytesCopied.toFloat() / fileSize.toFloat(),
                                            )
                                        }
                                    }
                                    zipStream.closeEntry()
                                    completedUnits++
                                }
                        }
                    }
                }
            }
        }

        private suspend fun writeSettingsToXml(
            outputStream: OutputStream,
            excludedKeyNames: Set<String>,
        ) {
            val preferences =
                context.dataStore.data
                    .first()
                    .asMap()
            val serializer = moe.rukamori.archivetune.compat.xml.Xml.newSerializer()
            serializer.setOutput(outputStream, Charsets.UTF_8.name())
            serializer.startDocument(Charsets.UTF_8.name(), true)
            serializer.startTag(null, "ArchiveTuneBackup")
            serializer.startTag(null, "Settings")

            preferences.forEach { (key, value) ->
                if (
                    key.name !in excludedKeyNames &&
                    key.name !in ScheduledBackupRepository.NON_PORTABLE_PREFERENCE_KEYS
                ) {
                    serializer.writePreference(key, value)
                }
            }

            serializer.endTag(null, "Settings")
            serializer.endTag(null, "ArchiveTuneBackup")
            serializer.endDocument()
            serializer.flush()
        }

        private fun moe.rukamori.archivetune.compat.xml.StreamingXmlSerializer.writePreference(
            key: Preferences.Key<*>,
            value: Any,
        ) {
            val tagName =
                when (value) {
                    is Boolean -> "boolean"
                    is Int -> "int"
                    is Long -> "long"
                    is Float -> "float"
                    is String -> "string"
                    is Set<*> -> "string-set"
                    else -> return
                }
            startTag(null, tagName)
            attribute(null, "name", key.name)
            if (value is Set<*>) {
                value.forEach { item ->
                    startTag(null, "item")
                    text(item.toString())
                    endTag(null, "item")
                }
            } else {
                attribute(null, "value", value.toString())
            }
            endTag(null, tagName)
        }

        companion object {
            const val SETTINGS_XML_FILENAME = "settings.xml"
            private const val BUFFER_SIZE = 64 * 1024

            val ACCOUNT_PREFERENCE_KEYS: Set<String> =
                setOf(
                    "innerTubeCookie",
                    "visitorData",
                    "dataSyncId",
                    "poToken",
                    "poTokenGvs",
                    "poTokenPlayer",
                    "poTokenSourceUrl",
                    "webClientPoTokenEnabled",
                    "accountName",
                    "accountEmail",
                    "accountChannelHandle",
                    "useLoginForBrowse",
                    "lastfmSession",
                    "lastfmUsername",
                    "lastfmProvider",
                    "lastfmCustomEndpoint",
                    "lastfmApiKeyOverride",
                    "lastfmSecretOverride",
                    "paxsenixApiKey",
                    "listenbrainz_token",
                    "discordToken",
                    "discordUsername",
                    "discordName",
                    "proxyUsername",
                    "proxyPassword",
                    "spotify_sp_dc",
                    "spotify_sp_key",
                    "spotify_access_token",
                    "spotify_access_token_expires_at",
                    "spotify_account_name",
                    "spotify_account_avatar_url",
                )
        }
    }

enum class BackupArchiveCategory {
    LIBRARY,
    ACCOUNT,
    SETTINGS,
    DOWNLOADS,
}
