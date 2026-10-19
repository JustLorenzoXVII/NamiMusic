package com.justlorenzo.archivetunex.ui

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf

/** Le pagine delle Impostazioni. */
enum class SettingsPage { Appearance, Player, MiniPlayer, Playback, Content, Storage, Privacy, About }

/** Le tre schede in basso. */
enum class Tab { Home, Search, Library }

/** Le pagine che si aprono sopra una scheda. */
sealed interface Route {
    data class Album(val browseId: String) : Route

    data class Artist(val browseId: String) : Route

    data class Playlist(val playlistId: String) : Route

    data class LocalPlaylist(val playlistId: String, val title: String) : Route

    data object Downloads : Route

    data object Liked : Route

    /** Pagina di YouTube Music generica (generi e mood, "altro" di una sezione). */
    data class Browse(val browseId: String, val params: String?, val title: String) : Route
}

/**
 * Navigazione dell'app: una pila di pagine per ogni scheda, così passando da una scheda
 * all'altra si ritrova la pagina dove si era rimasti (come Apple Music e YouTube Music).
 */
@Stable
class AtxNavigator {
    var tab by mutableStateOf(Tab.Home)
        private set

    var playerExpanded by mutableStateOf(false)
    var queueVisible by mutableStateOf(false)
    var loginVisible by mutableStateOf(false)
    var accountVisible by mutableStateOf(false)
    var logsVisible by mutableStateOf(false)
    var lyricsVisible by mutableStateOf(false)
    var settingsVisible by mutableStateOf(false)
    var settingsPage by mutableStateOf<SettingsPage?>(null)

    /** Apre le Impostazioni, direttamente su una pagina se indicata. */
    fun openSettings(page: SettingsPage? = null) {
        accountVisible = false
        settingsPage = page
        settingsVisible = true
    }

    private val stacks: Map<Tab, MutableList<Route>> =
        Tab.entries.associateWith { mutableStateListOf<Route>() }

    val currentStack: List<Route>
        get() = stacks.getValue(tab)

    fun selectTab(newTab: Tab) {
        if (newTab == tab) {
            // Toccando di nuovo la scheda si torna alla sua pagina principale
            stacks.getValue(tab).clear()
        } else {
            tab = newTab
        }
    }

    fun open(route: Route) {
        playerExpanded = false
        queueVisible = false
        lyricsVisible = false
        settingsVisible = false
        stacks.getValue(tab).add(route)
    }

    /** Sostituisce la pagina aperta con un'altra (senza lasciarla nella cronologia). */
    fun replace(route: Route) {
        val stack = stacks.getValue(tab)
        if (stack.isNotEmpty()) stack.removeAt(stack.lastIndex)
        stack.add(route)
    }

    /** Indietro: chiude prima ciò che è sopra (coda, player, login), poi la pagina aperta. */
    fun back(): Boolean {
        when {
            logsVisible -> logsVisible = false
            settingsVisible && settingsPage != null -> settingsPage = null
            settingsVisible -> settingsVisible = false
            lyricsVisible -> lyricsVisible = false
            loginVisible -> loginVisible = false
            queueVisible -> queueVisible = false
            playerExpanded -> playerExpanded = false
            stacks.getValue(tab).isNotEmpty() -> stacks.getValue(tab).removeAt(stacks.getValue(tab).lastIndex)
            tab != Tab.Home -> tab = Tab.Home
            else -> return false
        }
        return true
    }

    val canGoBack: Boolean
        get() = logsVisible || settingsVisible || lyricsVisible || loginVisible || queueVisible || playerExpanded || currentStack.isNotEmpty() || tab != Tab.Home
}

val LocalNavigator = staticCompositionLocalOf<AtxNavigator> { error("AtxNavigator non disponibile") }
