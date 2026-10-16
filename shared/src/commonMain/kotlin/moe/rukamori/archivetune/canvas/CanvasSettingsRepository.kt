/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.rukamori.archivetune.canvas

import moe.rukamori.archivetune.compat.NetworkState

import kotlinx.coroutines.IO

import moe.rukamori.archivetune.compat.Context
import moe.rukamori.archivetune.compat.getSystemService
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import moe.rukamori.archivetune.constants.ArchiveTuneCanvasKey
import moe.rukamori.archivetune.constants.CanvasSourceKey
import moe.rukamori.archivetune.constants.CanvasWifiOnlyKey
import moe.rukamori.archivetune.constants.LowDataModeKey
import moe.rukamori.archivetune.constants.MaxCanvasCacheSizeKey
import moe.rukamori.archivetune.ui.player.CanvasArtworkPlaybackCache
import moe.rukamori.archivetune.utils.dataStore
import kotlinx.io.IOException

class CanvasSettingsRepository constructor(
    private val context: Context,
    private val spotifyCanvas: SpotifyCanvasRepository,
) {

    val spotifyConnected = spotifyCanvas.connected

    val configuration: Flow<CanvasConfiguration> = context.dataStore.data.map { preferences ->
        CanvasConfiguration(
            enabled = preferences[ArchiveTuneCanvasKey] ?: false,
            source = CanvasSource.fromPreference(preferences[CanvasSourceKey]),
            wifiOnly = preferences[CanvasWifiOnlyKey] ?: false,
            cacheLimitMb = (preferences[MaxCanvasCacheSizeKey] ?: 256).coerceAtLeast(-1),
            lowDataMode = preferences[LowDataModeKey] ?: false,
        )
    }.distinctUntilChanged()

    val connectivity: Flow<CanvasConnectivity> = callbackFlow {
        trySend(currentConnectivity())
        val stop = NetworkState.observe { trySend(currentConnectivity()) }
        awaitClose { stop() }
    }.distinctUntilChanged()

    fun currentConnectivity(): CanvasConnectivity =
        CanvasConnectivity(
            online = NetworkState.isConnected(),
            wifi = NetworkState.isConnected() && !NetworkState.isCellular(),
            metered = NetworkState.isMetered() || NetworkState.isCellular(),
        )

    suspend fun setEnabled(enabled: Boolean) {
        context.dataStore.edit { it[ArchiveTuneCanvasKey] = enabled }
    }

    suspend fun setSource(source: CanvasSource) {
        context.dataStore.edit { it[CanvasSourceKey] = source.name }
    }

    suspend fun setWifiOnly(wifiOnly: Boolean) {
        context.dataStore.edit { it[CanvasWifiOnlyKey] = wifiOnly }
    }

    suspend fun setCacheLimit(limitMb: Int) {
        context.dataStore.edit { it[MaxCanvasCacheSizeKey] = limitMb }
    }

    suspend fun initializeCache() = withContext(Dispatchers.IO) {
        CanvasArtworkPlaybackCache.init(context, configuration.first().cacheLimitMb)
    }

    suspend fun applyCacheLimit(limitMb: Int) = withContext(Dispatchers.IO) {
        CanvasArtworkPlaybackCache.setMaxSize(limitMb)
    }

    fun cancelDownloads() = CanvasArtworkPlaybackCache.cancelDownloads()

    suspend fun cacheBytes(): Long = withContext(Dispatchers.IO) {
        CanvasArtworkPlaybackCache.byteSize()
    }

    suspend fun clearCache() = withContext(Dispatchers.IO) {
        if (!CanvasArtworkPlaybackCache.clearAndPersist()) throw IOException("Canvas cache could not be cleared")
    }

    suspend fun isHealthy(source: CanvasSource): Boolean = withContext(Dispatchers.IO) {
        when (source) {
            CanvasSource.BETTER_LYRICS -> ArchiveTuneCanvas.isHealthy()
            CanvasSource.APPLE_MUSIC -> AppleMusicProvider.isHealthy()
            CanvasSource.TIDAL -> TidalCanvasProvider.isHealthy()
            CanvasSource.SPOTIFY -> spotifyCanvas.isHealthy()
            CanvasSource.ALL -> error("Health checks require one provider")
        }
    }
}

