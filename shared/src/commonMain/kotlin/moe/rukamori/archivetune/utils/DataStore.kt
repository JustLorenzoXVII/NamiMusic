/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.rukamori.archivetune.utils

import kotlinx.coroutines.IO

import kotlin.concurrent.Volatile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.datastore.core.DataMigration
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import moe.rukamori.archivetune.compat.Context
import moe.rukamori.archivetune.compat.LocalContext
import moe.rukamori.archivetune.compat.isMainThread
import moe.rukamori.archivetune.compat.platformFilesDirectory
import okio.Path.Companion.toPath
import kotlin.concurrent.Volatile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import moe.rukamori.archivetune.constants.HISTORY_DURATION_LEGACY_FLOAT_KEY
import moe.rukamori.archivetune.constants.HISTORY_DURATION_MAX
import moe.rukamori.archivetune.constants.HISTORY_DURATION_MIN
import moe.rukamori.archivetune.constants.HistoryDuration
import moe.rukamori.archivetune.constants.UpdateChannel
import moe.rukamori.archivetune.constants.UpdateChannelKey
import moe.rukamori.archivetune.extensions.toEnum
import kotlin.properties.ReadOnlyProperty

/**
 * Le impostazioni dell'app (DataStore "settings", come l'originale).
 * Su Android il file è lo stesso di preferencesDataStore("settings"): files/datastore/settings.preferences_pb.
 */
private object SettingsDataStore {
    val instance: DataStore<Preferences> by lazy {
        PreferenceDataStoreFactory.createWithPath(
            migrations =
                listOf(
                            object : DataMigration<Preferences> {
                                override suspend fun shouldMigrate(currentData: Preferences): Boolean =
                                    currentData[HISTORY_DURATION_LEGACY_FLOAT_KEY] != null &&
                                        currentData[HistoryDuration] == null
                
                                override suspend fun migrate(currentData: Preferences): Preferences =
                                    currentData.toMutablePreferences().apply {
                                        val oldFloat = currentData[HISTORY_DURATION_LEGACY_FLOAT_KEY]
                                        if (oldFloat != null) {
                                            this[HistoryDuration] =
                                                oldFloat
                                                    .toInt()
                                                    .coerceIn(HISTORY_DURATION_MIN, HISTORY_DURATION_MAX)
                                            this.remove(HISTORY_DURATION_LEGACY_FLOAT_KEY)
                                        }
                                    }
                
                                override suspend fun cleanUp() {}
                            },
                            object : DataMigration<Preferences> {
                                override suspend fun shouldMigrate(currentData: Preferences): Boolean =
                                    when (currentData[UpdateChannelKey]) {
                                        "NIGHTLY", "DAILY_NIGHTLY", "CANARY" -> true
                                        else -> false
                                    }
                
                                override suspend fun migrate(currentData: Preferences): Preferences =
                                    currentData.toMutablePreferences().apply {
                                        this[UpdateChannelKey] = UpdateChannel.ARTIFACT.name
                                    }
                
                                override suspend fun cleanUp() {}
                            },
                        ),
            produceFile = { (platformFilesDirectory().trimEnd('/') + "/datastore/settings.preferences_pb").toPath() },
        )
    }
}

val Context.dataStore: DataStore<Preferences>
    get() = SettingsDataStore.instance

object PreferenceStore {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _prefs = MutableStateFlow<Preferences?>(null)

    @Volatile private var started = false

    fun start(context: Context) {
        if (started) return
        run {
            started = true
            scope.launch {
                context.dataStore.data.collect { preferences ->
                    _prefs.value = preferences
                }
            }
        }
    }

    fun <T> get(key: Preferences.Key<T>): T? = _prefs.value?.get(key)

    fun launchEdit(
        dataStore: DataStore<Preferences>,
        block: MutablePreferences.() -> Unit,
    ) {
        scope.launch {
            dataStore.edit { prefs ->
                prefs.block()
            }
        }
    }
}

operator fun <T> DataStore<Preferences>.get(key: Preferences.Key<T>): T? =
    PreferenceStore.get(key)
        ?: if (isMainThread()) {
            null
        } else {
            runBlocking(Dispatchers.IO) {
                withTimeoutOrNull(1500) {
                    data.first()[key]
                }
            }
        }

fun <T> DataStore<Preferences>.get(
    key: Preferences.Key<T>,
    defaultValue: T,
): T =
    PreferenceStore.get(key)
        ?: if (isMainThread()) {
            defaultValue
        } else {
            runBlocking(Dispatchers.IO) {
                withTimeoutOrNull(1500) {
                    data.first()[key]
                } ?: defaultValue
            }
        }

suspend fun <T> DataStore<Preferences>.getAsync(key: Preferences.Key<T>): T? = data.first()[key]

suspend fun <T> DataStore<Preferences>.getAsync(
    key: Preferences.Key<T>,
    defaultValue: T,
): T = data.first()[key] ?: defaultValue

fun <T> preference(
    context: Context,
    key: Preferences.Key<T>,
    defaultValue: T,
) = ReadOnlyProperty<Any?, T> { _, _ -> context.dataStore[key] ?: defaultValue }

inline fun <reified T : Enum<T>> enumPreference(
    context: Context,
    key: Preferences.Key<String>,
    defaultValue: T,
) = ReadOnlyProperty<Any?, T> { _, _ -> context.dataStore[key].toEnum(defaultValue) }

@Composable
fun <T> rememberPreference(
    key: Preferences.Key<T>,
    defaultValue: T,
): MutableState<T> {
    val context = LocalContext.current

    val state =
        remember {
            context.dataStore.data
                .map { it[key] ?: defaultValue }
                .distinctUntilChanged()
        }.collectAsState(defaultValue)

    return remember {
        object : MutableState<T> {
            override var value: T
                get() = state.value
                set(value) {
                    PreferenceStore.launchEdit(context.dataStore) {
                        this[key] = value
                    }
                }

            override fun component1() = value

            override fun component2(): (T) -> Unit = { value = it }
        }
    }
}

@Composable
inline fun <reified T : Enum<T>> rememberEnumPreference(
    key: Preferences.Key<String>,
    defaultValue: T,
): MutableState<T> {
    val context = LocalContext.current

    val state =
        remember {
            context.dataStore.data
                .map { it[key].toEnum(defaultValue = defaultValue) }
                .distinctUntilChanged()
        }.collectAsState(defaultValue)

    return remember {
        object : MutableState<T> {
            override var value: T
                get() = state.value
                set(value) {
                    PreferenceStore.launchEdit(context.dataStore) {
                        this[key] = value.name
                    }
                }

            override fun component1() = value

            override fun component2(): (T) -> Unit = { value = it }
        }
    }
}
