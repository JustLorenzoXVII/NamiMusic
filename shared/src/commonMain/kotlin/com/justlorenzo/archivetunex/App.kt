package com.justlorenzo.archivetunex

import androidx.compose.ui.graphics.Color
import com.justlorenzo.archivetunex.ui.toAtxStyle
import moe.rukamori.archivetune.extensions.toEnum
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import com.justlorenzo.archivetunex.ui.tr
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.spring
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.foundation.layout.width
import androidx.compose.ui.layout.onSizeChanged
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
import com.justlorenzo.archivetunex.ui.screens.PlayerMorph
import com.justlorenzo.archivetunex.ui.screens.JoinedBottomBar
import com.justlorenzo.archivetunex.ui.screens.BarTab
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
import moe.rukamori.archivetune.utils.dataStore
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

    // Le impostazioni salvate si leggono PRIMA di disegnare l'app: così il primo fotogramma ha già
    // tema, colori, lingua e stile giusti (prima si vedevano quelli predefiniti e poi cambiavano di colpo).
    // Intanto resta il colore della schermata di avvio; su Android la splash aspetta [AppStartup.ready].
    val startupPrefs by androidx.compose.runtime.produceState<androidx.datastore.preferences.core.Preferences?>(null) {
        value = moe.rukamori.archivetune.compat.AppContext.dataStore.data.first()
    }
    val prefs = startupPrefs
    if (prefs == null) {
        Box(Modifier.fillMaxSize().background(Color(0xFF2B1230)))
        return
    }
    LoadedApp(prefs)
}

/** Lo stato dell'avvio: pronto quando l'app ha letto le impostazioni e disegna con quelle giuste. */
object AppStartup {
    @kotlin.concurrent.Volatile
    var ready: Boolean = false
}

