/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.rukamori.archivetune.repository

import moe.rukamori.archivetune.compat.ImageTools
import moe.rukamori.archivetune.compat.contentResolver


import moe.rukamori.archivetune.compat.getOrDefault

import kotlinx.coroutines.IO

import moe.rukamori.archivetune.compat.toByteArray

import moe.rukamori.archivetune.compat.Context
import moe.rukamori.archivetune.compat.Intent
import moe.rukamori.archivetune.compat.startActivity
import moe.rukamori.archivetune.compat.Uri
import moe.rukamori.archivetune.compat.SystemClock
import moe.rukamori.archivetune.compat.LruCache
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import moe.rukamori.archivetune.db.MusicDatabase
import moe.rukamori.archivetune.db.entities.PlaylistEntity
import moe.rukamori.archivetune.innertube.YouTube
import moe.rukamori.archivetune.compat.time.LocalDateTime
import kotlin.math.min

class PlaylistCoverRepository constructor(
        private val context: Context,
        private val database: MusicDatabase,
    ) {
        private val refreshLocks = Array(8) { Mutex() }
        private val refreshedCovers = LruCache<String, RefreshedCover>(64)

        suspend fun refreshRemoteCover(
            playlistId: String,
            thumbnailUrl: String,
        ): String = withContext(Dispatchers.IO) {
            val lock = refreshLocks[(thumbnailUrl.hashCode() and Int.MAX_VALUE) % refreshLocks.size]
            lock.withLock {
                val cached = refreshedCovers.get(thumbnailUrl)
                if (cached != null && SystemClock.elapsedRealtime() - cached.updatedAt < COVER_REFRESH_CACHE_MILLIS) {
                    return@withLock cached.result.getOrThrow()
                }

                val result: Result<String> =
                    try {
                        val refreshedUrl =
                            YouTube.playlist(playlistId).getOrThrow().playlist.thumbnail
                                ?.takeIf(String::isNotBlank)
                                ?: throw IllegalStateException("Playlist returned no cover artwork")
                        database.refreshPlaylistThumbnail(
                            browseId = playlistId,
                            previousThumbnailUrl = thumbnailUrl,
                            thumbnailUrl = refreshedUrl,
                        )
                        Result.success(refreshedUrl)
                    } catch (exception: CancellationException) {
                        throw exception
                    } catch (exception: Exception) {
                        Result.failure(exception)
                    }

                refreshedCovers.put(thumbnailUrl, RefreshedCover(result, SystemClock.elapsedRealtime()))
                result.getOrThrow()
            }
        }

        suspend fun getPlaylist(playlistId: String): PlaylistEntity? =
            withContext(Dispatchers.IO) {
                database.playlist(playlistId).first()?.playlist
            }

        suspend fun setLocalCover(
            playlist: PlaylistEntity,
            uri: Uri,
        ) = withContext(Dispatchers.IO) {
            persistReadPermission(uri)
            try {
                val previous = updateThumbnail(playlist.id, uri.toString())
                previous.thumbnailUrl
                    ?.takeIf { it != uri.toString() }
                    ?.let(Uri::parse)
                    ?.takeIf { it.scheme == "content" }
                    ?.let(::releaseReadPermission)
            } catch (throwable: Throwable) {
                releaseReadPermission(uri)
                throw throwable
            }
        }

        suspend fun setRemoteCover(
            playlist: PlaylistEntity,
            uri: Uri,
        ) = withContext(Dispatchers.IO) {
            persistReadPermission(uri)
            try {
                val image = decodeUploadImage(uri)
                val remoteCoverUrl =
                    YouTube
                        .uploadCustomPlaylistCover(
                            playlistId = requireNotNull(playlist.browseId),
                            image = image,
                        ).getOrThrow()
                val previous = updateThumbnail(playlist.id, remoteCoverUrl)
                previous.thumbnailUrl
                    ?.let(Uri::parse)
                    ?.takeIf { it.scheme == "content" }
                    ?.let(::releaseReadPermission)
            } finally {
                releaseReadPermission(uri)
            }
        }

        suspend fun removeLocalCover(playlist: PlaylistEntity) =
            withContext(Dispatchers.IO) {
                val previous = updateThumbnail(playlist.id, null)
                previous.thumbnailUrl
                    ?.let(Uri::parse)
                    ?.takeIf { it.scheme == "content" }
                    ?.let(::releaseReadPermission)
            }

        suspend fun removeRemoteCover(playlist: PlaylistEntity) =
            withContext(Dispatchers.IO) {
                val remoteCoverUrl =
                    YouTube
                        .removeCustomPlaylistCover(requireNotNull(playlist.browseId))
                        .getOrThrow()
                val previous = updateThumbnail(playlist.id, remoteCoverUrl)
                previous.thumbnailUrl
                    ?.let(Uri::parse)
                    ?.takeIf { it.scheme == "content" }
                    ?.let(::releaseReadPermission)
            }

        private suspend fun updateThumbnail(
            playlistId: String,
            thumbnailUrl: String?,
        ): PlaylistEntity {
            val current =
                requireNotNull(database.playlist(playlistId).first()?.playlist) {
                    "Playlist $playlistId no longer exists"
                }
            database.withTransaction {
                update(
                    current.copy(
                        thumbnailUrl = thumbnailUrl,
                        lastUpdateTime = LocalDateTime.now(),
                    ),
                )
            }
            return current
        }

        private fun persistReadPermission(uri: Uri) {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        }

        private fun releaseReadPermission(uri: Uri) {
            runCatching {
                context.contentResolver.releasePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
        }

        private fun decodeUploadImage(uri: Uri): ByteArray {
            val bytes =
                context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: throw IllegalArgumentException("Selected playlist cover could not be opened")
            require(ImageTools.isDecodableImage(bytes)) { "Selected playlist cover is not a valid image" }
            return ImageTools.prepareSquareJpeg(
                bytes = bytes,
                maxDimension = UPLOAD_DIMENSION_PX,
                maxBytes = MAX_UPLOAD_BYTES,
                initialQuality = INITIAL_JPEG_QUALITY,
                minQuality = MIN_JPEG_QUALITY,
                qualityStep = JPEG_QUALITY_STEP,
            )
        }

        private data class RefreshedCover(
            val result: Result<String>,
            val updatedAt: Long,
        )

        private companion object {
            const val COVER_REFRESH_CACHE_MILLIS = 60_000L
            const val UPLOAD_DIMENSION_PX = 1080
            const val MAX_UPLOAD_BYTES = 2 * 1024 * 1024
            const val MAX_DECODE_PIXELS = 8_000_000L
            const val INITIAL_JPEG_QUALITY = 92
            const val MIN_JPEG_QUALITY = 60
            const val JPEG_QUALITY_STEP = 8
        }
    }
