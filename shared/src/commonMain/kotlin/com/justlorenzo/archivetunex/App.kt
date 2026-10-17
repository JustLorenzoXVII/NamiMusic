package com.justlorenzo.archivetunex

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import archivetunex.shared.generated.resources.atx_home
import com.justlorenzo.archivetunex.ui.screens.BrowseScreen
import com.justlorenzo.archivetunex.ui.screens.DownloadsScreen
import com.justlorenzo.archivetunex.ui.screens.LikedScreen
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import archivetunex.shared.generated.resources.Res
import archivetunex.shared.generated.resources.library_filled
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
import com.justlorenzo.archivetunex.ui.screens.SettingsOverlay
import com.justlorenzo.archivetunex.ui.screens.LogsOverlay
import com.justlorenzo.archivetunex.ui.screens.MiniPlayer
import com.justlorenzo.archivetunex.ui.screens.PlayerScreen
import com.justlorenzo.archivetunex.ui.screens.PlaylistScreen
import com.justlorenzo.archivetunex.ui.screens.QueueSheet
import com.justlorenzo.archivetunex.ui.screens.SearchScreen
import com.justlorenzo.archivetunex.ui.theme.ArchiveTuneXTheme
import com.justlorenzo.archivetunex.ui.ThemeSource
import com.justlorenzo.archivetunex.ui.toMaterialKolor
import com.justlorenzo.archivetunex.ui.LocalAtxStyle
import com.justlorenzo.archivetunex.ui.TabLabels
import com.justlorenzo.archivetunex.ui.rememberAtxStyle
import com.justlorenzo.archivetunex.ui.components.rememberArtworkColors
import androidx.compose.runtime.collectAsState
import moe.rukamori.archivetune.compat.BackHandler
import moe.rukamori.archivetune.compat.SystemBars
import moe.rukamori.archivetune.compat.Timber
import moe.rukamori.archivetune.constants.DarkModeKey
import moe.rukamori.archivetune.ui.screens.settings.DarkMode
import moe.rukamori.archivetune.utils.rememberEnumPreference
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
    val darkMode by rememberEnumPreference(DarkModeKey, DarkMode.AUTO)
    val systemDark = isSystemInDarkTheme()
    val dark =
        when (darkMode) {
            DarkMode.ON -> true
            DarkMode.OFF -> false
            DarkMode.AUTO -> systemDark
        }
    LaunchedEffect(dark) { SystemBars.setDarkContent(!dark) }

    val style = rememberAtxStyle()
    // "Colori della copertina": tutta l'app prende i colori del brano in riproduzione
    val nowPlaying by actions.player.mediaMetadata.collectAsState()
    val albumColors = rememberArtworkColors(if (style.themeSource == ThemeSource.Album) nowPlaying?.thumbnailUrl else null)
    val seedTarget =
        when (style.themeSource) {
            ThemeSource.Album -> albumColors?.primary ?: style.customColor
            else -> style.customColor
        }
    val seed by animateColorAsState(seedTarget, tween(700), label = "seed")

    ArchiveTuneXTheme(
        darkTheme = dark,
        seed = seed,
        paletteStyle = style.paletteStyle.toMaterialKolor(),
        useSystemColors = style.themeSource == ThemeSource.System,
        pureBlack = style.pureBlack,
    ) {
        CompositionLocalProvider(
            LocalNavigator provides navigator,
            LocalActions provides actions,
            LocalAtxStyle provides style,
            moe.rukamori.archivetune.LocalPlayerConnection provides actions.player,
            moe.rukamori.archivetune.LocalDatabase provides actions.database,
        ) {
            BackHandler(enabled = navigator.canGoBack) { navigator.back() }
            AppContent(navigator)
        }
    }
}

@Composable
private fun AppContent(navigator: AtxNavigator) {
    val stateHolder = rememberSaveableStateHolder()

    // Surface: dà a tutti i testi il colore giusto per il tema (chiaro o scuro)
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(Modifier.fillMaxSize()) {
            // Pagina corrente della scheda; ogni pagina ricorda il punto in cui la si era lasciata
            val stack = navigator.currentStack
            val route = stack.lastOrNull()
            val key = "${navigator.tab}-${stack.size}-$route"
            // Durante la dissolvenza ci sono due pagine insieme: ognuna usa il proprio stato
            // (targetState), così le chiavi non si ripetono.
            AnimatedContent(
                targetState = Triple(key, navigator.tab, route),
                transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(160)) },
                contentKey = { it.first },
                label = "pagina",
                modifier = Modifier.fillMaxSize(),
            ) { (pageKey, pageTab, pageRoute) ->
                stateHolder.SaveableStateProvider(pageKey) {
                    when (pageRoute) {
                        null ->
                            when (pageTab) {
                                Tab.Home -> HomeScreen()
                                Tab.Search -> SearchScreen()
                                Tab.Library -> LibraryScreen()
                            }
                        is Route.Album -> AlbumScreen(pageRoute.browseId)
                        is Route.Artist -> ArtistScreen(pageRoute.browseId)
                        is Route.Playlist -> PlaylistScreen(pageRoute.playlistId)
                        is Route.LocalPlaylist -> LocalPlaylistScreen(pageRoute.playlistId, pageRoute.title)
                        is Route.Browse -> BrowseScreen(pageRoute.browseId, pageRoute.params, pageRoute.title)
                        Route.Downloads -> DownloadsScreen()
                        Route.Liked -> LikedScreen()
                    }
                }
            }

            // Barra fluttuante in basso: mini player e schede a pillola
            Column(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth().navigationBarsPadding().padding(bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                MiniPlayer()
                FloatingTabs(navigator, Modifier.align(Alignment.CenterHorizontally))
            }

            AnimatedVisibility(
                visible = navigator.playerExpanded,
                enter = slideInVertically(tween(380)) { it },
                exit = slideOutVertically(tween(300)) { it },
            ) {
                PlayerScreen()
            }
            AnimatedVisibility(visible = navigator.settingsVisible, enter = fadeIn(), exit = fadeOut()) {
                SettingsOverlay()
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
}

/** Schede a pillola fluttuante: quella attiva si allarga e mostra il nome (stile Expressive). */
@Composable
private fun FloatingTabs(
    navigator: AtxNavigator,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.shadow(16.dp, RoundedCornerShape(36.dp)),
        shape = RoundedCornerShape(36.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Row(
            Modifier.padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TabPill(navigator, Tab.Home, "Home", Res.drawable.atx_home)
            TabPill(navigator, Tab.Search, "Cerca", Res.drawable.search)
            TabPill(navigator, Tab.Library, "Libreria", Res.drawable.library_filled)
        }
    }
}

@Composable
private fun TabPill(
    navigator: AtxNavigator,
    tab: Tab,
    label: String,
    icon: DrawableResource,
) {
    val selected = navigator.tab == tab
    val labels = LocalAtxStyle.current.tabLabels
    val showLabel = labels == TabLabels.Always || (labels == TabLabels.Selected && selected)
    val container by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
        label = "tab",
    )
    val content by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "tabContent",
    )
    Row(
        Modifier
            .clip(RoundedCornerShape(30.dp))
            .background(container)
            .clickable { navigator.selectTab(tab) }
            .animateContentSize(spring(dampingRatio = 0.6f, stiffness = 500f))
            .padding(horizontal = if (selected) 20.dp else 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AtxIcon(icon, if (showLabel) null else label, tint = content)
        if (showLabel) {
            Text(label, color = content, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        }
    }
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
