/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.rukamori.archivetune.androidauto

import moe.rukamori.archivetune.compat.NetworkState

import moe.rukamori.archivetune.compat.Manifest
import moe.rukamori.archivetune.compat.Context
import moe.rukamori.archivetune.compat.PackageManager
import moe.rukamori.archivetune.compat.packageManager
import moe.rukamori.archivetune.compat.Build
import moe.rukamori.archivetune.compat.ContextCompat
import moe.rukamori.archivetune.compat.getSystemService
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import moe.rukamori.archivetune.constants.AndroidAutoLocalSongsKey
import moe.rukamori.archivetune.constants.AndroidAutoMeteredArtworkKey
import moe.rukamori.archivetune.constants.AndroidAutoMeteredPlaybackKey
import moe.rukamori.archivetune.constants.AndroidAutoOnlineRecommendationsKey
import moe.rukamori.archivetune.constants.AndroidAutoOnlineVoiceSearchKey
import moe.rukamori.archivetune.constants.AndroidAutoPrimaryActionKey
import moe.rukamori.archivetune.constants.AndroidAutoSecondaryActionKey
import moe.rukamori.archivetune.utils.dataStore
import moe.rukamori.archivetune.utils.get

class AndroidAutoSettingsRepository constructor(
    private val context: Context,
) {

    val configuration: Flow<AndroidAutoConfiguration> = context.dataStore.data.map { preferences ->
        AndroidAutoConfiguration(
            onlineRecommendations = preferences[AndroidAutoOnlineRecommendationsKey] ?: true,
            onlineVoiceSearch = preferences[AndroidAutoOnlineVoiceSearchKey] ?: true,
            localSongs = preferences[AndroidAutoLocalSongsKey] ?: true,
            meteredPlayback = preferences[AndroidAutoMeteredPlaybackKey] ?: true,
            meteredArtwork = preferences[AndroidAutoMeteredArtworkKey] ?: true,
            primaryAction = AndroidAutoCustomAction.fromPreference(
                preferences[AndroidAutoPrimaryActionKey],
                AndroidAutoCustomAction.LIKE,
            ),
            secondaryAction = AndroidAutoCustomAction.fromPreference(
                preferences[AndroidAutoSecondaryActionKey],
                AndroidAutoCustomAction.START_RADIO,
            ),
        )
    }.distinctUntilChanged()

    // Android Auto non esiste su iPhone (l'equivalente è CarPlay): mai collegato.
    val connectionStatus: Flow<AndroidAutoConnectionStatus> = kotlinx.coroutines.flow.flowOf(AndroidAutoConnectionStatus.DISCONNECTED)

    val networkState: Flow<AndroidAutoNetworkState> = callbackFlow {
        trySend(currentNetworkState())
        val stop = NetworkState.observe { trySend(currentNetworkState()) }
        awaitClose { stop() }
    }.distinctUntilChanged()

    fun currentConfiguration(): AndroidAutoConfiguration = AndroidAutoConfiguration(
        onlineRecommendations = context.dataStore.get(AndroidAutoOnlineRecommendationsKey, true),
        onlineVoiceSearch = context.dataStore.get(AndroidAutoOnlineVoiceSearchKey, true),
        localSongs = context.dataStore.get(AndroidAutoLocalSongsKey, true),
        meteredPlayback = context.dataStore.get(AndroidAutoMeteredPlaybackKey, true),
        meteredArtwork = context.dataStore.get(AndroidAutoMeteredArtworkKey, true),
        primaryAction = AndroidAutoCustomAction.fromPreference(
            context.dataStore[AndroidAutoPrimaryActionKey],
            AndroidAutoCustomAction.LIKE,
        ),
        secondaryAction = AndroidAutoCustomAction.fromPreference(
            context.dataStore[AndroidAutoSecondaryActionKey],
            AndroidAutoCustomAction.START_RADIO,
        ),
    )

    fun currentNetworkState(): AndroidAutoNetworkState =
        AndroidAutoNetworkState(
            online = NetworkState.isConnected(),
            metered = NetworkState.isMetered() || NetworkState.isCellular(),
        )

    fun hasLocalAudioPermission(): Boolean = ContextCompat.checkSelfPermission(
        context,
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        },
    ) == PackageManager.PERMISSION_GRANTED

    suspend fun setOnlineRecommendations(enabled: Boolean) = edit(AndroidAutoOnlineRecommendationsKey, enabled)
    suspend fun setOnlineVoiceSearch(enabled: Boolean) = edit(AndroidAutoOnlineVoiceSearchKey, enabled)
    suspend fun setLocalSongs(enabled: Boolean) = edit(AndroidAutoLocalSongsKey, enabled)
    suspend fun setMeteredPlayback(enabled: Boolean) = edit(AndroidAutoMeteredPlaybackKey, enabled)
    suspend fun setMeteredArtwork(enabled: Boolean) = edit(AndroidAutoMeteredArtworkKey, enabled)

    suspend fun setActions(primary: AndroidAutoCustomAction, secondary: AndroidAutoCustomAction) {
        context.dataStore.edit { preferences ->
            preferences[AndroidAutoPrimaryActionKey] = primary.name
            preferences[AndroidAutoSecondaryActionKey] = secondary.name
        }
    }

    private suspend fun <T> edit(key: androidx.datastore.preferences.core.Preferences.Key<T>, value: T) {
        context.dataStore.edit { it[key] = value }
    }

}

