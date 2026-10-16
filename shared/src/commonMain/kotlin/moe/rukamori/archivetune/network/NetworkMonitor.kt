/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.rukamori.archivetune.network


import moe.rukamori.archivetune.compat.Context
import kotlinx.coroutines.channels.awaitClose
import moe.rukamori.archivetune.compat.NetworkState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

internal interface NetworkEvents<T> {
    fun onCapabilitiesChanged(
        network: T,
        isValidatedInternet: Boolean,
    )

    fun onLost(network: T)
}

internal class ValidatedNetworkRegistry<T>(
    initialValidatedNetworks: Set<T> = emptySet(),
) {
    private val validatedNetworks = initialValidatedNetworks.toMutableSet()

    fun isOnline(): Boolean = validatedNetworks.isNotEmpty()

    fun onCapabilitiesChanged(
        network: T,
        isValidatedInternet: Boolean,
    ): Boolean {
        if (isValidatedInternet) {
            validatedNetworks += network
        } else {
            validatedNetworks -= network
        }
        return isOnline()
    }

    fun onLost(network: T): Boolean {
        validatedNetworks -= network
        return isOnline()
    }
}

internal fun <T> validatedNetworkStatusFlow(
    initialValidatedNetworks: Set<T> = emptySet(),
    register: (NetworkEvents<T>) -> AutoCloseable,
): Flow<Boolean> =
    callbackFlow {
        val registry = ValidatedNetworkRegistry(initialValidatedNetworks)
        trySend(registry.isOnline())

        val handle =
            register(
                object : NetworkEvents<T> {
                    override fun onCapabilitiesChanged(
                        network: T,
                        isValidatedInternet: Boolean,
                    ) {
                        trySend(registry.onCapabilitiesChanged(network, isValidatedInternet))
                    }

                    override fun onLost(network: T) {
                        trySend(registry.onLost(network))
                    }
                },
            )

        awaitClose {
            handle.close()
        }
    }.distinctUntilChanged()

class NetworkMonitor constructor(
    context: Context,
) {
    /** In linea / non in linea (ConnectivityManager su Android; su iPhone sempre in linea). */
    val isOnline: Flow<Boolean> =
        validatedNetworkStatusFlow(initialValidatedNetworks = if (NetworkState.isConnected()) setOf(Unit) else emptySet()) { events ->
            val stop =
                NetworkState.observe {
                    events.onCapabilitiesChanged(Unit, NetworkState.isConnected())
                }
            AutoCloseable { stop() }
        }
}
