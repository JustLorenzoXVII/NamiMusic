/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.rukamori.archivetune.onboarding

import archivetunex.shared.generated.resources.*

import kotlinx.coroutines.IO

import moe.rukamori.archivetune.compat.ImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import moe.rukamori.archivetune.BuildConfig
import org.jetbrains.compose.resources.StringResource

class ObserveOnboardingDataUseCase constructor(
        private val repository: OnboardingRepository,
    ) {
        operator fun invoke(refreshSignals: Flow<Int>): Flow<OnboardingData> =
            repository
                .observeShouldShowOnboarding()
                .combine(refreshSignals) { shouldShowOnboarding, _ ->
                    OnboardingData(
                        shouldShowOnboarding = shouldShowOnboarding,
                        permissions = repository.currentPermissions(),
                    )
                }.flowOn(Dispatchers.IO)
    }

class BuildOnboardingUiStateUseCase constructor() {
        operator fun invoke(
            data: OnboardingData,
            currentPage: Int,
        ): OnboardingUiState =
            OnboardingUiState(
                shouldShowOnboarding = data.shouldShowOnboarding,
                currentPage = currentPage.coerceIn(0, pages.lastIndex),
                variantLabelResId = variantLabelResId(),
                versionName = BuildConfig.VERSION_NAME,
                pages = pages,
                permissions = ImmutableList.copyOf(data.permissions.map { it.toUiModel() }),
                loginBenefits = loginBenefits,
                communityActions = communityActions,
            )

        private fun variantLabelResId(): StringResource =
            if (BuildConfig.DISTRIBUTION == DISTRIBUTION_GMS) {
                Res.string.onboarding_gms_variant
            } else {
                Res.string.onboarding_foss_variant
            }

        private fun OnboardingPermissionData.toUiModel(): OnboardingPermissionUiModel =
            OnboardingPermissionUiModel(
                id = id,
                titleResId = id.titleResId(),
                descriptionResId = id.descriptionResId(),
                iconResId = id.iconResId(),
                status = status,
                action = action,
            )

        private fun OnboardingPermissionId.titleResId(): StringResource =
            when (this) {
                OnboardingPermissionId.NOTIFICATIONS -> {
                    Res.string.onboarding_permission_notifications_title
                }

                OnboardingPermissionId.LOCAL_AUDIO -> {
                    Res.string.permission_storage_title
                }

                OnboardingPermissionId.MICROPHONE -> {
                    Res.string.music_recognition_permission_title
                }

                OnboardingPermissionId.DEVICE_AUDIO_CAPTURE -> {
                    Res.string.onboarding_permission_device_audio_capture_title
                }

                OnboardingPermissionId.BLUETOOTH_CONNECT -> {
                    Res.string.onboarding_permission_bluetooth_connect_title
                }

                OnboardingPermissionId.NETWORK -> {
                    Res.string.onboarding_permission_network_title
                }

                OnboardingPermissionId.PLAYBACK_SERVICE -> {
                    Res.string.onboarding_permission_playback_service_title
                }

                OnboardingPermissionId.AUDIO_SETTINGS -> {
                    Res.string.onboarding_permission_audio_settings_title
                }

                OnboardingPermissionId.APP_INSTALLATION -> {
                    Res.string.onboarding_permission_app_installation_title
                }

                OnboardingPermissionId.BLUETOOTH_SCAN -> {
                    Res.string.onboarding_permission_bluetooth_scan_title
                }
            }

        private fun OnboardingPermissionId.descriptionResId(): Int =
            when (this) {
                OnboardingPermissionId.NOTIFICATIONS -> {
                    Res.string.onboarding_permission_notifications_desc
                }

                OnboardingPermissionId.LOCAL_AUDIO -> {
                    Res.string.permission_storage_desc
                }

                OnboardingPermissionId.MICROPHONE -> {
                    Res.string.music_recognition_permission_desc
                }

                OnboardingPermissionId.DEVICE_AUDIO_CAPTURE -> {
                    Res.string.onboarding_permission_device_audio_capture_desc
                }

                OnboardingPermissionId.BLUETOOTH_CONNECT -> {
                    Res.string.onboarding_permission_bluetooth_connect_desc
                }

                OnboardingPermissionId.NETWORK -> {
                    Res.string.onboarding_permission_network_desc
                }

                OnboardingPermissionId.PLAYBACK_SERVICE -> {
                    Res.string.onboarding_permission_playback_service_desc
                }

                OnboardingPermissionId.AUDIO_SETTINGS -> {
                    Res.string.onboarding_permission_audio_settings_desc
                }

                OnboardingPermissionId.APP_INSTALLATION -> {
                    Res.string.onboarding_permission_app_installation_desc
                }

                OnboardingPermissionId.BLUETOOTH_SCAN -> {
                    Res.string.onboarding_permission_bluetooth_scan_desc
                }
            }

