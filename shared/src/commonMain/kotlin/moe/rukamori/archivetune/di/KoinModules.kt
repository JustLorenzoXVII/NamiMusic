/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 *
 * Generato da koingen.py: le classi che nell'originale venivano create da Hilt
 * (@Inject constructor, @HiltViewModel) qui sono registrate in Koin.
 */

package moe.rukamori.archivetune.di

import moe.rukamori.archivetune.about.AboutAttributionRepository
import moe.rukamori.archivetune.about.AboutContributorsRepository
import moe.rukamori.archivetune.about.FetchAboutContributorsUseCase
import moe.rukamori.archivetune.about.FetchAboutDependencyLicensesUseCase
import moe.rukamori.archivetune.about.FetchAboutTranslationContributorsUseCase
import moe.rukamori.archivetune.ai.AiIntegrationSettingsRepository
import moe.rukamori.archivetune.ai.FetchAiModelsUseCase
import moe.rukamori.archivetune.ai.ObserveAiIntegrationSettingsUseCase
import moe.rukamori.archivetune.ai.TestAiIntegrationUseCase
import moe.rukamori.archivetune.ai.UpdateAiIntegrationSettingsUseCase
import moe.rukamori.archivetune.aicontentfilter.AiContentFilterRepository
import moe.rukamori.archivetune.aicontentfilter.FilterAiContentUseCase
import moe.rukamori.archivetune.aicontentfilter.LoadAiContentFilterPolicyUseCase
import moe.rukamori.archivetune.aicontentfilter.ObserveAiContentFilterUseCase
import moe.rukamori.archivetune.aicontentfilter.RefreshAiContentFilterUseCase
import moe.rukamori.archivetune.aicontentfilter.UpdateAiContentFilterSettingsUseCase
import moe.rukamori.archivetune.appicon.AppIconRepository
import moe.rukamori.archivetune.appicon.LoadAppIconsUseCase
import moe.rukamori.archivetune.appicon.RestoreLauncherEntryUseCase
import moe.rukamori.archivetune.appicon.SelectAppIconUseCase
import moe.rukamori.archivetune.artist.ArtistBlockRepository
import moe.rukamori.archivetune.artist.ObserveArtistBlockedUseCase
import moe.rukamori.archivetune.artist.SetArtistBlockedUseCase
import moe.rukamori.archivetune.auth.CompleteYouTubeLoginUseCase
import moe.rukamori.archivetune.auth.SwitchSavedYouTubeAccountUseCase
import moe.rukamori.archivetune.auth.UpdateYouTubeLoginContextUseCase
import moe.rukamori.archivetune.auth.YouTubeLoginRepository
import moe.rukamori.archivetune.backup.BackupArchiveRepository
import moe.rukamori.archivetune.backup.BackupOperationCoordinator
import moe.rukamori.archivetune.backup.CreateBackupUseCase
import moe.rukamori.archivetune.backup.ObserveScheduledBackupSettingsUseCase
import moe.rukamori.archivetune.backup.ScheduledBackupRepository
import moe.rukamori.archivetune.backup.ScheduledBackupScheduler
import moe.rukamori.archivetune.backup.UpdateScheduledBackupUseCase
import moe.rukamori.archivetune.browse.BrowseRepository
import moe.rukamori.archivetune.browse.LoadBrowseContinuationUseCase
import moe.rukamori.archivetune.browse.LoadBrowseUseCase
import moe.rukamori.archivetune.canvas.CanvasArtworkRepository
import moe.rukamori.archivetune.canvas.CanvasPlaybackUseCase
import moe.rukamori.archivetune.canvas.CanvasSettingsRepository
import moe.rukamori.archivetune.canvas.CanvasSettingsUseCases
import moe.rukamori.archivetune.canvas.SpotifyCanvasRepository
import moe.rukamori.archivetune.canvas.SpotifyCanvasSessionRepository
import moe.rukamori.archivetune.canvas.StartCanvasPolicyUseCase
import moe.rukamori.archivetune.domain.player.ObserveImmersivePlayerUseCase
import moe.rukamori.archivetune.downloads.DownloadedArtworkRepository
import moe.rukamori.archivetune.downloads.ManageDownloadsUseCase
import moe.rukamori.archivetune.downloads.Media3DownloadRepository
import moe.rukamori.archivetune.equalizer.ApplyEqualizerPresetUseCase
import moe.rukamori.archivetune.equalizer.EqualizerRepository
import moe.rukamori.archivetune.equalizer.ManageEqualizerProfilesUseCase
import moe.rukamori.archivetune.equalizer.ObserveEqualizerUseCase
import moe.rukamori.archivetune.equalizer.UpdateEqualizerUseCase
import moe.rukamori.archivetune.gatekeeper.GatekeeperRepository
import moe.rukamori.archivetune.gatekeeper.RunGatekeeperCheckUseCase
import moe.rukamori.archivetune.home.HomeRepository
import moe.rukamori.archivetune.home.LoadCommunityPlaylistPreviewsUseCase
import moe.rukamori.archivetune.home.LoadPersonalizedQuickPicksUseCase
import moe.rukamori.archivetune.home.ObserveHomePresentationPreferencesUseCase
import moe.rukamori.archivetune.home.PrepareCommunityHomePageUseCase
import moe.rukamori.archivetune.library.ObserveLibraryTopMixesUseCase
import moe.rukamori.archivetune.library.RefreshLibraryTopMixesUseCase
import moe.rukamori.archivetune.library.RefreshLibraryUseCase
import moe.rukamori.archivetune.localmedia.LocalMetadataRepository
import moe.rukamori.archivetune.localmedia.ScanLocalMusicUseCase
import moe.rukamori.archivetune.lyrics.LyricsHelper
import moe.rukamori.archivetune.lyrics.LyricsRenderingRepository
import moe.rukamori.archivetune.lyrics.PrepareLyricsUseCase
import moe.rukamori.archivetune.lyrics.translation.StandardLyricsTranslationRepository
import moe.rukamori.archivetune.lyrics.translation.TranslateLyricsUseCase
import moe.rukamori.archivetune.lyrics.translation.TranslatedLyricsRepository
import moe.rukamori.archivetune.mediainfo.MediaInfoRepository
import moe.rukamori.archivetune.mediainfo.ObserveMediaInfoUseCase
import moe.rukamori.archivetune.network.NetworkMonitor
import moe.rukamori.archivetune.network.ObserveNetworkBannerStateUseCase
import moe.rukamori.archivetune.onboarding.BuildOnboardingUiStateUseCase
import moe.rukamori.archivetune.onboarding.CompleteOnboardingUseCase
import moe.rukamori.archivetune.onboarding.ObserveOnboardingDataUseCase
import moe.rukamori.archivetune.onboarding.OnboardingRepository
import moe.rukamori.archivetune.onboarding.OnboardingViewModel
import moe.rukamori.archivetune.playback.DownloadUtil
import moe.rukamori.archivetune.playback.ImmersivePlayerRepository
import moe.rukamori.archivetune.playlist.CreatePlaylistUseCase
import moe.rukamori.archivetune.playlist.GetCreatePlaylistOptionsUseCase
import moe.rukamori.archivetune.playlist.RemovePlaylistCoverUseCase
import moe.rukamori.archivetune.playlist.UpdatePlaylistCoverUseCase
import moe.rukamori.archivetune.playlistexport.ExportPlaylistAsCsvUseCase
import moe.rukamori.archivetune.playlistexport.ObserveExportablePlaylistsUseCase
import moe.rukamori.archivetune.playlisttags.AddTagsToPlaylistsUseCase
import moe.rukamori.archivetune.playlisttags.CreatePlaylistTagUseCase
import moe.rukamori.archivetune.playlisttags.DeletePlaylistTagUseCase
import moe.rukamori.archivetune.playlisttags.ObservePlaylistTagsUseCase
import moe.rukamori.archivetune.playlisttags.SavePlaylistTagsUseCase
import moe.rukamori.archivetune.playlisttags.UpdatePlaylistTagColorUseCase
import moe.rukamori.archivetune.playlisttags.UpdatePlaylistTagUseCase
import moe.rukamori.archivetune.podcast.LoadPodcastContinuationUseCase
import moe.rukamori.archivetune.podcast.LoadPodcastUseCase
import moe.rukamori.archivetune.podcast.ObservePodcastLibraryMembershipUseCase
import moe.rukamori.archivetune.podcast.ObserveSavedPodcastsUseCase
import moe.rukamori.archivetune.podcast.PodcastRepository
import moe.rukamori.archivetune.podcast.SearchPodcastEpisodesUseCase
import moe.rukamori.archivetune.podcast.ToggleEpisodeLibraryUseCase
import moe.rukamori.archivetune.podcast.TogglePodcastSaveUseCase
import moe.rukamori.archivetune.privacy.ListeningStatsRepository
import moe.rukamori.archivetune.privacy.ResetListeningStatsUseCase
import moe.rukamori.archivetune.repository.LibraryTopMixRepository
import moe.rukamori.archivetune.repository.NewsRepository
import moe.rukamori.archivetune.repository.PlaylistCoverRepository
import moe.rukamori.archivetune.repository.PlaylistCreationRepository
import moe.rukamori.archivetune.repository.PlaylistExportRepository
import moe.rukamori.archivetune.repository.PlaylistTagsRepository
import moe.rukamori.archivetune.repository.SearchDiscoveryRepository
import moe.rukamori.archivetune.scrobbling.LastFmSettingsRepository
import moe.rukamori.archivetune.scrobbling.LoginLastFmUseCase
import moe.rukamori.archivetune.scrobbling.LogoutLastFmUseCase
import moe.rukamori.archivetune.scrobbling.ObserveLastFmSettingsUseCase
import moe.rukamori.archivetune.scrobbling.SaveLastFmServiceConfigUseCase
import moe.rukamori.archivetune.scrobbling.UpdateLastFmScrobblingOptionsUseCase
import moe.rukamori.archivetune.search.LoadSearchDiscoveryUseCase
import moe.rukamori.archivetune.sponsorblock.GetSponsorBlockSegmentsUseCase
import moe.rukamori.archivetune.sponsorblock.ObserveSponsorBlockSettingsUseCase
import moe.rukamori.archivetune.sponsorblock.SetSponsorBlockApiUrlUseCase
import moe.rukamori.archivetune.sponsorblock.SetSponsorBlockCategoriesUseCase
import moe.rukamori.archivetune.sponsorblock.SetSponsorBlockEnabledUseCase
import moe.rukamori.archivetune.sponsorblock.SponsorBlockPlaybackController
import moe.rukamori.archivetune.sponsorblock.SponsorBlockRepository
import moe.rukamori.archivetune.sponsorblock.ValidateSponsorBlockApiUrlUseCase
import moe.rukamori.archivetune.storage.ClearStorageCacheUseCase
import moe.rukamori.archivetune.storage.ObserveStorageFoldersUseCase
import moe.rukamori.archivetune.storage.SetStorageFolderUseCase
import moe.rukamori.archivetune.storage.StorageLocationRepository
import moe.rukamori.archivetune.together.AttachMusicTogetherServiceUseCase
import moe.rukamori.archivetune.together.MusicTogetherRepository
import moe.rukamori.archivetune.together.MusicTogetherSessionActionsUseCase
import moe.rukamori.archivetune.together.ObserveMusicTogetherStateUseCase
import moe.rukamori.archivetune.together.UpdateMusicTogetherPreferencesUseCase
import moe.rukamori.archivetune.updates.ObserveUpdateSettingsUseCase
import moe.rukamori.archivetune.updates.UpdateCheckScheduler
import moe.rukamori.archivetune.updates.UpdateSettingsRepository
import moe.rukamori.archivetune.updates.UpdateUpdateSettingsUseCase
import moe.rukamori.archivetune.utils.SyncUtils
import moe.rukamori.archivetune.viewmodels.AboutViewModel
import moe.rukamori.archivetune.viewmodels.AccountViewModel
import moe.rukamori.archivetune.viewmodels.AiIntegrationSettingsViewModel
import moe.rukamori.archivetune.viewmodels.AlbumViewModel
import moe.rukamori.archivetune.viewmodels.AndroidAutoSettingsViewModel
import moe.rukamori.archivetune.viewmodels.ArtistAlbumsViewModel
import moe.rukamori.archivetune.viewmodels.ArtistItemsViewModel
import moe.rukamori.archivetune.viewmodels.ArtistSongsViewModel
import moe.rukamori.archivetune.viewmodels.ArtistViewModel
import moe.rukamori.archivetune.viewmodels.AutoPlaylistViewModel
import moe.rukamori.archivetune.viewmodels.BackupRestoreViewModel
import moe.rukamori.archivetune.viewmodels.BrowseViewModel
import moe.rukamori.archivetune.viewmodels.CachePlaylistViewModel
import moe.rukamori.archivetune.viewmodels.CanvasPlaybackViewModel
import moe.rukamori.archivetune.viewmodels.CanvasSettingsViewModel
import moe.rukamori.archivetune.viewmodels.ChartsViewModel
import moe.rukamori.archivetune.viewmodels.ContentSettingsViewModel
import moe.rukamori.archivetune.viewmodels.CreatePlaylistViewModel
import moe.rukamori.archivetune.viewmodels.DownloadLibraryViewModel
import moe.rukamori.archivetune.viewmodels.EqualizerViewModel
import moe.rukamori.archivetune.viewmodels.ExploreViewModel
import moe.rukamori.archivetune.viewmodels.GatekeeperViewModel
import moe.rukamori.archivetune.viewmodels.HistoryViewModel
import moe.rukamori.archivetune.viewmodels.HomeViewModel
import moe.rukamori.archivetune.viewmodels.IconViewModel
import moe.rukamori.archivetune.viewmodels.ImmersivePlayerViewModel
import moe.rukamori.archivetune.viewmodels.LastFmSettingsViewModel
import moe.rukamori.archivetune.viewmodels.LibraryAlbumsViewModel
import moe.rukamori.archivetune.viewmodels.LibraryArtistsViewModel
import moe.rukamori.archivetune.viewmodels.LibraryMixViewModel
import moe.rukamori.archivetune.viewmodels.LibraryPlaylistsViewModel
import moe.rukamori.archivetune.viewmodels.LibrarySongsViewModel
import moe.rukamori.archivetune.viewmodels.LibraryViewModel
import moe.rukamori.archivetune.viewmodels.LocalPlaylistViewModel
import moe.rukamori.archivetune.viewmodels.LocalSearchViewModel
import moe.rukamori.archivetune.viewmodels.LocalSongsViewModel
import moe.rukamori.archivetune.viewmodels.LogcatViewModel
import moe.rukamori.archivetune.viewmodels.LoginViewModel
import moe.rukamori.archivetune.viewmodels.LyricsMenuViewModel
import moe.rukamori.archivetune.viewmodels.LyricsRenderViewModel
import moe.rukamori.archivetune.viewmodels.MediaInfoViewModel
import moe.rukamori.archivetune.viewmodels.MoodAndGenresViewModel
import moe.rukamori.archivetune.viewmodels.MusicRecognitionViewModel
import moe.rukamori.archivetune.viewmodels.MusicTogetherViewModel
import moe.rukamori.archivetune.viewmodels.NetworkBannerViewModel
import moe.rukamori.archivetune.viewmodels.NewReleaseViewModel
import moe.rukamori.archivetune.viewmodels.NewsViewModel
import moe.rukamori.archivetune.viewmodels.OnlinePlaylistViewModel
import moe.rukamori.archivetune.viewmodels.OnlineSearchSuggestionViewModel
import moe.rukamori.archivetune.viewmodels.OnlineSearchViewModel
import moe.rukamori.archivetune.viewmodels.PlaybackPerformanceSettingsViewModel
import moe.rukamori.archivetune.viewmodels.PlaylistTagsViewModel
import moe.rukamori.archivetune.viewmodels.PodcastLibraryViewModel
import moe.rukamori.archivetune.viewmodels.PodcastViewModel
import moe.rukamori.archivetune.viewmodels.ResetListeningStatsViewModel
import moe.rukamori.archivetune.viewmodels.SearchDiscoveryViewModel
import moe.rukamori.archivetune.viewmodels.SponsorBlockSettingsViewModel
import moe.rukamori.archivetune.viewmodels.StatsViewModel
import moe.rukamori.archivetune.viewmodels.StorageSettingsViewModel
import moe.rukamori.archivetune.viewmodels.TopPlaylistViewModel
import moe.rukamori.archivetune.viewmodels.UpdateSettingsViewModel
import moe.rukamori.archivetune.viewmodels.ViewNewsViewModel
import moe.rukamori.archivetune.viewmodels.YearInMusicViewModel
import moe.rukamori.archivetune.viewmodels.YouTubeBrowseViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.core.qualifier.named
import org.koin.dsl.module

