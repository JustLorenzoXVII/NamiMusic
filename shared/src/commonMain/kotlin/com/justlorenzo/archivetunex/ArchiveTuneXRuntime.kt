package com.justlorenzo.archivetunex

import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import moe.rukamori.archivetune.compat.AppContext
import moe.rukamori.archivetune.compat.Timber
import moe.rukamori.archivetune.compat.plantArchiveTuneLogTrees
import moe.rukamori.archivetune.constants.AudioQuality
import moe.rukamori.archivetune.constants.AudioQualityKey
import moe.rukamori.archivetune.constants.ContentCountryKey
import moe.rukamori.archivetune.constants.ContentLanguageKey
import moe.rukamori.archivetune.constants.SYSTEM_DEFAULT
import moe.rukamori.archivetune.constants.VisitorDataKey
import moe.rukamori.archivetune.extensions.toEnum
import moe.rukamori.archivetune.innertube.models.YouTubeLocale
import moe.rukamori.archivetune.morideobfuscator.youtubei.YoutubeiAudioQuality
import com.justlorenzo.archivetunex.player.StreamResolver
import moe.rukamori.archivetune.db.MusicDatabase
import moe.rukamori.archivetune.di.archiveTuneCoreModule
import moe.rukamori.archivetune.di.archiveTuneInjectedModule
import moe.rukamori.archivetune.innertube.YouTube
import moe.rukamori.archivetune.playback.DownloadUtil
import moe.rukamori.archivetune.playback.MusicService
import moe.rukamori.archivetune.playback.PlayerConnection
import moe.rukamori.archivetune.utils.YTPlayerUtils
import moe.rukamori.archivetune.utils.dataStore
import moe.rukamori.archivetune.utils.toPlaybackAuthState
import org.koin.core.context.startKoin
import org.koin.mp.KoinPlatform

/**
 * Avvio del motore di ArchiveTuneX, uguale per Android e iPhone: log, Koin, account YouTube,
 * database, lettore e download. La nuova interfaccia usa [playerConnection], [database] e
 * [downloadUtil]; la vecchia interfaccia di ArchiveTune non viene più creata.
 */
object ArchiveTuneXRuntime {
    private var started = false
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    lateinit var database: MusicDatabase
        private set
    lateinit var downloadUtil: DownloadUtil
        private set
    lateinit var musicService: MusicService
        private set
    lateinit var playerConnection: PlayerConnection
        private set

    fun start() {
        if (started) return
        started = true
        plantArchiveTuneLogTrees()
        if (KoinPlatform.getKoinOrNull() == null) {
            startKoin {
                modules(archiveTuneCoreModule, archiveTuneInjectedModule)
            }
        }
        val koin = KoinPlatform.getKoin()
        database = koin.get()
        downloadUtil = koin.get()
        musicService = koin.get()
        playerConnection = PlayerConnection(musicService, database, scope)
        restoreYouTubeAccount()
        applyContentSettings()
    }

    /** Lingua/paese dei contenuti e qualità audio scelti nelle Impostazioni. */
    private fun applyContentSettings() {
        val dataStore = AppContext.dataStore
        val systemLocale = YouTube.locale
        scope.launch(Dispatchers.IO) {
            dataStore.data
                .map { prefs -> prefs[ContentLanguageKey] to prefs[ContentCountryKey] }
                .distinctUntilChanged()
                .collect { (language, country) ->
                    YouTube.locale =
                        YouTubeLocale(
                            gl = country?.takeIf { it != SYSTEM_DEFAULT } ?: systemLocale.gl,
                            hl = language?.takeIf { it != SYSTEM_DEFAULT } ?: systemLocale.hl,
                        )
                }
        }
        scope.launch(Dispatchers.IO) {
            dataStore.data
                .map { it[AudioQualityKey].toEnum(AudioQuality.HIGHEST) }
                .distinctUntilChanged()
                .collect { quality ->
                    StreamResolver.audioQuality =
                        when (quality) {
                            AudioQuality.AUTO -> YoutubeiAudioQuality.AUTO
                            AudioQuality.HIGH -> YoutubeiAudioQuality.HIGH
                            AudioQuality.HIGHEST -> YoutubeiAudioQuality.HIGHEST
                            AudioQuality.LOW -> YoutubeiAudioQuality.LOW
                        }
                }
        }
    }

    /**
     * Come App.onCreate dell'originale: l'account (cookie, visitorData, dataSyncId) salvato nelle
     * preferenze viene passato a YouTube, e si aggiorna a ogni login o logout.
     */
    private fun restoreYouTubeAccount() {
        val dataStore = AppContext.dataStore
        scope.launch(Dispatchers.IO) {
            dataStore.data
                .map { it.toPlaybackAuthState() }
                .distinctUntilChanged()
                .collect { authState ->
                    val previousFingerprint = YouTube.currentPlaybackAuthState().fingerprint
                    YouTube.authState = authState
                    if (previousFingerprint != authState.fingerprint) {
                        YTPlayerUtils.clearPlaybackAuthCaches()
                    }
                }
        }
        scope.launch(Dispatchers.IO) {
            dataStore.data
                .map { it.toPlaybackAuthState().visitorData }
                .distinctUntilChanged()
                .collect { visitorData ->
                    if (!visitorData.isNullOrBlank()) return@collect
                    YouTube
                        .visitorData()
                        .onFailure { Timber.tag("Runtime").w(it, "visitorData non disponibile") }
                        .getOrNull()
                        ?.let { newVisitorData ->
                            dataStore.edit { settings -> settings[VisitorDataKey] = newVisitorData }
                        }
                }
        }
    }
}