        private fun OnboardingPermissionId.iconResId(): Int =
            when (this) {
                OnboardingPermissionId.NOTIFICATIONS -> Res.drawable.music_note
                OnboardingPermissionId.LOCAL_AUDIO -> Res.drawable.storage
                OnboardingPermissionId.MICROPHONE -> Res.drawable.mic
                OnboardingPermissionId.DEVICE_AUDIO_CAPTURE -> Res.drawable.screenshot
                OnboardingPermissionId.BLUETOOTH_CONNECT -> Res.drawable.bluetooth
                OnboardingPermissionId.NETWORK -> Res.drawable.wifi_proxy
                OnboardingPermissionId.PLAYBACK_SERVICE -> Res.drawable.library_music
                OnboardingPermissionId.AUDIO_SETTINGS -> Res.drawable.settings
                OnboardingPermissionId.APP_INSTALLATION -> Res.drawable.download
                OnboardingPermissionId.BLUETOOTH_SCAN -> Res.drawable.bluetooth
            }

        private companion object {
            const val DISTRIBUTION_GMS = "gms"

            val pages =
                ImmutableList.of(
                    OnboardingPageUiModel(
                        id = OnboardingPageId.WELCOME,
                        titleResId = Res.string.onboarding_welcome_title,
                        subtitleResId = Res.string.onboarding_welcome_subtitle,
                        iconResId = Res.drawable.app_icon_small,
                    ),
                    OnboardingPageUiModel(
                        id = OnboardingPageId.PERMISSIONS,
                        titleResId = Res.string.onboarding_permissions_title,
                        subtitleResId = Res.string.onboarding_permissions_subtitle,
                        iconResId = Res.drawable.security,
                    ),
                    OnboardingPageUiModel(
                        id = OnboardingPageId.LOGIN,
                        titleResId = Res.string.onboarding_login_title,
                        subtitleResId = Res.string.onboarding_login_subtitle,
                        iconResId = Res.drawable.login,
                    ),
                    OnboardingPageUiModel(
                        id = OnboardingPageId.COMMUNITY,
                        titleResId = Res.string.onboarding_community_title,
                        subtitleResId = Res.string.onboarding_community_subtitle,
                        iconResId = Res.drawable.star,
                    ),
                )

            val loginBenefits =
                ImmutableList.of(
                    OnboardingLoginBenefitUiModel(
                        id = "library",
                        titleResId = Res.string.onboarding_login_library_title,
                        descriptionResId = Res.string.onboarding_login_library_desc,
                        iconResId = Res.drawable.library_music,
                    ),
                    OnboardingLoginBenefitUiModel(
                        id = "history",
                        titleResId = Res.string.onboarding_login_history_title,
                        descriptionResId = Res.string.onboarding_login_history_desc,
                        iconResId = Res.drawable.history,
                    ),
                    OnboardingLoginBenefitUiModel(
                        id = "playback",
                        titleResId = Res.string.onboarding_login_playback_title,
                        descriptionResId = Res.string.onboarding_login_playback_desc,
                        iconResId = Res.drawable.bolt,
                    ),
                )

            val communityActions =
                ImmutableList.of(
                    OnboardingCommunityActionUiModel(
                        id = "github",
                        titleResId = Res.string.support_development_star,
                        descriptionResId = Res.string.onboarding_community_github_desc,
                        iconResId = Res.drawable.github,
                        url = "https://github.com/rukamori/ArchiveTune",
                    ),
                    OnboardingCommunityActionUiModel(
                        id = "discord",
                        titleResId = Res.string.onboarding_community_discord_title,
                        descriptionResId = Res.string.onboarding_community_telegram_desc,
                        iconResId = Res.drawable.discord,
                        url = "https://discord.gg/XF2fpb9rTq",
                    ),
                    OnboardingCommunityActionUiModel(
                        id = "telegram",
                        titleResId = Res.string.onboarding_community_telegram_title,
                        descriptionResId = Res.string.onboarding_community_telegram_desc,
                        iconResId = Res.drawable.telegram,
                        url = "https://t.me/ArchiveTuneGC",
                    ),
                    OnboardingCommunityActionUiModel(
                        id = "donate",
                        titleResId = Res.string.about_content_desc_donate,
                        descriptionResId = Res.string.onboarding_community_donate_desc,
                        iconResId = Res.drawable.coffee,
                        url = "https://koiiverse.cloud/donate",
                    ),
                )
        }
    }

class CompleteOnboardingUseCase constructor(
        private val repository: OnboardingRepository,
    ) {
        suspend operator fun invoke() {
            repository.markCompleted()
        }
    }
