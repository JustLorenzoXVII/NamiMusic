/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.rukamori.archivetune.innertube

import io.ktor.client.plugins.api.createClientPlugin
import kotlinx.io.IOException
import kotlin.concurrent.Volatile

/**
 * Interruttore per bloccare tutte le richieste verso YouTube.
 *
 * ArchiveTuneX: nell'originale parte bloccato finché il server di ArchiveTune non verifica
 * che la build sia ufficiale. Questa è una build personale, quindi parte sbloccato.
 * Era un Interceptor di OkHttp; qui è un plugin di Ktor, che funziona anche su iOS.
 */
object NetworkGatekeeper {
    @Volatile
    private var connectionBlocked = false

    val isConnectionBlocked: Boolean
        get() = connectionBlocked

    fun setConnectionBlocked(blocked: Boolean) {
        connectionBlocked = blocked
    }

    internal val Plugin =
        createClientPlugin("NetworkGatekeeper") {
            onRequest { _, _ ->
                if (connectionBlocked) {
                    throw IOException("Connessione bloccata: NetworkGatekeeper è attivo.")
                }
            }
        }
}
