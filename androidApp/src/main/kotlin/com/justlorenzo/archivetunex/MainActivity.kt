package com.justlorenzo.archivetunex

import android.os.Bundle
import android.view.View
import android.view.ViewTreeObserver
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.justlorenzo.archivetunex.player.initArchiveTuneX
import moe.rukamori.archivetune.compat.SystemBars

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        initArchiveTuneX(this)
        SystemBars.attach(this)

        setContent {
            App()
        }

        // La splash di sistema resta finché l'app non ha letto le impostazioni e disegnato il primo
        // fotogramma con tema, colori e lingua giusti: così non si vede nessun cambio di colore all'avvio.
        val content = findViewById<View>(android.R.id.content)
        content.viewTreeObserver.addOnPreDrawListener(
            object : ViewTreeObserver.OnPreDrawListener {
                override fun onPreDraw(): Boolean =
                    if (AppStartup.ready) {
                        content.viewTreeObserver.removeOnPreDrawListener(this)
                        true
                    } else {
                        false
                    }
            },
        )
    }
}
