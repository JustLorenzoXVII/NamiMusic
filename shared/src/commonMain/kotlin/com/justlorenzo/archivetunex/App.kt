package com.justlorenzo.archivetunex

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import archivetunex.shared.generated.resources.Res
import archivetunex.shared.generated.resources.home_filled
import archivetunex.shared.generated.resources.home_outlined
import archivetunex.shared.generated.resources.library_filled
import archivetunex.shared.generated.resources.library_outlined
import archivetunex.shared.generated.resources.search
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.network.ktor3.KtorNetworkFetcherFactory
import coil3.request.crossfade
import coil3.util.Logger
import com.justlorenzo.archivetunex.ui.AtxActions
import com.justlorenzo.archivetunex.ui.AtxNavigator
import com.justlorenzo.archivetunex.ui.LocalActions
import com.justlorenzo.archivetunex.ui.LocalNavigator
import com.justlorenzo.archivetunex.ui.Route
import com.justlorenzo.archivetunex.ui.Tab
import com.justlorenzo.archivetunex.ui.components.AtxIcon
import com.justlorenzo.archivetunex.ui.screens.AccountSheet
import com.justlorenzo.archivetunex.ui.screens.AlbumScreen
import com.justlorenzo.archivetunex.ui.screens.ArtistScreen
import com.justlorenzo.archivetunex.ui.screens.HomeScreen
import com.justlorenzo.archivetunex.ui.screens.LibraryScreen
import com.justlorenzo.archivetunex.ui.screens.LocalPlaylistScreen
import com.justlorenzo.archivetunex.ui.screens.LoginOverlay
import com.justlorenzo.archivetunex.ui.screens.LogsOverlay
import com.justlorenzo.archivetunex.ui.screens.MiniPlayer
import com.justlorenzo.archivetunex.ui.screens.PlayerScreen
import com.justlorenzo.archivetunex.ui.screens.PlaylistScreen
import com.justlorenzo.archivetunex.ui.screens.QueueSheet
import com.justlorenzo.archivetunex.ui.screens.SearchScreen
import com.justlorenzo.archivetunex.ui.theme.ArchiveTuneXTheme
import moe.rukamori.archivetune.compat.BackHandler
import moe.rukamori.archivetune.compat.SystemBars
import moe.rukamori.archivetune.compat.Timber
import moe.rukamori.archivetune.downloads.DownloadedArtworkRepository
import moe.rukamori.archivetune.innertube.network.createPlatformHttpClient
import org.jetbrains.compose.resources.DrawableResource
import org.koin.mp.KoinPlatform

/**
 * ArchiveTuneX: la nuova interfaccia (Material 3 Expressive, stile Fuji Music), uguale su
 * Android e iPhone, sopra il motore di ArchiveTune.
 */
@Composable
fun App() {
    remember { ArchiveTuneXRuntime.start() }

    // Copertine scaricate con il client di rete della piattaforma (OkHttp su Android, Darwin
    // su iPhone) e, se il brano è scaricato, lette dal telefono.
    setSingletonImageLoaderFactory { context ->
        val downloadedArtwork = KoinPlatform.getKoin().get<DownloadedArtworkRepository>()
        ImageLoader
            .Builder(context)
            .crossfade(true)
            .logger(CoilToTimberLogger)
            .components {
                add(KtorNetworkFetcherFactory(httpClient = { createPlatformHttpClient(proxy = null, dnsOverHttpsUrl = null) {} }))
                add(downloadedArtwork.coilMapper())
            }.build()
    }

    val navigator = remember { AtxNavigator() }
    val actions =
        remember {
            AtxActions(
                player = ArchiveTuneXRuntime.playerConnection,
                database = ArchiveTuneXRuntime.database,
                downloads = ArchiveTuneXRuntime.downloadUtil,
            )
        }
    val dark = isSystemInDarkTheme()
    LaunchedEffect(dark) { SystemBars.setDarkContent(!dark) }

    ArchiveTuneXTheme(darkTheme = dark) {
        CompositionLocalProvider(LocalNavigator provides navigator, LocalActions provides actions) {
            BackHandler(enabled = navigator.canGoBack) { navigator.back() }
            AppContent(navigator)
        }
    }
}

@Composable
private fun AppContent(navigator: AtxNavigator) {
    val stateHolder = rememberSaveableStateHolder()

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // Pagina corrente della scheda; ogni pagina ricorda il punto in cui la si era lasciata
        val stack = navigator.currentStack
        val route = stack.lastOrNull()
        val key = "${navigator.tab}-${stack.size}-$route"
        Box(Modifier.fillMaxSize()) {
            stateHolder.SaveableStateProvider(key) {
                when (route) {
                    null ->
                        when (navigator.tab) {
                            Tab.Home -> HomeScreen()
                            Tab.Search -> SearchScreen()
                            Tab.Library -> LibraryScreen()
                        }
                    is Route.Album -> AlbumScreen(route.browseId)
                    is Route.Artist -> ArtistScreen(route.browseId)
                    is Route.Playlist -> PlaylistScreen(route.playlistId)
                    is Route.LocalPlaylist -> LocalPlaylistScreen(route.playlistId, route.title)
                }
            }
        }

        Column(Modifier.align(Alignment.BottomCenter)) {
            MiniPlayer(Modifier.padding(bottom = 8.dp))
            NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                TabItem(navigator, Tab.Home, "Home", Res.drawable.home_filled, Res.drawable.home_outlined)
                TabItem(navigator, Tab.Search, "Cerca", Res.drawable.search, Res.drawable.search)
                TabItem(navigator, Tab.Library, "Libreria", Res.drawable.library_filled, Res.drawable.library_outlined)
            }
        }

        AnimatedVisibility(
            visible = navigator.playerExpanded,
            enter = slideInVertically { it },
            exit = slideOutVertically { it },
        ) {
            PlayerScreen()
        }
        AnimatedVisibility(visible = navigator.loginVisible, enter = fadeIn(), exit = fadeOut()) {
            LoginOverlay()
        }
        AnimatedVisibility(visible = navigator.logsVisible, enter = fadeIn(), exit = fadeOut()) {
            LogsOverlay()
        }
        if (navigator.queueVisible) QueueSheet()
        if (navigator.accountVisible) AccountSheet()
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.TabItem(
    navigator: AtxNavigator,
    tab: Tab,
    label: String,
    selectedIcon: DrawableResource,
    icon: DrawableResource,
) {
    val selected = navigator.tab == tab
    NavigationBarItem(
        selected = selected,
        onClick = { navigator.selectTab(tab) },
        icon = { AtxIcon(if (selected) selectedIcon else icon, null) },
        label = { Text(label) },
    )
}

/** Errori di caricamento delle copertine nel registro errori. */
private object CoilToTimberLogger : Logger {
    override var minLevel: Logger.Level = Logger.Level.Warn

    override fun log(
        tag: String,
        level: Logger.Level,
        message: String?,
        throwable: Throwable?,
    ) {
        if (level < minLevel) return
        val text = listOfNotNull(message, throwable?.toString()).joinToString(" - ")
        Timber.tag("Coil").w(text)
    }
}
