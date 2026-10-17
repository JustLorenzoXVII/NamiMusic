/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.rukamori.archivetune.canvas

import kotlin.concurrent.Volatile

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import moe.rukamori.archivetune.compat.okhttp3.Call
import moe.rukamori.archivetune.compat.okhttp3.Interceptor
import moe.rukamori.archivetune.compat.okhttp3.Response
import kotlinx.io.IOException
import moe.rukamori.archivetune.compat.synchronized
import moe.rukamori.archivetune.compat.Synchronized

object CanvasNetworkAccess {
    private val mutablePolicy = MutableStateFlow(CanvasPolicy())
    val policy = mutablePolicy.asStateFlow()
    private val calls = LinkedHashMap<Call, CanvasSource>()

    @Volatile
    internal var connectivity: () -> CanvasConnectivity = { CanvasConnectivity() }

    @Synchronized
    internal fun update(policy: CanvasPolicy) {
        mutablePolicy.value = policy
        calls.filterValues { source -> !policy.networkAllowed || !policy.configuration.source.accepts(source) }
            .keys.toList().forEach(Call::cancel)
    }

    fun check(source: CanvasSource? = null) {
        requireAllowed(policy.value.copy(connectivity = connectivity()), source)
    }

    fun checkTransfer(source: CanvasSource) {
        requireAllowed(policy.value, source)
    }

    private fun requireAllowed(current: CanvasPolicy, source: CanvasSource?) {
        if (!current.networkAllowed || (source != null && !current.configuration.source.accepts(source))) {
            throw IOException("Canvas network access is disabled by the current policy")
        }
    }

    fun intercept(chain: Interceptor.Chain, source: CanvasSource): Response {
        val call = chain.call()
        synchronized(this) {
            check(source)
            calls[call] = source
        }
        try {
            val response = chain.proceed(chain.request())
            val body = response.body
            if (body == null) {
                remove(call)
                return response
            }
            // ArchiveTuneX: il client HTTP comune scarica la risposta intera, quindi il controllo
            // si fa una volta a fine trasferimento invece che a ogni blocco letto (ForwardingSource di okio).
            try {
                checkTransfer(source)
            } finally {
                remove(call)
            }
            return response
        } catch (error: Exception) {
            remove(call)
            throw error
        }
    }

    @Synchronized
    private fun remove(call: Call) {
        calls.remove(call)
    }
}
