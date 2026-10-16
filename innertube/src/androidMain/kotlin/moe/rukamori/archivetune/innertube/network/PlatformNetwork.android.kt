package moe.rukamori.archivetune.innertube.network

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.compression.ContentEncoding
import moe.rukamori.archivetune.innertube.models.YouTubeLocale
import okhttp3.Credentials
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.dnsoverhttps.DnsOverHttps
import java.net.InetSocketAddress
import java.net.Proxy
import java.util.Locale

actual fun createPlatformHttpClient(
    proxy: ProxySettings?,
    dnsOverHttpsUrl: String?,
    configure: HttpClientConfig<*>.() -> Unit,
): HttpClient =
    HttpClient(OkHttp) {
        engine {
            config {
                if (!dnsOverHttpsUrl.isNullOrBlank()) {
                    dns(createDnsOverHttps(dnsOverHttpsUrl))
                }
                if (proxy == null) {
                    proxy(Proxy.NO_PROXY)
                } else {
                    proxy(proxy.toJavaProxy())
                    val username = proxy.username
                    val password = proxy.password
                    if (!username.isNullOrBlank() && !password.isNullOrBlank()) {
                        proxyAuthenticator { _, response ->
                            response.request
                                .newBuilder()
                                .header("Proxy-Authorization", Credentials.basic(username, password))
                                .build()
                        }
                    }
                }
            }
        }
        install(ContentEncoding) {
            gzip(0.9F)
            deflate(0.8F)
        }
        configure()
    }

internal actual fun systemYouTubeLocale(): YouTubeLocale =
    YouTubeLocale(
        gl = Locale.getDefault().country,
        hl = Locale.getDefault().toLanguageTag(),
    )

/** Converte le impostazioni comuni nel Proxy di Java, per OkHttp ed ExoPlayer. */
fun ProxySettings.toJavaProxy(): Proxy =
    Proxy(
        when (type) {
            ProxySettings.Type.HTTP -> Proxy.Type.HTTP
            ProxySettings.Type.SOCKS -> Proxy.Type.SOCKS
        },
        InetSocketAddress.createUnresolved(host, port),
    )

private fun createDnsOverHttps(url: String): DnsOverHttps =
    DnsOverHttps
        .Builder()
        .client(OkHttpClient.Builder().build())
        .url(url.toHttpUrl())
        .build()
