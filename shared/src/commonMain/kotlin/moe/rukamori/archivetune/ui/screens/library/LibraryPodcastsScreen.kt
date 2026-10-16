/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.rukamori.archivetune.ui.screens.library

import archivetunex.shared.generated.resources.*

import moe.rukamori.archivetune.compat.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.unit.dp
import org.koin.compose.viewmodel.koinViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import moe.rukamori.archivetune.LocalPlayerAwareWindowInsets
import moe.rukamori.archivetune.LocalPlayerConnection
import moe.rukamori.archivetune.extensions.toMediaItem
import moe.rukamori.archivetune.playback.queues.ListQueue
import moe.rukamori.archivetune.podcast.PodcastLibraryAction
import moe.rukamori.archivetune.podcast.PodcastLibraryEvent
import moe.rukamori.archivetune.podcast.PodcastLibraryItemUiModel
import moe.rukamori.archivetune.podcast.PodcastLibraryScreenState
import moe.rukamori.archivetune.ui.component.LocalMenuState
import moe.rukamori.archivetune.ui.component.MediaDetailStatePanel
import moe.rukamori.archivetune.ui.component.MenuSurfaceSection
import moe.rukamori.archivetune.ui.component.NewMenuItem
import moe.rukamori.archivetune.viewmodels.PodcastLibraryViewModel

