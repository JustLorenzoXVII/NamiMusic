/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)

package moe.rukamori.archivetune.ui.screens.settings

import archivetunex.shared.generated.resources.*

import moe.rukamori.archivetune.compat.Intent
import moe.rukamori.archivetune.compat.startActivity
import moe.rukamori.archivetune.compat.Build
import moe.rukamori.archivetune.compat.Settings
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import moe.rukamori.archivetune.compat.LocalContext
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import moe.rukamori.archivetune.compat.toUri
import org.koin.compose.viewmodel.koinViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import moe.rukamori.archivetune.LocalPlayerAwareWindowInsets
import moe.rukamori.archivetune.constants.*
import moe.rukamori.archivetune.innertube.YouTube
import moe.rukamori.archivetune.ui.component.EditTextPreference
import moe.rukamori.archivetune.ui.component.IconButton
import moe.rukamori.archivetune.ui.component.ListPreference
import moe.rukamori.archivetune.ui.component.PreferenceEntry
import moe.rukamori.archivetune.ui.component.PreferenceGroup
import moe.rukamori.archivetune.ui.component.SwitchPreference
import moe.rukamori.archivetune.ui.utils.backToMain
import moe.rukamori.archivetune.utils.rememberEnumPreference
import moe.rukamori.archivetune.utils.rememberPreference
import moe.rukamori.archivetune.utils.setAppLocale
import moe.rukamori.archivetune.viewmodels.AiContentFilterSettingsEffect
import moe.rukamori.archivetune.viewmodels.AiContentFilterSettingsState
import moe.rukamori.archivetune.viewmodels.ContentSettingsViewModel
import moe.rukamori.archivetune.compat.Locale
import moe.rukamori.archivetune.compat.uppercase
import moe.rukamori.archivetune.compat.lowercase

