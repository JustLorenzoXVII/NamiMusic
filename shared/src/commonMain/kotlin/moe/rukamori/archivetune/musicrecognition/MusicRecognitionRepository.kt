/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.rukamori.archivetune.musicrecognition

import moe.rukamori.archivetune.compat.getOrDefault

import kotlinx.coroutines.IO

import moe.rukamori.archivetune.compat.System

import moe.rukamori.archivetune.compat.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import moe.rukamori.archivetune.shazamkit.Shazam
import moe.rukamori.archivetune.shazamkit.ShazamSignatureGenerator
import moe.rukamori.archivetune.shazamkit.models.RecognitionResult
import moe.rukamori.archivetune.utils.dataStore
import moe.rukamori.archivetune.compat.Timber
import kotlin.coroutines.coroutineContext

class MusicRecognitionRepository constructor(
        private val context: Context,
    ) {
        fun observeHistory(): Flow<List<RecognitionHistoryEntry>> =
            context.dataStore.data
                .map { preferences ->
                    decodeHistory(preferences[MusicRecognitionHistoryJsonKey])
                        .map(RecognitionHistoryRecord::toDomain)
                }.catch { throwable ->
                    if (throwable is CancellationException) throw throwable
                    Timber.e(throwable, "Failed to observe music recognition history")
                    emit(emptyList())
                }.flowOn(Dispatchers.IO)

        fun observeBackgroundRecognitionEnabled(): Flow<Boolean> =
            context.dataStore.data
                .map { preferences ->
                    preferences[BackgroundRecognitionEnabledKey] ?: BackgroundRecognitionEnabledDefault
                }.distinctUntilChanged()
                .flowOn(Dispatchers.IO)

        suspend fun isBackgroundRecognitionEnabled(): Boolean =
            withContext(Dispatchers.IO) {
                context.dataStore.data.first()[BackgroundRecognitionEnabledKey]
                    ?: BackgroundRecognitionEnabledDefault
            }

        suspend fun setBackgroundRecognitionEnabled(enabled: Boolean) {
            withContext(Dispatchers.IO) {
                context.dataStore.edit { preferences ->
                    preferences[BackgroundRecognitionEnabledKey] = enabled
                }
            }
        }

        suspend fun captureAudio(): ShortArray =
            withContext(Dispatchers.IO) {
                // Microfono: AudioRecord su Android, AVAudioEngine su iPhone.
                moe.rukamori.archivetune.compat.platformRecordMicrophone(SampleRateHz, RecordingDurationMillis)
            }

        suspend fun recognize(samples: ShortArray): Result<RecognizedTrack> {
            val signature =
                withContext(Dispatchers.Default) {
                    ShazamSignatureGenerator()
                        .apply { feedPcm16Mono(samples) }
                        .nextSignatureOrNull()
                } ?: return Result.failure(
                    MusicRecognitionException(MusicRecognitionFailure.SignatureFailed),
                )

            return withContext(Dispatchers.IO) {
                Shazam
                    .recognize(signature.uri, signature.sampleDurationMs)
                    .fold(
                        onSuccess = { Result.success(it.toDomain()) },
                        onFailure = { throwable ->
                            if (throwable is CancellationException) throw throwable
                            val failure =
                                if (
                                    throwable.message?.contains("no match", ignoreCase = true) == true ||
                                    throwable.message?.contains("404") == true
                                ) {
                                    MusicRecognitionFailure.NoMatch
                                } else {
                                    MusicRecognitionFailure.RecognitionFailed
                                }
                            Result.failure(MusicRecognitionException(failure, throwable))
                        },
                    )
            }
        }

        suspend fun saveToHistory(track: RecognizedTrack) {
            val entry = track.toHistoryRecord(System.currentTimeMillis())
            withContext(Dispatchers.IO) {
                context.dataStore.edit { preferences ->
                    val current = decodeHistory(preferences[MusicRecognitionHistoryJsonKey])
                    val next =
                        buildList {
                            add(entry)
                            addAll(current.filterNot { it.stableKey == entry.stableKey })
                        }.take(MusicRecognitionHistoryLimit)
                    preferences[MusicRecognitionHistoryJsonKey] = HistoryJson.encodeToString(next)
                }
            }
        }
    }

private const val SampleRateHz = 16_000
private const val RecordingDurationMillis = 8_400L
private const val MinimumBufferSizeBytes = 4_096
private const val MusicRecognitionHistoryLimit = 50
private const val BackgroundRecognitionEnabledDefault = true

private val MusicRecognitionHistoryJsonKey = stringPreferencesKey("musicRecognitionHistoryJson")
private val BackgroundRecognitionEnabledKey =
    booleanPreferencesKey("musicRecognitionBackgroundEnabled")

private val HistoryJson =
    Json {
        ignoreUnknownKeys = true
        encodeDefaults = false
    }

private fun decodeHistory(raw: String?): List<RecognitionHistoryRecord> =
    raw
        ?.takeIf(String::isNotBlank)
        ?.let {
            runCatching {
                HistoryJson.decodeFromString<List<RecognitionHistoryRecord>>(it)
            }.onFailure { throwable ->
                Timber.w(throwable, "Failed to decode music recognition history")
            }.getOrDefault(emptyList())
        }
        ?: emptyList()

@Serializable
private data class RecognitionHistoryRecord(
    val trackId: String,
    val title: String,
    val artist: String,
    val album: String?,
    val coverArtUrl: String?,
    val coverArtHqUrl: String?,
    val genre: String?,
    val releaseDate: String?,
    val shazamUrl: String?,
    val isrc: String?,
    val recognizedAtEpochMillis: Long,
) {
    val stableKey: String
        get() =
            trackId.takeIf { it.isNotBlank() }
                ?: listOf(title, artist, isrc.orEmpty())
                    .joinToString("|") { it.trim().lowercase() }

    fun toDomain(): RecognitionHistoryEntry =
        RecognitionHistoryEntry(
            trackId = trackId,
            title = title,
            artist = artist,
            album = album,
            coverArtUrl = coverArtUrl,
            coverArtHqUrl = coverArtHqUrl,
            genre = genre,
            releaseDate = releaseDate,
            shazamUrl = shazamUrl,
            isrc = isrc,
            recognizedAtEpochMillis = recognizedAtEpochMillis,
        )
}

private fun RecognizedTrack.toHistoryRecord(recognizedAtEpochMillis: Long): RecognitionHistoryRecord =
    RecognitionHistoryRecord(
        trackId = trackId,
        title = title,
        artist = artist,
        album = album,
        coverArtUrl = coverArtUrl,
        coverArtHqUrl = coverArtHqUrl,
        genre = genre,
        releaseDate = releaseDate,
        shazamUrl = shazamUrl,
        isrc = isrc,
        recognizedAtEpochMillis = recognizedAtEpochMillis,
    )

private fun RecognitionResult.toDomain(): RecognizedTrack =
    RecognizedTrack(
        trackId = trackId,
        title = title,
        artist = artist,
        album = album,
        coverArtUrl = coverArtUrl,
        coverArtHqUrl = coverArtHqUrl,
        genre = genre,
        releaseDate = releaseDate,
        label = label,
        lyrics = lyrics.orEmpty().toList(),
        shazamUrl = shazamUrl,
        isrc = isrc,
    )
