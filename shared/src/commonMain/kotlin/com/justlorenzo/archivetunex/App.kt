package com.justlorenzo.archivetunex

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/**
 * L'interfaccia di ArchiveTune, la stessa su Android e iPhone.
 * (La vecchia schermata di prova è stata sostituita dall'interfaccia originale.)
 */
@Composable
fun App() {
    val host = remember { ArchiveTuneXRuntime.start() }
    host.Content()
}
