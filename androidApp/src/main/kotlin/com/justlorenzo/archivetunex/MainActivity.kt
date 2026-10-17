package com.justlorenzo.archivetunex

import android.os.Bundle
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
    }
}
