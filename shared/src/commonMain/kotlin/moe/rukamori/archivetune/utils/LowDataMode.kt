/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.rukamori.archivetune.utils

import moe.rukamori.archivetune.compat.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import moe.rukamori.archivetune.compat.LocalContext
import moe.rukamori.archivetune.constants.LowDataModeKey
import moe.rukamori.archivetune.compat.NetworkState

fun Context.isLowDataModeActive(): Boolean = isLowDataModeActive(dataStore.get(LowDataModeKey, false))

fun Context.isLowDataModeActive(enabled: Boolean): Boolean {
    if (!enabled) return false
    return NetworkState.isMetered() || NetworkState.isCellular()
}

@Composable
fun rememberLowDataModeActive(): Boolean {
    val enabled by rememberPreference(LowDataModeKey, false)
    return rememberLowDataModeActive(enabled)
}

@Composable
fun rememberLowDataModeActive(enabled: Boolean): Boolean {
    val context = LocalContext.current
    var active by remember(context, enabled) {
        mutableStateOf(context.isLowDataModeActive(enabled))
    }

    DisposableEffect(context, enabled) {
        if (!enabled) {
            active = false
            return@DisposableEffect onDispose {}
        }
        active = context.isLowDataModeActive(enabled)
        val stop = NetworkState.observe { active = context.isLowDataModeActive(enabled) }
        onDispose { stop() }
    }

    return active
}
