/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.rukamori.archivetune.ui.screens.settings

import archivetunex.shared.generated.resources.*

import moe.rukamori.archivetune.compat.ActivityNotFoundException
import moe.rukamori.archivetune.compat.Context
import moe.rukamori.archivetune.compat.Intent
import moe.rukamori.archivetune.compat.startActivity
import moe.rukamori.archivetune.compat.Uri
import moe.rukamori.archivetune.compat.Settings
import moe.rukamori.archivetune.compat.Toast
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import androidx.navigation.NavController
import moe.rukamori.archivetune.BuildConfig

@Composable
fun buildSettingsGroups(
    navController: NavController,
    isAndroid12OrLater: Boolean,
    hasUpdate: Boolean,
    context: Context,
): List<SettingsGroup> {
    val account =
        SettingsItem(
            key = "account",
            icon = painterResource(Res.drawable.account),
            title = stringResource(Res.string.account),
            subtitle = stringResource(Res.string.settings_account_subtitle),
            accentColor = MaterialTheme.colorScheme.primary,
            onClick = { navController.navigate("settings/account") },
        )
    val stats =
        SettingsItem(
            key = "stats",
            icon = painterResource(Res.drawable.stats),
            title = stringResource(Res.string.settings_stats_title),
            subtitle = stringResource(Res.string.settings_stats_subtitle),
            accentColor = MaterialTheme.colorScheme.primary,
            onClick = { navController.navigate("stats") },
        )
    val appearance =
        SettingsItem(
            key = "appearance",
            icon = painterResource(Res.drawable.palette),
            title = stringResource(Res.string.appearance),
            subtitle = stringResource(Res.string.settings_appearance_subtitle),
            accentColor = MaterialTheme.colorScheme.secondary,
            onClick = { navController.navigate("settings/appearance") },
        )
    val playback =
        SettingsItem(
            key = "playback",
            icon = painterResource(Res.drawable.music_note),
            title = stringResource(Res.string.settings_playback_title),
            subtitle = stringResource(Res.string.settings_playback_subtitle),
            accentColor = MaterialTheme.colorScheme.tertiary,
            onClick = { navController.navigate("settings/player") },
        )
    val canvas =
        SettingsItem(
            key = "canvas",
            icon = painterResource(Res.drawable.motion_photos_on),
            title = stringResource(Res.string.archivetune_canvas),
            subtitle = stringResource(Res.string.canvas_settings_subtitle),
            accentColor = MaterialTheme.colorScheme.tertiary,
            onClick = { navController.navigate("settings/canvas") },
        )
    val lyrics =
        SettingsItem(
            key = "lyrics",
            icon = painterResource(Res.drawable.lyrics),
            title = stringResource(Res.string.lyrics),
            subtitle = stringResource(Res.string.settings_lyrics_subtitle),
            accentColor = MaterialTheme.colorScheme.secondary,
            onClick = { navController.navigate("settings/lyrics") },
        )
    val content =
        SettingsItem(
            key = "content",
            icon = painterResource(Res.drawable.language),
            title = stringResource(Res.string.content),
            subtitle = stringResource(Res.string.settings_content_subtitle),
            accentColor = MaterialTheme.colorScheme.primary,
            onClick = { navController.navigate("settings/content") },
        )
    val behavior =
        SettingsItem(
            key = "behavior",
            icon = painterResource(Res.drawable.swipe),
            title = stringResource(Res.string.settings_behavior_title),
            subtitle = stringResource(Res.string.settings_behavior_subtitle),
            accentColor = MaterialTheme.colorScheme.primary,
            onClick = { navController.navigate("settings/privacy") },
        )
    val integration =
        SettingsItem(
            key = "integration",
            icon = painterResource(Res.drawable.auto_awesome),
            title = stringResource(Res.string.integration),
            subtitle = stringResource(Res.string.settings_integration_subtitle),
            accentColor = MaterialTheme.colorScheme.secondary,
            onClick = { navController.navigate("settings/integration") },
        )
    val androidAuto =
        SettingsItem(
            key = "android_auto",
            icon = painterResource(Res.drawable.directions_car),
            title = stringResource(Res.string.android_auto),
            subtitle = stringResource(Res.string.android_auto_settings_subtitle),
            accentColor = MaterialTheme.colorScheme.primary,
            onClick = { navController.navigate("settings/android_auto") },
        )
    val aiIntegration =
        SettingsItem(
            key = "ai_integration",
            icon = painterResource(Res.drawable.ai),
            title = stringResource(Res.string.ai_integration),
            subtitle = stringResource(Res.string.ai_integration_desc),
            accentColor = MaterialTheme.colorScheme.secondary,
            onClick = { navController.navigate("settings/ai_integration") },
        )
    val internet =
        SettingsItem(
            key = "internet",
            icon = painterResource(Res.drawable.wifi_proxy),
            title = stringResource(Res.string.internet),
            subtitle = stringResource(Res.string.settings_internet_subtitle),
            accentColor = MaterialTheme.colorScheme.tertiary,
            onClick = { navController.navigate("settings/internet") },
        )
    val storage =
        SettingsItem(
            key = "storage",
            icon = painterResource(Res.drawable.storage),
            title = stringResource(Res.string.storage),
            subtitle = stringResource(Res.string.settings_storage_subtitle),
            accentColor = MaterialTheme.colorScheme.primary,
            onClick = { navController.navigate("settings/storage") },
        )
    val backupRestore =
        SettingsItem(
            key = "backup_restore",
            icon = painterResource(Res.drawable.backup),
            title = stringResource(Res.string.backup_restore),
            subtitle = stringResource(Res.string.settings_backup_restore_subtitle),
            accentColor = MaterialTheme.colorScheme.primary,
            onClick = { navController.navigate("settings/backup_restore") },
        )
    val developerOptions =
        SettingsItem(
            key = "developer_options",
            icon = painterResource(Res.drawable.experiment),
            title = stringResource(Res.string.settings_developer_options_title),
            subtitle = stringResource(Res.string.settings_developer_options_subtitle),
            accentColor = MaterialTheme.colorScheme.tertiary,
            onClick = { navController.navigate("settings/misc") },
        )
    val defaultLinks =
        if (isAndroid12OrLater) {
            SettingsItem(
                key = "default_links",
                icon = painterResource(Res.drawable.link),
                title = stringResource(Res.string.default_links),
                subtitle = stringResource(Res.string.open_supported_links),
                accentColor = MaterialTheme.colorScheme.secondary,
                onClick = {
                    try {
                        val intent =
                            Intent(
                                Settings.ACTION_APP_OPEN_BY_DEFAULT_SETTINGS,
                                Uri.parse("package:${context.packageName}"),
                            ).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        when (e) {
                            is ActivityNotFoundException,
                            is SecurityException,
                            -> {
                                Toast
                                    .makeText(
                                        context,
                                        Res.string.open_app_settings_error,
                                        Toast.LENGTH_LONG,
                                    ).show()
                            }

                            else -> {
                                Toast
                                    .makeText(
                                        context,
                                        Res.string.open_app_settings_error,
                                        Toast.LENGTH_LONG,
                                    ).show()
                            }
                        }
                    }
                },
            )
        } else {
            null
        }
    val updates =
        if (BuildConfig.UPDATER_AVAILABLE) {
            SettingsItem(
                key = "updates",
                icon = painterResource(Res.drawable.update),
                title = stringResource(Res.string.updates),
                subtitle =
                    if (hasUpdate) {
                        stringResource(Res.string.new_version_available)
                    } else {
                        stringResource(Res.string.settings_updates_subtitle)
                    },
                showUpdateIndicator = hasUpdate,
                accentColor =
                    if (hasUpdate) {
                        MaterialTheme.colorScheme.tertiary
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                badge = if (hasUpdate) "v${BuildConfig.VERSION_NAME}" else BuildConfig.VERSION_NAME,
                onClick = { navController.navigate("settings/update") },
            )
        } else {
            null
        }
    val about =
        SettingsItem(
            key = "about",
            icon = painterResource(Res.drawable.info),
            title = stringResource(Res.string.about),
            subtitle = stringResource(Res.string.settings_about_subtitle),
            accentColor = MaterialTheme.colorScheme.secondary,
            onClick = { navController.navigate("settings/about") },
        )

    return listOf(
        SettingsGroup(
            title = stringResource(Res.string.settings),
            items = listOf(account, stats),
        ),
        SettingsGroup(
            title = stringResource(Res.string.settings_section_player_content),
            items = listOf(appearance, playback, canvas, lyrics, content, behavior),
        ),
        SettingsGroup(
            title = stringResource(Res.string.integration),
            items = listOf(androidAuto, integration, aiIntegration, internet),
        ),
        SettingsGroup(
            title = stringResource(Res.string.storage),
            items = listOf(storage, backupRestore),
        ),
        SettingsGroup(
            title = stringResource(Res.string.about),
            items =
                buildList {
                    add(developerOptions)
                    defaultLinks?.let(::add)
                    updates?.let(::add)
                    add(about)
                },
        ),
    )
}
