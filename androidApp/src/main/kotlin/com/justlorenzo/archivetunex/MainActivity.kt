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
        MainThreadWatchdog.start()
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

/**
 * Sentinella: se il thread dello schermo resta bloccato più di 2 secondi, scrive nel Logcat
 * (filtro "NamiBlocco") il punto esatto del codice in cui è fermo. Serve a trovare i blocchi "non risponde".
 */
private object MainThreadWatchdog {
    @Volatile private var started = false

    fun start() {
        if (started) return
        started = true
        val main = android.os.Handler(android.os.Looper.getMainLooper())
        Thread({
            while (true) {
                val answered = java.util.concurrent.atomic.AtomicBoolean(false)
                main.post { answered.set(true) }
                Thread.sleep(2_000)
                if (!answered.get()) {
                    val stack = android.os.Looper.getMainLooper().thread.stackTrace.joinToString("\n") { "    at $it" }
                    android.util.Log.e("NamiBlocco", "Schermo bloccato da più di 2 secondi. Punto in cui è fermo:\n$stack")
                    // Aspetta che si sblocchi prima di controllare di nuovo (un solo messaggio per blocco)
                    while (!answered.get()) Thread.sleep(500)
                }
            }
        }, "NamiWatchdog").apply { isDaemon = true }.start()
    }
}
