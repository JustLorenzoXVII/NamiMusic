/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.rukamori.archivetune.browse

import org.jetbrains.compose.resources.StringResource
import androidx.compose.runtime.Immutable
import moe.rukamori.archivetune.compat.ImmutableList
import moe.rukamori.archivetune.innertube.models.EpisodeItem
import moe.rukamori.archivetune.innertube.models.YTItem

sealed interface BrowseScreenState {
    data object Loading : BrowseScreenState

    @Immutable
    data class Success(
        val uiState: BrowseUiState,
    ) : BrowseScreenState

    @Immutable
    data class Empty(
        val title: String?,
        val fallbackTitleResId: StringResource?,
    ) : BrowseScreenState

    @Immutable
    data class Error(
        val messageResId: StringResource,
    ) : BrowseScreenState
}

@Immutable
data class BrowseUiState(
    val title: String?,
    val fallbackTitleResId: StringResource?,
    val items: ImmutableList<YTItem>,
    val isLoadingMore: Boolean,
    val canLoadMore: Boolean,
)

sealed interface BrowseAction {
    data object Retry : BrowseAction

    data object LoadMore : BrowseAction

    data class OpenItem(
        val itemId: String,
    ) : BrowseAction

    data class OpenItemMenu(
        val itemId: String,
    ) : BrowseAction
}

sealed interface BrowseEvent {
    data class OpenAlbum(
        val browseId: String,
    ) : BrowseEvent

    data class OpenPlaylist(
        val playlistId: String,
    ) : BrowseEvent

    data class OpenArtist(
        val browseId: String,
    ) : BrowseEvent

    data class OpenPodcast(
        val browseId: String,
    ) : BrowseEvent

    @Immutable
    data class PlayEpisode(
        val episode: EpisodeItem,
    ) : BrowseEvent

    @Immutable
    data class ShowItemMenu(
        val item: YTItem,
    ) : BrowseEvent

    data class ShowMessage(
        val messageResId: StringResource,
    ) : BrowseEvent
}