val archiveTuneInjectedModule: Module =
    module {
        factoryOf(::FetchAboutContributorsUseCase)
        factoryOf(::FetchAboutDependencyLicensesUseCase)
        factoryOf(::FetchAboutTranslationContributorsUseCase)
        factoryOf(::FetchAiModelsUseCase)
        factoryOf(::ObserveAiIntegrationSettingsUseCase)
        factoryOf(::TestAiIntegrationUseCase)
        factoryOf(::UpdateAiIntegrationSettingsUseCase)
        factoryOf(::FilterAiContentUseCase)
        factoryOf(::LoadAiContentFilterPolicyUseCase)
        factoryOf(::ObserveAiContentFilterUseCase)
        factoryOf(::RefreshAiContentFilterUseCase)
        factoryOf(::UpdateAiContentFilterSettingsUseCase)
        factoryOf(::LoadAppIconsUseCase)
        factoryOf(::RestoreLauncherEntryUseCase)
        factoryOf(::SelectAppIconUseCase)
        factoryOf(::ArtistBlockRepository)
        factoryOf(::ObserveArtistBlockedUseCase)
        factoryOf(::SetArtistBlockedUseCase)
        factoryOf(::CompleteYouTubeLoginUseCase)
        factoryOf(::SwitchSavedYouTubeAccountUseCase)
        factoryOf(::UpdateYouTubeLoginContextUseCase)
        factoryOf(::CreateBackupUseCase)
        factoryOf(::ObserveScheduledBackupSettingsUseCase)
        factoryOf(::UpdateScheduledBackupUseCase)
        factoryOf(::LoadBrowseContinuationUseCase)
        factoryOf(::LoadBrowseUseCase)
        factoryOf(::CanvasPlaybackUseCase)
        factoryOf(::CanvasSettingsUseCases)
        factoryOf(::ObserveImmersivePlayerUseCase)
        factoryOf(::ManageDownloadsUseCase)
        factoryOf(::ApplyEqualizerPresetUseCase)
        factoryOf(::ManageEqualizerProfilesUseCase)
        factoryOf(::ObserveEqualizerUseCase)
        factoryOf(::UpdateEqualizerUseCase)
        factoryOf(::RunGatekeeperCheckUseCase)
        factoryOf(::HomeRepository)
        factoryOf(::LoadCommunityPlaylistPreviewsUseCase)
        factoryOf(::LoadPersonalizedQuickPicksUseCase)
        factoryOf(::ObserveHomePresentationPreferencesUseCase)
        factoryOf(::PrepareCommunityHomePageUseCase)
        factoryOf(::ObserveLibraryTopMixesUseCase)
        factoryOf(::RefreshLibraryTopMixesUseCase)
        factoryOf(::RefreshLibraryUseCase)
        factoryOf(::LocalMetadataRepository)
        factoryOf(::ScanLocalMusicUseCase)
        factoryOf(::PrepareLyricsUseCase)
        factoryOf(::TranslateLyricsUseCase)
        factoryOf(::MediaInfoRepository)
        factoryOf(::ObserveMediaInfoUseCase)
        factoryOf(::ObserveNetworkBannerStateUseCase)
        factoryOf(::BuildOnboardingUiStateUseCase)
        factoryOf(::CompleteOnboardingUseCase)
        factoryOf(::ObserveOnboardingDataUseCase)
        factoryOf(::OnboardingRepository)
        factoryOf(::ImmersivePlayerRepository)
        factoryOf(::CreatePlaylistUseCase)
        factoryOf(::GetCreatePlaylistOptionsUseCase)
        factoryOf(::RemovePlaylistCoverUseCase)
        factoryOf(::UpdatePlaylistCoverUseCase)
        factoryOf(::ExportPlaylistAsCsvUseCase)
        factoryOf(::ObserveExportablePlaylistsUseCase)
        factoryOf(::AddTagsToPlaylistsUseCase)
        factoryOf(::CreatePlaylistTagUseCase)
        factoryOf(::DeletePlaylistTagUseCase)
        factoryOf(::ObservePlaylistTagsUseCase)
        factoryOf(::SavePlaylistTagsUseCase)
        factoryOf(::UpdatePlaylistTagColorUseCase)
        factoryOf(::UpdatePlaylistTagUseCase)
        factoryOf(::LoadPodcastContinuationUseCase)
        factoryOf(::LoadPodcastUseCase)
        factoryOf(::ObservePodcastLibraryMembershipUseCase)
        factoryOf(::ObserveSavedPodcastsUseCase)
        factoryOf(::SearchPodcastEpisodesUseCase)
        factoryOf(::ToggleEpisodeLibraryUseCase)
        factoryOf(::TogglePodcastSaveUseCase)
        factoryOf(::ListeningStatsRepository)
        factoryOf(::ResetListeningStatsUseCase)
        factoryOf(::LibraryTopMixRepository)
        factoryOf(::LoginLastFmUseCase)
        factoryOf(::LogoutLastFmUseCase)
        factoryOf(::ObserveLastFmSettingsUseCase)
        factoryOf(::SaveLastFmServiceConfigUseCase)
        factoryOf(::UpdateLastFmScrobblingOptionsUseCase)
        factoryOf(::LoadSearchDiscoveryUseCase)
        factoryOf(::GetSponsorBlockSegmentsUseCase)
        factoryOf(::ObserveSponsorBlockSettingsUseCase)
        factoryOf(::SetSponsorBlockApiUrlUseCase)
        factoryOf(::SetSponsorBlockCategoriesUseCase)
        factoryOf(::SetSponsorBlockEnabledUseCase)
        factoryOf(::ValidateSponsorBlockApiUrlUseCase)
        factoryOf(::ClearStorageCacheUseCase)
        factoryOf(::ObserveStorageFoldersUseCase)
        factoryOf(::SetStorageFolderUseCase)
        factory { StorageLocationRepository(get(), get(named("player")), get(named("download")), get(), get()) }
        factoryOf(::AttachMusicTogetherServiceUseCase)
        factoryOf(::MusicTogetherSessionActionsUseCase)
        factoryOf(::ObserveMusicTogetherStateUseCase)
        factoryOf(::UpdateMusicTogetherPreferencesUseCase)
        factoryOf(::ObserveUpdateSettingsUseCase)
        factoryOf(::UpdateUpdateSettingsUseCase)
        factoryOf(::ArtistViewModel)
        factoryOf(::AutoPlaylistViewModel)
        factoryOf(::CanvasSettingsViewModel)
        factoryOf(::LocalSearchViewModel)
        factoryOf(::OnlineSearchSuggestionViewModel)
        factoryOf(::PlaylistTagsViewModel)
        factoryOf(::StatsViewModel)
        factoryOf(::YearInMusicViewModel)
        singleOf(::AboutAttributionRepository)
        singleOf(::AboutContributorsRepository)
        singleOf(::AiIntegrationSettingsRepository)
        singleOf(::AiContentFilterRepository)
        singleOf(::AppIconRepository)
        singleOf(::YouTubeLoginRepository)
        singleOf(::BackupArchiveRepository)
        singleOf(::BackupOperationCoordinator)
        singleOf(::ScheduledBackupRepository)
        singleOf(::ScheduledBackupScheduler)
        singleOf(::BrowseRepository)
        singleOf(::CanvasArtworkRepository)
        singleOf(::CanvasSettingsRepository)
        singleOf(::SpotifyCanvasRepository)
        singleOf(::SpotifyCanvasSessionRepository)
        singleOf(::StartCanvasPolicyUseCase)
        singleOf(::DownloadedArtworkRepository)
        singleOf(::Media3DownloadRepository)
        singleOf(::EqualizerRepository)
        singleOf(::GatekeeperRepository)
        singleOf(::LyricsHelper)
        singleOf(::LyricsRenderingRepository)
        singleOf(::StandardLyricsTranslationRepository)
        singleOf(::TranslatedLyricsRepository)
        singleOf(::NetworkMonitor)
        single { DownloadUtil(get()) }
        singleOf(::PodcastRepository)
        singleOf(::NewsRepository)
        singleOf(::PlaylistCoverRepository)
        singleOf(::PlaylistCreationRepository)
        singleOf(::PlaylistExportRepository)
        singleOf(::PlaylistTagsRepository)
        singleOf(::SearchDiscoveryRepository)
        singleOf(::LastFmSettingsRepository)
        singleOf(::SponsorBlockPlaybackController)
        singleOf(::SponsorBlockRepository)
        singleOf(::MusicTogetherRepository)
        singleOf(::UpdateCheckScheduler)
        singleOf(::UpdateSettingsRepository)
        singleOf(::SyncUtils)
        viewModelOf(::OnboardingViewModel)
        viewModelOf(::AboutViewModel)
        viewModelOf(::AccountViewModel)
        viewModelOf(::AiIntegrationSettingsViewModel)
        viewModelOf(::AlbumViewModel)
        viewModelOf(::AndroidAutoSettingsViewModel)
        viewModelOf(::ArtistAlbumsViewModel)
        viewModelOf(::ArtistItemsViewModel)
        viewModelOf(::ArtistSongsViewModel)
        viewModelOf(::BackupRestoreViewModel)
        viewModelOf(::BrowseViewModel)
        viewModel { CachePlaylistViewModel(get(), get(), get(named("player")), get(named("download"))) }
        viewModelOf(::CanvasPlaybackViewModel)
        viewModelOf(::ChartsViewModel)
        viewModelOf(::ContentSettingsViewModel)
        viewModelOf(::CreatePlaylistViewModel)
        viewModelOf(::DownloadLibraryViewModel)
        viewModelOf(::EqualizerViewModel)
        viewModelOf(::ExploreViewModel)
        viewModelOf(::GatekeeperViewModel)
        viewModelOf(::HistoryViewModel)
        viewModelOf(::HomeViewModel)
        viewModelOf(::IconViewModel)
        viewModelOf(::ImmersivePlayerViewModel)
        viewModelOf(::LastFmSettingsViewModel)
        viewModelOf(::LibraryAlbumsViewModel)
        viewModelOf(::LibraryArtistsViewModel)
        viewModelOf(::LibraryMixViewModel)
        viewModelOf(::LibraryPlaylistsViewModel)
        viewModelOf(::LibrarySongsViewModel)
        viewModelOf(::LibraryViewModel)
        viewModelOf(::LocalPlaylistViewModel)
        viewModelOf(::LocalSongsViewModel)
        viewModelOf(::LogcatViewModel)
        viewModelOf(::LoginViewModel)
        viewModelOf(::LyricsMenuViewModel)
        viewModelOf(::LyricsRenderViewModel)
        viewModelOf(::MediaInfoViewModel)
        viewModelOf(::MoodAndGenresViewModel)
        viewModelOf(::MusicRecognitionViewModel)
        viewModelOf(::MusicTogetherViewModel)
        viewModelOf(::NetworkBannerViewModel)
        viewModelOf(::NewReleaseViewModel)
        viewModelOf(::NewsViewModel)
        viewModelOf(::OnlinePlaylistViewModel)
        viewModelOf(::OnlineSearchViewModel)
        viewModelOf(::PlaybackPerformanceSettingsViewModel)
        viewModelOf(::PodcastLibraryViewModel)
        viewModelOf(::PodcastViewModel)
        viewModelOf(::ResetListeningStatsViewModel)
        viewModelOf(::SearchDiscoveryViewModel)
        viewModelOf(::SponsorBlockSettingsViewModel)
        viewModelOf(::StorageSettingsViewModel)
        viewModelOf(::TopPlaylistViewModel)
        viewModelOf(::UpdateSettingsViewModel)
        viewModelOf(::ViewNewsViewModel)
        viewModelOf(::YouTubeBrowseViewModel)
    }
