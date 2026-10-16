package moe.rukamori.archivetune.compat

import android.app.ActivityManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import com.justlorenzo.archivetunex.player.AndroidAppContext

actual object NetworkState {
    private val manager: ConnectivityManager?
        get() = AndroidAppContext.context.getSystemService(ConnectivityManager::class.java)

    private fun capabilities(): NetworkCapabilities? = manager?.let { m -> m.activeNetwork?.let(m::getNetworkCapabilities) }

    actual fun isConnected(): Boolean {
        val caps = capabilities() ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) ||
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    actual fun isMetered(): Boolean = manager?.isActiveNetworkMetered == true

    actual fun isCellular(): Boolean = capabilities()?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true

    actual fun observe(onChange: () -> Unit): () -> Unit {
        val m = manager ?: return {}
        val callback =
            object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) = onChange()

                override fun onCapabilitiesChanged(
                    network: Network,
                    networkCapabilities: NetworkCapabilities,
                ) = onChange()

                override fun onLost(network: Network) = onChange()
            }
        val request = NetworkRequest.Builder().addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET).build()
        return runCatching {
            m.registerNetworkCallback(request, callback)
            val stop: () -> Unit = { runCatching { m.unregisterNetworkCallback(callback) } }
            stop
        }.getOrDefault {}
    }
}

actual fun platformIsLowRamDevice(): Boolean =
    (AndroidAppContext.context.getSystemService(android.content.Context.ACTIVITY_SERVICE) as? ActivityManager)?.isLowRamDevice == true
