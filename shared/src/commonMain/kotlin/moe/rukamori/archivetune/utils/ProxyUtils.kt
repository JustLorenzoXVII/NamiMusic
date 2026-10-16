/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.rukamori.archivetune.utils

import moe.rukamori.archivetune.compat.Proxy
import moe.rukamori.archivetune.innertube.YouTube
import moe.rukamori.archivetune.innertube.network.ProxySettings

object ProxyUtils {
    fun createProxyOrNull(
        type: Proxy.Type,
        host: String?,
        port: Int?,
        username: String? = null,
        password: String? = null,
    ): ProxySettings? {
        val resolvedHost = host?.trim()?.takeIf(String::isNotEmpty) ?: return null
        val resolvedPort = port?.takeIf { it in 1..65535 } ?: return null
        val resolvedType =
            when (type) {
                Proxy.Type.SOCKS -> ProxySettings.Type.SOCKS
                else -> ProxySettings.Type.HTTP
            }
        return ProxySettings(resolvedType, resolvedHost, resolvedPort, username?.takeIf(String::isNotBlank), password?.takeIf(String::isNotBlank))
    }

    fun applyYouTubeProxy(
        enabled: Boolean,
        type: Proxy.Type,
        host: String?,
        port: Int?,
        username: String?,
        password: String?,
    ) {
        YouTube.proxy = if (enabled) createProxyOrNull(type, host, port, username, password) else null
    }
}
