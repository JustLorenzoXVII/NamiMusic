package com.justlorenzo.archivetunex

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.network.ktor3.KtorNetworkFetcherFactory
import coil3.request.crossfade
import moe.rukamori.archivetune.downloads.DownloadedArtworkRepository
import moe.rukamori.archivetune.innertube.network.createPlatformHttpClient
import org.koin.mp.KoinPlatform

/**
 * L'interfaccia di ArchiveTune, la stessa su Android e iPhone.
 * (La vecchia schermata di prova è stata sostituita dall'interfaccia originale.)
 */
@Composable
fun App() {
    val host = remember { ArchiveTuneXRuntime.start() }

    // Come newImageLoader() dell'originale: le copertine si scaricano con il client di rete
    // della piattaforma (OkHttp su Android, Darwin su iPhone). Senza questo Coil sceglieva da
    // solo il motore CIO, che su iPhone non supporta HTTPS, e le copertine restavano vuote.
    setSingletonImageLoaderFactory { context ->
        val downloadedArtwork = KoinPlatform.getKoin().get<DownloadedArtworkRepository>()
        ImageLoader
            .Builder(context)
            .crossfade(true)
            .components {
                add(KtorNetworkFetcherFactory(httpClient = { createPlatformHttpClient(proxy = null, dnsOverHttpsUrl = null) {} }))
                add(downloadedArtwork.coilMapper())
            }.build()
    }

    host.Content()
}
