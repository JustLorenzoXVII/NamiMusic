package com.justlorenzo.archivetunex.player

import android.content.Intent
import androidx.media3.common.Player
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

/**
 * Servizio di riproduzione: tiene la musica attiva in background e mostra
 * la notifica con i controlli (anche su schermata di blocco, cuffie e Android Auto in futuro).
 */
class PlaybackService : MediaSessionService() {
    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        // Il servizio può partire anche prima dell'app (es. ripresa dalla notifica):
        // in quel caso prepara il contesto e crea il lettore.
        initArchiveTuneX(this)
        com.justlorenzo.archivetunex.player.Player.controller
        val player = sessionPlayer ?: return
        mediaSession = MediaSession.Builder(this, player).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        // Se la musica è in pausa e l'app viene chiusa dalle recenti, si ferma anche il servizio.
        val player = mediaSession?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        mediaSession?.release()
        mediaSession = null
        super.onDestroy()
    }

    companion object {
        /** Impostato da [AndroidAudioEngine] quando nasce il lettore. */
        internal var sessionPlayer: Player? = null
    }
}
