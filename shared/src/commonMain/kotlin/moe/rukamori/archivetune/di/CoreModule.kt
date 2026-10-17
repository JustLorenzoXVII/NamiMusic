/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 *
 * ArchiveTuneX: quello che nell'originale forniva AppModule di Hilt (database, cache, contesto).
 */

package moe.rukamori.archivetune.di

import moe.rukamori.archivetune.compat.AppContext
import moe.rukamori.archivetune.compat.Context
import moe.rukamori.archivetune.compat.media3.Cache
import moe.rukamori.archivetune.compat.media3.EmptyCache
import moe.rukamori.archivetune.db.MusicDatabase
import moe.rukamori.archivetune.playback.DownloadsCache
import moe.rukamori.archivetune.playback.MusicService
import moe.rukamori.archivetune.playback.MusicServiceHolder
import moe.rukamori.archivetune.utils.NetworkConnectivityObserver
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module

val archiveTuneCoreModule: Module =
    module {
        single<Context> { AppContext }
        single { MusicDatabase.newInstance() }
        single<Cache>(named("player")) { EmptyCache }
        single<Cache>(named("download")) { DownloadsCache(get()) }
        single { NetworkConnectivityObserver(get()) }
        single { MusicService(get(), get()).also { MusicServiceHolder.instance = it } }
    }
