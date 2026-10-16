package moe.rukamori.archivetune.innertube

import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.CancellationException
import moe.rukamori.archivetune.innertube.network.createPlatformHttpClient

/**
 * Controlla in anticipo se googlevideo accetta un URL audio: scarica solo i primi 2 byte.
 * Così il lettore può scartare i link che darebbero 403 e provare un altro client.
 */
object StreamProbe {
    private val client by lazy {
        createPlatformHttpClient(proxy = null, dnsOverHttpsUrl = null) {
            expectSuccess = false
            followRedirects = true
        }
    }

    /** Codice HTTP della prova (200/206 = ok), oppure -1 se la richiesta non è partita. */
    suspend fun status(
        url: String,
        userAgent: String,
    ): Int =
        try {
            val response: HttpResponse =
                client.get(url) {
                    header(HttpHeaders.UserAgent, userAgent)
                    header(HttpHeaders.Range, "bytes=0-1")
                }
            response.status.value
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Exception) {
            -1
        }
}
