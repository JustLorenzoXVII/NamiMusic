/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.rukamori.archivetune.playlist

import moe.rukamori.archivetune.compat.Uri
import moe.rukamori.archivetune.repository.PlaylistCoverRepository

class UpdatePlaylistCoverUseCase constructor(
        private val repository: PlaylistCoverRepository,
    ) {
        suspend operator fun invoke(
            playlistId: String,
            uri: Uri,
        ) {
            val playlist = requireNotNull(repository.getPlaylist(playlistId))
            if (playlist.browseId == null) {
                repository.setLocalCover(playlist, uri)
            } else {
                repository.setRemoteCover(playlist, uri)
            }
        }
    }

class RemovePlaylistCoverUseCase constructor(
        private val repository: PlaylistCoverRepository,
    ) {
        suspend operator fun invoke(playlistId: String) {
            val playlist = requireNotNull(repository.getPlaylist(playlistId))
            if (playlist.browseId == null) {
                repository.removeLocalCover(playlist)
            } else {
                repository.removeRemoteCover(playlist)
            }
        }
    }