@Composable
fun LibraryPodcastsScreen(
    navController: NavController,
    viewModel: PodcastLibraryViewModel = koinViewModel(),
) {
    val state by viewModel.screenState.collectAsStateWithLifecycle()
    val refreshState by viewModel.refreshState.collectAsStateWithLifecycle()
    val playerConnection = LocalPlayerConnection.current
    val menuState = LocalMenuState.current
    val snackbarHostState = remember { SnackbarHostState() }
    val unknownErrorMessage = stringResource(Res.string.error_unknown)
    val loginRequiredMessage = stringResource(Res.string.not_logged_in_youtube)
    val syncDisabledMessage = stringResource(Res.string.sync_disabled)
    val noEpisodesMessage = stringResource(Res.string.podcast_has_no_episodes)
    val onRefresh = remember(viewModel) { { viewModel.onAction(PodcastLibraryAction.Refresh) } }
    val onRefreshErrorShown = remember(viewModel) { { viewModel.onRefreshErrorShown() } }
    val onPodcastClick =
        remember(navController) {
            { browseId: String -> navController.navigate("podcast/${Uri.encode(browseId)}") }
        }
    val onPodcastPlay =
        remember(viewModel) {
            { browseId: String -> viewModel.onAction(PodcastLibraryAction.PlayPodcast(browseId)) }
        }
    val onPodcastRemove =
        remember(viewModel) {
            { browseId: String -> viewModel.onAction(PodcastLibraryAction.RemovePodcast(browseId)) }
        }
    val onPodcastMenuClick =
        remember(menuState, onPodcastRemove) {
            { podcast: PodcastLibraryItemUiModel ->
                menuState.show {
                    PodcastLibraryMenu(
                        onRemove = { onPodcastRemove(podcast.browseId) },
                        onDismiss = menuState::dismiss,
                    )
                }
            }
        }

    LaunchedEffect(
        viewModel,
        playerConnection,
        unknownErrorMessage,
        loginRequiredMessage,
        syncDisabledMessage,
        noEpisodesMessage,
    ) {
        viewModel.events.collect { event ->
            when (event) {
                is PodcastLibraryEvent.Play -> {
                    playerConnection?.playQueue(
                        ListQueue(
                            title = event.request.title,
                            items = event.request.items.map { metadata -> metadata.toMediaItem() },
                            startIndex = event.request.startIndex,
                        ),
                    )
                }

                is PodcastLibraryEvent.ShowMessage -> {
                    val message =
                        when (event.messageResId) {
                            Res.string.not_logged_in_youtube -> loginRequiredMessage
                            Res.string.sync_disabled -> syncDisabledMessage
                            Res.string.podcast_has_no_episodes -> noEpisodesMessage
                            else -> unknownErrorMessage
                        }
                    snackbarHostState.showSnackbar(message)
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LibraryRefreshContainer(
            state = refreshState,
            onRefresh = onRefresh,
            onErrorShown = onRefreshErrorShown,
            modifier = Modifier.fillMaxSize(),
        ) {
            LibraryPodcastsContent(
                state = state,
                onRetry = onRefresh,
                onPodcastClick = onPodcastClick,
                onPodcastPlay = onPodcastPlay,
                onPodcastMenuClick = onPodcastMenuClick,
            )
        }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(LocalPlayerAwareWindowInsets.current.asPaddingValues()),
        )
    }
}

@Composable
private fun BoxScope.LibraryPodcastsContent(
    state: PodcastLibraryScreenState,
    onRetry: () -> Unit,
    onPodcastClick: (String) -> Unit,
    onPodcastPlay: (String) -> Unit,
    onPodcastMenuClick: (PodcastLibraryItemUiModel) -> Unit,
) {
    val bottomPadding =
        LocalPlayerAwareWindowInsets.current
            .only(WindowInsetsSides.Bottom)
            .asPaddingValues()
            .calculateBottomPadding()

    when (state) {
        PodcastLibraryScreenState.Loading -> {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }

        PodcastLibraryScreenState.Empty -> {
            MediaDetailStatePanel(
                title = stringResource(Res.string.podcast),
                description = stringResource(Res.string.browse_empty_description),
                iconRes = Res.drawable.mic,
                actionLabel = stringResource(Res.string.retry),
                onAction = onRetry,
                modifier = Modifier.align(Alignment.Center),
            )
        }

        is PodcastLibraryScreenState.Error -> {
            MediaDetailStatePanel(
                title = stringResource(Res.string.podcast),
                description = stringResource(state.messageResId),
                iconRes = Res.drawable.error,
                actionLabel = stringResource(Res.string.retry),
                onAction = onRetry,
                modifier = Modifier.align(Alignment.Center),
            )
        }

        is PodcastLibraryScreenState.Success -> {
            LazyColumn(
                contentPadding =
                    PaddingValues(
                        start = 24.dp,
                        top = LibraryHeaderContentPadding,
                        end = 24.dp,
                        bottom = bottomPadding + 16.dp,
                    ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(
                    items = state.uiState.podcasts,
                    key = PodcastLibraryItemUiModel::browseId,
                    contentType = { "library_podcast" },
                ) { podcast ->
                    val onClick = remember(podcast.browseId, onPodcastClick) { { onPodcastClick(podcast.browseId) } }
                    val onPlay = remember(podcast.browseId, onPodcastPlay) { { onPodcastPlay(podcast.browseId) } }
                    val onMenuClick = remember(podcast, onPodcastMenuClick) { { onPodcastMenuClick(podcast) } }
                    PodcastLibraryCard(
                        podcast = podcast,
                        onClick = onClick,
                        onPlay = onPlay,
                        onMenuClick = onMenuClick,
                    )
                }
            }
        }
    }
}

@Composable
private fun PodcastLibraryCard(
    podcast: PodcastLibraryItemUiModel,
    onClick: () -> Unit,
    onPlay: () -> Unit,
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isRemote = podcast.isSavedRemotely
    LibraryMediaListCard(
        title = podcast.title,
        subtitle = podcast.author ?: stringResource(Res.string.podcast),
        thumbnailUrl = podcast.thumbnailUrl,
        sourceLabel = stringResource(if (isRemote) Res.string.youtube_synced else Res.string.personal_label),
        sourceColor = if (isRemote) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
        onClick = onClick,
        onPlay = onPlay,
        onMenuClick = onMenuClick,
        modifier = modifier,
    )
}

@Composable
private fun PodcastLibraryMenu(
    onRemove: () -> Unit,
    onDismiss: () -> Unit,
) {
    MenuSurfaceSection(modifier = Modifier.padding(vertical = 6.dp)) {
        NewMenuItem(
            headlineContent = { Text(stringResource(Res.string.remove_from_library)) },
            leadingContent = {
                Icon(
                    painter = painterResource(Res.drawable.library_add_check),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                )
            },
            onClick = {
                onDismiss()
                onRemove()
            },
        )
    }
}
