package com.justlorenzo.archivetunex

import moe.rukamori.archivetune.MainActivity
import moe.rukamori.archivetune.di.archiveTuneCoreModule
import moe.rukamori.archivetune.di.archiveTuneInjectedModule
import org.koin.mp.KoinPlatform
import org.koin.core.context.startKoin

/**
 * Avvio dell'app, uguale per Android e iPhone: Koin (al posto di Hilt), database, lettore
 * e la "MainActivity" comune di ArchiveTune con tutta l'interfaccia.
 */
object ArchiveTuneXRuntime {
    private var host: MainActivity? = null

    fun start(): MainActivity {
        host?.let { return it }
        if (KoinPlatform.getKoinOrNull() == null) {
            startKoin {
                modules(archiveTuneCoreModule, archiveTuneInjectedModule)
            }
        }
        val koin = KoinPlatform.getKoin()
        val created =
            MainActivity(
                database = koin.get(),
                downloadUtil = koin.get(),
                syncUtils = koin.get(),
                observeUpdateSettings = koin.get(),
                musicService = koin.get(),
            )
        host = created
        created.onCreate()
        return created
    }
}