@Composable
private fun LoadedApp(prefs: androidx.datastore.preferences.core.Preferences) {
    val navigator = remember { AtxNavigator() }
    val actions =
        remember {
            AtxActions(
                player = ArchiveTuneXRuntime.playerConnection,
                database = ArchiveTuneXRuntime.database,
                downloads = ArchiveTuneXRuntime.downloadUtil,
            )
        }
    val dataStore = moe.rukamori.archivetune.compat.AppContext.dataStore
    // Valori iniziali presi dalle impostazioni già lette: niente "salto" al primo fotogramma
    val darkMode by remember { dataStore.data.map { it[DarkModeKey].toEnum(DarkMode.AUTO) }.distinctUntilChanged() }
        .collectAsState(prefs[DarkModeKey].toEnum(DarkMode.AUTO))
    // Lingua dell'interfaccia: impostata subito, e cambiandola l'app si ridisegna
    val appLanguage by remember {
        dataStore.data.map { it[com.justlorenzo.archivetunex.ui.AppLanguageKey].toEnum(com.justlorenzo.archivetunex.ui.AppLanguage.System) }.distinctUntilChanged()
    }.collectAsState(prefs[com.justlorenzo.archivetunex.ui.AppLanguageKey].toEnum(com.justlorenzo.archivetunex.ui.AppLanguage.System))
    remember { com.justlorenzo.archivetunex.ui.I18n.language = appLanguage }
    // La chiave del LaunchedEffect legge la lingua durante il disegno: quando cambia, l'effetto riparte
    LaunchedEffect(appLanguage) { com.justlorenzo.archivetunex.ui.I18n.language = appLanguage }
    // Primo fotogramma pronto: la splash di Android può sparire
    androidx.compose.runtime.SideEffect { AppStartup.ready = true }
    val systemDark = isSystemInDarkTheme()
    val dark =
        when (darkMode) {
            DarkMode.ON -> true
            DarkMode.OFF -> false
            DarkMode.AUTO -> systemDark
        }
    LaunchedEffect(dark) { SystemBars.setDarkContent(!dark) }

    val style = rememberAtxStyle(remember { prefs.toAtxStyle() })
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
    var miniBounds by remember { mutableStateOf<androidx.compose.ui.geometry.Rect?>(null) }
    val playerTransition = remember { com.justlorenzo.archivetunex.ui.screens.PlayerTransition() }
    var playerProgress by remember { mutableStateOf(0f) }

    // Surface: dà a tutti i testi il colore giusto per il tema (chiaro o scuro)
    CompositionLocalProvider(com.justlorenzo.archivetunex.ui.screens.LocalPlayerTransition provides playerTransition) {
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

            val bottomStyle = LocalAtxStyle.current.miniLayout
            if (bottomStyle == com.justlorenzo.archivetunex.ui.MiniLayout.Joined) {
                // Stile Fuji: mini player e barra di navigazione uniti
                JoinedBottomBar(
                    tabs =
                        listOf(
                            BarTab(Tab.Home, tr("Home"), Res.drawable.atx_home),
                            BarTab(Tab.Search, tr("Cerca"), Res.drawable.search),
                            BarTab(Tab.Library, tr("Libreria"), Res.drawable.library_filled),
                        ),
                    hideMini = playerProgress > 0f,
                    onMiniBounds = { miniBounds = it },
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            } else {
            // Barra fluttuante in basso: mini player e schede a pillola
            Column(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth().navigationBarsPadding().padding(bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Il mini player è largo quanto la barra delle schede (un po' di più per Fluttuante e Grande)
                val density = androidx.compose.ui.platform.LocalDensity.current
                var tabsWidth by remember { mutableStateOf(0.dp) }
                androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxWidth()) {
                    val miniStyle = LocalAtxStyle.current
                    val extra =
                        when (miniStyle.miniLayout) {
                            com.justlorenzo.archivetunex.ui.MiniLayout.Joined, com.justlorenzo.archivetunex.ui.MiniLayout.Pill -> 24.dp
                            com.justlorenzo.archivetunex.ui.MiniLayout.Floating -> 64.dp
                            com.justlorenzo.archivetunex.ui.MiniLayout.Large -> 110.dp
                        }
                    // Spazio minimo per leggere il titolo: copertina + almeno 150 dp di testo + i pulsanti scelti
                    val art =
                        when (miniStyle.miniLayout) {
                            com.justlorenzo.archivetunex.ui.MiniLayout.Joined, com.justlorenzo.archivetunex.ui.MiniLayout.Pill -> 50.dp
                            com.justlorenzo.archivetunex.ui.MiniLayout.Floating -> 56.dp
                            com.justlorenzo.archivetunex.ui.MiniLayout.Large -> 80.dp
                        }
                    val buttons =
                        when (miniStyle.miniButtons) {
                            com.justlorenzo.archivetunex.ui.MiniButtons.PlayOnly -> 1
                            com.justlorenzo.archivetunex.ui.MiniButtons.PlayNext -> 2
                            com.justlorenzo.archivetunex.ui.MiniButtons.All -> 3
                        }
                    val readable = 16.dp + art + 12.dp + 150.dp + 12.dp + 48.dp * buttons + 12.dp
                    val limit = maxWidth - 24.dp
                    val target =
                        if (tabsWidth > 0.dp) maxOf(tabsWidth + extra, readable).coerceAtMost(limit) else limit
                    val miniWidth by androidx.compose.animation.core.animateDpAsState(target, spring(dampingRatio = 0.8f, stiffness = 400f), label = "miniWidth")
                    MiniPlayer(
                        Modifier
                            .align(Alignment.Center)
                            .width(miniWidth)
                            .onGloballyPositioned { miniBounds = it.boundsInRoot() }
                            // Mentre il player è aperto (o si sta aprendo) il mini player vero si nasconde
                            .graphicsLayer { alpha = if (playerProgress > 0f) 0f else 1f },
                    )
                }
                FloatingTabs(
                    navigator,
                    Modifier.align(Alignment.CenterHorizontally).onSizeChanged { tabsWidth = with(density) { it.width.toDp() } },
                )
            }
            }

            // Il player si apre dal mini player e si richiude dentro di lui
            PlayerMorph(
                miniBounds = miniBounds,
                miniRadius =
                    when (LocalAtxStyle.current.miniLayout) {
                        com.justlorenzo.archivetunex.ui.MiniLayout.Joined -> 28.dp
                        com.justlorenzo.archivetunex.ui.MiniLayout.Pill -> 29.dp
                        com.justlorenzo.archivetunex.ui.MiniLayout.Floating -> 26.dp
                        com.justlorenzo.archivetunex.ui.MiniLayout.Large -> 30.dp
                    },
                onProgress = { playerProgress = it },
                topCornersOnly = bottomStyle == com.justlorenzo.archivetunex.ui.MiniLayout.Joined,
            )
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
            TabPill(navigator, Tab.Home, tr("Home"), Res.drawable.atx_home)
            TabPill(navigator, Tab.Search, tr("Cerca"), Res.drawable.search)
            TabPill(navigator, Tab.Library, tr("Libreria"), Res.drawable.library_filled)
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
