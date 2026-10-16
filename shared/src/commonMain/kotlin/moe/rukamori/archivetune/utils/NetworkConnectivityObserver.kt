/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.rukamori.archivetune.utils

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import moe.rukamori.archivetune.compat.Context
import moe.rukamori.archivetune.compat.NetworkState

/**
 * Simple NetworkConnectivityObserver based on OuterTune's implementation
 * Provides network connectivity monitoring for auto-play functionality
 */
class NetworkConnectivityObserver(
    context: Context,
) {
    private val _networkStatus = Channel<Boolean>(Channel.CONFLATED)
    val networkStatus = _networkStatus.receiveAsFlow()

    private val stop: () -> Unit = NetworkState.observe { _networkStatus.trySend(isCurrentlyConnected()) }

    init {
        _networkStatus.trySend(isCurrentlyConnected())
    }

    fun unregister() = stop()

    /** Check current connectivity state synchronously */
    fun isCurrentlyConnected(): Boolean = NetworkState.isConnected()
}
