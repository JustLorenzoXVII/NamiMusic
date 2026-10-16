/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.rukamori.archivetune.spotify

import kotlinx.coroutines.IO

import androidx.compose.runtime.Immutable
import moe.rukamori.archivetune.compat.ImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import moe.rukamori.archivetune.spotify.models.SpotifyTrack

class ResolveSpotifyPlaylistDownloadsUseCase constructor() {
        suspend operator fun invoke(tracks: List<SpotifyTrack>): ImmutableList<SpotifyDownloadItem> =
            withContext(Dispatchers.IO) {
                val resolvedItems = ArrayList<SpotifyDownloadItem>(tracks.size)

                tracks.chunked(MAX_CONCURRENT_RESOLUTIONS).forEach { chunk ->
                    val resolvedChunk =
                        coroutineScope {
                            chunk
                                .map { track ->
                                    async {
                                        SpotifyPlaybackResolver
                                            .resolveToMetadata(track)
                                            ?.let { metadata ->
                                                SpotifyDownloadItem(
                                                    id = metadata.id,
                                                    title = metadata.title,
                                                )
                                            }
                                    }
                                }.awaitAll()
                                .filterNotNull()
                        }
                    resolvedItems += resolvedChunk
                }

                ImmutableList.copyOf(resolvedItems.distinctBy(SpotifyDownloadItem::id))
            }

        private companion object {
            const val MAX_CONCURRENT_RESOLUTIONS = 8
        }
    }

@Immutable
data class SpotifyDownloadItem(
    val id: String,
    val title: String,
)
