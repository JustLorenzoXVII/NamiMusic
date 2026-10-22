package com.justlorenzo.archivetunex.ios

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.ComposeUIViewController
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import moe.rukamori.archivetune.App
import moe.rukamori.archivetune.BuildConfig
import moe.rukamori.archivetune.compat.AppContext
import moe.rukamori.archivetune.compat.File
import moe.rukamori.archivetune.constants.AccountEmailKey
import moe.rukamori.archivetune.constants.AccountNameKey
import moe.rukamori.archivetune.constants.AudioQuality
import moe.rukamori.archivetune.constants.AudioQualityKey
import moe.rukamori.archivetune.constants.ContentCountryKey
import moe.rukamori.archivetune.constants.ContentLanguageKey
import moe.rukamori.archivetune.constants.CountryCodeToName
import moe.rukamori.archivetune.constants.LanguageCodeToName
import moe.rukamori.archivetune.constants.SYSTEM_DEFAULT
import moe.rukamori.archivetune.ui.screens.WebNavigator
import moe.rukamori.archivetune.ui.screens.YouTubeLoginWebView
import moe.rukamori.archivetune.utils.dataStore
import moe.rukamori.archivetune.utils.toPlaybackAuthState
import moe.rukamori.archivetune.viewmodels.LoginScreenState
import moe.rukamori.archivetune.viewmodels.LoginViewModel
import org.koin.compose.viewmodel.koinViewModel
import platform.UIKit.UIViewController

/** L'account di YouTube Music collegato. */
data class NamiAccount(
    val name: String,
    val email: String?,
)

/** Una scelta di un elenco (lingua, paese): codice e nome. */
data class NamiOption(
    val code: String,
    val name: String,
)

/**
 * Impostazioni per l'interfaccia SwiftUI: account (accesso con Google), qualità audio, lingua e
 * paese dei contenuti, spazio dei download. Swift lo usa come `NamiSettings.shared`.
 */
object NamiSettings {
    private val scope get() = NamiBridge.scope

    // region Account

    fun observeAccount(listener: (NamiAccount?) -> Unit): NamiSubscription =
        NamiBridge.collect(
            AppContext.dataStore.data
                .map { prefs ->
                    if (!prefs.toPlaybackAuthState().hasLoginCookie) {
                        null
                    } else {
                        NamiAccount(
                            name = prefs[AccountNameKey]?.takeIf(String::isNotBlank) ?: "Account YouTube",
                            email = prefs[AccountEmailKey]?.takeIf(String::isNotBlank),
                        )
                    }
                }.distinctUntilChanged(),
            listener,
        )

    /** Esce dall'account: Home e playlist tornano quelle senza account, i download restano. */
    fun logout() {
        App.Companion.forgetAccount(AppContext)
    }

    /**
     * La pagina di accesso di Google (la stessa dell'app Android): quando l'accesso riesce
     * chiama [onDone] e Swift chiude il foglio.
     */
    fun loginController(onDone: () -> Unit): UIViewController =
        ComposeUIViewController {
            LoginPage(onDone)
        }

    // endregion

    // region Riproduzione e contenuti

    private val qualities = listOf(AudioQuality.HIGHEST, AudioQuality.HIGH, AudioQuality.AUTO, AudioQuality.LOW)

    /** 0 massima, 1 alta, 2 automatica, 3 bassa. */
    fun audioQuality(): Int =
        runBlocking {
            val name = AppContext.dataStore.data.first()[AudioQualityKey]
            qualities.indexOfFirst { it.name == name }.takeIf { it >= 0 } ?: 0
        }

    fun setAudioQuality(index: Int) {
        val quality = qualities.getOrElse(index) { AudioQuality.HIGHEST }
        scope.launch { AppContext.dataStore.edit { it[AudioQualityKey] = quality.name } }
    }

    fun languages(): List<NamiOption> =
        listOf(NamiOption(SYSTEM_DEFAULT, "Predefinita del sistema")) + LanguageCodeToName.map { NamiOption(it.key, it.value) }

    fun countries(): List<NamiOption> =
        listOf(NamiOption(SYSTEM_DEFAULT, "Predefinito del sistema")) + CountryCodeToName.map { NamiOption(it.key, it.value) }

    fun contentLanguage(): String = runBlocking { AppContext.dataStore.data.first()[ContentLanguageKey] ?: SYSTEM_DEFAULT }

    fun contentCountry(): String = runBlocking { AppContext.dataStore.data.first()[ContentCountryKey] ?: SYSTEM_DEFAULT }

    fun setContentLanguage(code: String) {
        scope.launch { AppContext.dataStore.edit { it[ContentLanguageKey] = code } }
    }

    fun setContentCountry(code: String) {
        scope.launch { AppContext.dataStore.edit { it[ContentCountryKey] = code } }
    }

    // endregion

    // region Spazio

    /** Byte occupati dai brani scaricati. */
    fun downloadsSize(callback: (Long) -> Unit) {
        scope.launch {
            val bytes =
                withContext(Dispatchers.Default) {
                    File(AppContext.filesDir, "downloads").listFiles()?.sumOf { if (it.isFile) it.length() else 0L } ?: 0L
                }
            callback(bytes)
        }
    }

    fun removeAllDownloads() {
        NamiBridge.actions?.downloads?.removeAllDownloads()
    }

    fun version(): String = BuildConfig.VERSION_NAME

    // endregion
}

@Composable
private fun LoginPage(onDone: () -> Unit) {
    val viewModel: LoginViewModel = koinViewModel()
    val state by viewModel.screenState.collectAsState()
    val webNavigator = remember { WebNavigator() }
    LaunchedEffect(state) {
        if (state is LoginScreenState.Success) onDone()
    }
    Box(Modifier.fillMaxSize()) {
        YouTubeLoginWebView(
            startUrl = "https://accounts.google.com/ServiceLogin?continue=https%3A%2F%2Fmusic.youtube.com",
            navigator = webNavigator,
            onCookiesCaptured = viewModel::onCookiesCaptured,
            onVisitorData = viewModel::onVisitorDataExtracted,
            onDataSyncId = viewModel::onDataSyncIdExtracted,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