@Composable
fun ContentSettings(
    navController: NavController,
    viewModel: ContentSettingsViewModel = koinViewModel(),
) {
    val context = LocalContext.current
    val aiContentFilterState by viewModel.aiContentFilterState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel, context) {
        viewModel.aiContentFilterEffects.collect { effect ->
            when (effect) {
                is AiContentFilterSettingsEffect.ShowMessage -> {
                    snackbarHostState.showSnackbar(context.getString(effect.messageResId))
                }

                is AiContentFilterSettingsEffect.OpenUrl -> {
                    context.startActivity(Intent(Intent.ACTION_VIEW, effect.url.toUri()))
                }
            }
        }
    }

    val (useSystemLanguage, onUseSystemLanguageChange) = rememberPreference(key = UseSystemLanguageKey, defaultValue = true)

    val (contentLanguage, onContentLanguageChange) = rememberPreference(key = ContentLanguageKey, defaultValue = "system")
    val (contentCountry, onContentCountryChange) = rememberPreference(key = ContentCountryKey, defaultValue = "system")
    val (playlistSuggestionSource, onPlaylistSuggestionSourceChange) =
        rememberEnumPreference(
            key = PlaylistSuggestionSourceKey,
            defaultValue = PlaylistSuggestionSource.BOTH,
        )
    val (hideExplicit, onHideExplicitChange) = rememberPreference(key = HideExplicitKey, defaultValue = false)
    val (hideVideo, onHideVideoChange) = rememberPreference(key = HideVideoKey, defaultValue = false)
    val (lengthTop, onLengthTopChange) = rememberPreference(key = TopSize, defaultValue = "50")
    val (quickPicks, onQuickPicksChange) = rememberEnumPreference(key = QuickPicksKey, defaultValue = QuickPicks.QUICK_PICKS)

    Column(
        Modifier
            .windowInsetsPadding(LocalPlayerAwareWindowInsets.current)
            .verticalScroll(rememberScrollState())
            .padding(bottom = SettingsDimensions.ScreenBottomPadding),
    ) {
        PreferenceGroup(title = stringResource(Res.string.general)) {
            item {
                ListPreference(
                    title = { Text(stringResource(Res.string.content_language)) },
                    icon = { Icon(painterResource(Res.drawable.language), null) },
                    selectedValue = contentLanguage,
                    values = listOf(SYSTEM_DEFAULT) + LanguageCodeToName.keys.toList(),
                    valueText = {
                        LanguageCodeToName.getOrElse(it) { stringResource(Res.string.system_default) }
                    },
                    onValueSelected = { newValue ->
                        val locale = Locale.getDefault()
                        val languageTag = locale.toLanguageTag().replace("-Hant", "")

                        YouTube.locale =
                            YouTube.locale.copy(
                                hl =
                                    newValue.takeIf { it != SYSTEM_DEFAULT }
                                        ?: locale.language.takeIf { it in LanguageCodeToName }
                                        ?: languageTag.takeIf { it in LanguageCodeToName }
                                        ?: "en",
                            )

                        onContentLanguageChange(newValue)
                    },
                )
            }

            item {
                ListPreference(
                    title = { Text(stringResource(Res.string.content_country)) },
                    icon = { Icon(painterResource(Res.drawable.location_on), null) },
                    selectedValue = contentCountry,
                    values = listOf(SYSTEM_DEFAULT) + CountryCodeToName.keys.toList(),
                    valueText = {
                        CountryCodeToName.getOrElse(it) { stringResource(Res.string.system_default) }
                    },
                    onValueSelected = { newValue ->
                        val locale = Locale.getDefault()

                        YouTube.locale =
                            YouTube.locale.copy(
                                gl =
                                    newValue.takeIf { it != SYSTEM_DEFAULT }
                                        ?: locale.country.takeIf { it in CountryCodeToName }
                                        ?: "US",
                            )

                        onContentCountryChange(newValue)
                    },
                )
            }

            item {
                ListPreference(
                    title = { Text(stringResource(Res.string.you_might_like_source)) },
                    icon = { Icon(painterResource(Res.drawable.playlist_play), null) },
                    selectedValue = playlistSuggestionSource,
                    values =
                        listOf(
                            PlaylistSuggestionSource.PLAYLIST_TITLE,
                            PlaylistSuggestionSource.PLAYLIST_CONTENT,
                            PlaylistSuggestionSource.BOTH,
                        ),
                    valueText = {
                        when (it) {
                            PlaylistSuggestionSource.PLAYLIST_TITLE -> stringResource(Res.string.playlist_suggestion_source_title)
                            PlaylistSuggestionSource.PLAYLIST_CONTENT -> stringResource(Res.string.playlist_suggestion_source_content)
                            PlaylistSuggestionSource.BOTH -> stringResource(Res.string.playlist_suggestion_source_both)
                        }
                    },
                    onValueSelected = onPlaylistSuggestionSourceChange,
                )
            }

            item {
                SwitchPreference(
                    title = { Text(stringResource(Res.string.hide_explicit)) },
                    icon = { Icon(painterResource(Res.drawable.explicit), null) },
                    checked = hideExplicit,
                    onCheckedChange = onHideExplicitChange,
                )
            }

            item {
                SwitchPreference(
                    title = { Text(stringResource(Res.string.hide_video)) },
                    icon = { Icon(painterResource(Res.drawable.slow_motion_video), null) },
                    checked = hideVideo,
                    onCheckedChange = onHideVideoChange,
                )
            }
        }

        AiContentFilterPreferences(
            state = aiContentFilterState,
            onEnabledChange = viewModel::setAiContentFilterEnabled,
            onIncludeModerateChange = viewModel::setAiContentFilterIncludeModerate,
            onRefresh = viewModel::refreshAiContentFilter,
            onOpenSource = viewModel::openAiContentFilterSource,
        )

        PreferenceGroup(title = stringResource(Res.string.app_language)) {
            item {
                SwitchPreference(
                    title = { Text(stringResource(Res.string.use_system_language)) },
                    icon = { Icon(painterResource(Res.drawable.language), null) },
                    checked = useSystemLanguage,
                    onCheckedChange = { checked ->
                        onUseSystemLanguageChange(checked)
                        val newLocale = if (checked) Locale.getDefault() else Locale.ENGLISH
                        setAppLocale(context, newLocale)
                        (context as? moe.rukamori.archivetune.compat.Activity)?.recreate()
                    },
                )
            }
        }

        PreferenceGroup(title = stringResource(Res.string.misc)) {
            item {
                EditTextPreference(
                    title = { Text(stringResource(Res.string.top_length)) },
                    icon = { Icon(painterResource(Res.drawable.trending_up), null) },
                    value = lengthTop,
                    isInputValid = { it.toIntOrNull()?.let { num -> num > 0 } == true },
                    onValueChange = onLengthTopChange,
                )
            }

            item {
                ListPreference(
                    title = { Text(stringResource(Res.string.set_quick_picks)) },
                    icon = { Icon(painterResource(Res.drawable.home_outlined), null) },
                    selectedValue = quickPicks,
                    values = listOf(QuickPicks.QUICK_PICKS, QuickPicks.LAST_LISTEN, QuickPicks.DONT_SHOW),
                    valueText = {
                        when (it) {
                            QuickPicks.QUICK_PICKS -> stringResource(Res.string.quick_picks)
                            QuickPicks.LAST_LISTEN -> stringResource(Res.string.last_song_listened)
                            QuickPicks.DONT_SHOW -> stringResource(Res.string.dont_show)
                        }
                    },
                    onValueSelected = onQuickPicksChange,
                )
            }
        }
    }

    TopAppBar(
        title = { Text(stringResource(Res.string.content)) },
        navigationIcon = {
            IconButton(
                onClick = navController::navigateUp,
                onLongClick = navController::backToMain,
            ) {
                Icon(
                    painterResource(Res.drawable.arrow_back),
                    contentDescription = null,
                )
            }
        },
    )

    Box(Modifier.fillMaxSize()) {
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun AiContentFilterPreferences(
    state: AiContentFilterSettingsState,
    onEnabledChange: (Boolean) -> Unit,
    onIncludeModerateChange: (Boolean) -> Unit,
    onRefresh: () -> Unit,
    onOpenSource: () -> Unit,
) {
    PreferenceGroup(title = stringResource(Res.string.ai_content_filter)) {
        when (state) {
            AiContentFilterSettingsState.Loading -> {
                item {
                    PreferenceEntry(
                        title = { Text(stringResource(Res.string.ai_content_filter)) },
                        description = stringResource(Res.string.loading),
                        icon = { Icon(painterResource(Res.drawable.auto_awesome), null) },
                        isEnabled = false,
                    )
                }
            }

            AiContentFilterSettingsState.Empty -> {
                Unit
            }

            is AiContentFilterSettingsState.Error -> {
                item {
                    PreferenceEntry(
                        title = { Text(stringResource(Res.string.ai_content_filter)) },
                        description = stringResource(state.messageResId),
                        icon = { Icon(painterResource(Res.drawable.auto_awesome), null) },
                        onClick = onRefresh,
                    )
                }
            }

            is AiContentFilterSettingsState.Success -> {
                val model = state.model
                item {
                    SwitchPreference(
                        title = { Text(stringResource(Res.string.ai_content_filter_hide)) },
                        description = stringResource(Res.string.ai_content_filter_hide_summary),
                        icon = { Icon(painterResource(Res.drawable.auto_awesome), null) },
                        checked = model.enabled,
                        onCheckedChange = onEnabledChange,
                    )
                }
                item {
                    SwitchPreference(
                        title = { Text(stringResource(Res.string.ai_content_filter_moderate)) },
                        description = stringResource(Res.string.ai_content_filter_moderate_summary),
                        icon = { Icon(painterResource(Res.drawable.filter_alt), null) },
                        checked = model.includeModerateConfidence,
                        onCheckedChange = onIncludeModerateChange,
                        isEnabled = model.enabled,
                    )
                }
                item {
                    PreferenceEntry(
                        title = { Text(stringResource(Res.string.ai_content_filter_update)) },
                        description =
                            if (model.refreshing) {
                                stringResource(Res.string.loading)
                            } else {
                                stringResource(
                                    Res.string.ai_content_filter_list_counts,
                                    model.blocklistCount,
                                    model.warnlistCount,
                                )
                            },
                        icon = { Icon(painterResource(Res.drawable.sync), null) },
                        onClick = onRefresh,
                        isEnabled = !model.refreshing,
                    )
                }
                item {
                    PreferenceEntry(
                        title = { Text(stringResource(Res.string.ai_content_filter_source)) },
                        description = stringResource(Res.string.ai_content_filter_source_summary),
                        icon = { Icon(painterResource(Res.drawable.info), null) },
                        onClick = onOpenSource,
                    )
                }
            }
        }
    }
}
