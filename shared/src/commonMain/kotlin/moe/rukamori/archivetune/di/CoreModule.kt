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
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import moe.rukamori.archivetune.androidauto.AndroidAutoSettingsUseCases
import moe.rukamori.archivetune.logcat.ClearLogcatUseCase
import moe.rukamori.archivetune.logcat.ExportLogcatUseCase
import moe.rukamori.archivetune.logcat.FilterLogcatUseCase
import moe.rukamori.archivetune.logcat.FormatLogcatUseCase
import moe.rukamori.archivetune.logcat.ObserveLogcatUseCase
import moe.rukamori.archivetune.musicrecognition.FilterRecognitionHistoryUseCase
import moe.rukamori.archivetune.musicrecognition.ObserveBackgroundRecognitionSettingUseCase
import moe.rukamori.archivetune.musicrecognition.ObserveRecognitionHistoryUseCase
import moe.rukamori.archivetune.musicrecognition.RecognizeMusicUseCase
import moe.rukamori.archivetune.musicrecognition.SetBackgroundRecognitionEnabledUseCase
import moe.rukamori.archivetune.playback.EqualizerPlaybackController
import moe.rukamori.archivetune.playback.preload.ObservePlaybackPerformanceSettingsUseCase
import moe.rukamori.archivetune.playback.preload.SetLowDataModeUseCase
import moe.rukamori.archivetune.playback.preload.SetPreloadNextSongUseCase

val archiveTuneCoreModule: Module =
    module {
        single<Context> { AppContext }
        single { MusicDatabase.newInstance() }
        single<Cache>(named("player")) { EmptyCache }
        single<Cache>(named("download")) { DownloadsCache(get()) }
        single { NetworkConnectivityObserver(get()) }
        single { MusicService(get(), get()).also { MusicServiceHolder.instance = it } }
        single { moe.rukamori.archivetune.localmedia.createLocalSongScanner(get()) }
        singleOf(::EqualizerPlaybackController)
        singleOf(::moe.rukamori.archivetune.androidauto.AndroidAutoSettingsRepository)
        singleOf(::moe.rukamori.archivetune.logcat.LogcatRepository)
        singleOf(::moe.rukamori.archivetune.musicrecognition.MusicRecognitionRepository)
        singleOf(::moe.rukamori.archivetune.playback.preload.PlaybackPerformanceSettingsRepository)
        factoryOf(::AndroidAutoSettingsUseCases)
        factoryOf(::ClearLogcatUseCase)
        factoryOf(::ExportLogcatUseCase)
        factoryOf(::FilterLogcatUseCase)
        factoryOf(::FormatLogcatUseCase)
        factoryOf(::ObserveLogcatUseCase)
        factoryOf(::FilterRecognitionHistoryUseCase)
        factoryOf(::ObserveBackgroundRecognitionSettingUseCase)
        factoryOf(::ObserveRecognitionHistoryUseCase)
        factoryOf(::RecognizeMusicUseCase)
        factoryOf(::SetBackgroundRecognitionEnabledUseCase)
        factoryOf(::ObservePlaybackPerformanceSettingsUseCase)
        factoryOf(::SetLowDataModeUseCase)
        factoryOf(::SetPreloadNextSongUseCase)
    }
