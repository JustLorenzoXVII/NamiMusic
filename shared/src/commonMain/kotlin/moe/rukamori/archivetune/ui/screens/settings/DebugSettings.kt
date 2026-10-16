/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package moe.rukamori.archivetune.ui.screens.settings

import archivetunex.shared.generated.resources.*

import moe.rukamori.archivetune.compat.format

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import moe.rukamori.archivetune.compat.LocalContext
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.booleanPreferencesKey
import moe.rukamori.archivetune.compat.media3.Player
import androidx.navigation.NavController
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import moe.rukamori.archivetune.BuildConfig
import moe.rukamori.archivetune.LeakCanaryController
import moe.rukamori.archivetune.LeakCanaryToggle
import moe.rukamori.archivetune.LocalPlayerAwareWindowInsets
import moe.rukamori.archivetune.LocalPlayerConnection
import moe.rukamori.archivetune.ui.component.IconButton
import moe.rukamori.archivetune.ui.component.PreferenceEntry
import moe.rukamori.archivetune.ui.component.PreferenceGroup
import moe.rukamori.archivetune.ui.component.SwitchPreference
import moe.rukamori.archivetune.ui.utils.backToMain
import moe.rukamori.archivetune.utils.makeTimeString
import moe.rukamori.archivetune.utils.rememberPreference
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugSettings(navController: NavController) {
    val (showDevDebug, onShowDevDebugChange) =
        rememberPreference(
            key = booleanPreferencesKey("dev_show_discord_debug"),
            defaultValue = false,
        )

    val (showNerdStats, onShowNerdStatsChange) =
        rememberPreference(
            key = booleanPreferencesKey("dev_show_nerd_stats"),
            defaultValue = false,
        )

    val (showCodecOnPlayer, onShowCodecOnPlayerChange) =
        rememberPreference(
            key = booleanPreferencesKey("show_codec_on_player"),
            defaultValue = false,
        )

    val leakCanaryPreference =
        if (BuildConfig.LEAK_CANARY_TOGGLE_AVAILABLE) {
            rememberPreference(
                key = booleanPreferencesKey(LeakCanaryToggle.PREFERENCE_KEY),
                defaultValue = false,
            )
        } else {
            null
        }
    val context = LocalContext.current

    val playerConnection = LocalPlayerConnection.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(Res.string.experiment_settings),
                            style = MaterialTheme.typography.titleLarge,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = navController::navigateUp,
                        onLongClick = navController::backToMain,
                    ) {
                        Icon(painterResource(Res.drawable.arrow_back), contentDescription = null)
                    }
                },
            )
        },
    ) { innerPadding: PaddingValues ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .windowInsetsPadding(
                        LocalPlayerAwareWindowInsets.current.only(
                            WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom,
                        ),
                    ).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PreferenceGroup(title = stringResource(Res.string.experimental_features)) {
                item {
                    SwitchPreference(
                        title = { Text(stringResource(Res.string.show_discord_debug_ui)) },
                        description = stringResource(Res.string.enable_discord_debug_lines),
                        icon = { Icon(painterResource(Res.drawable.discord), null) },
                        checked = showDevDebug,
                        onCheckedChange = onShowDevDebugChange,
                    )
                }

                item {
                    SwitchPreference(
                        title = { Text(stringResource(Res.string.show_nerd_stats)) },
                        description = stringResource(Res.string.description_show_nerd_stats),
                        icon = { Icon(painterResource(Res.drawable.stats), null) },
                        checked = showNerdStats,
                        onCheckedChange = onShowNerdStatsChange,
                    )
                }

                item {
                    SwitchPreference(
                        title = { Text(stringResource(Res.string.display_codec_on_player)) },
                        description = stringResource(Res.string.description_display_codec_on_player),
                        icon = { Icon(painterResource(Res.drawable.graphic_eq), null) },
                        checked = showCodecOnPlayer,
                        onCheckedChange = onShowCodecOnPlayerChange,
                    )
                }

                leakCanaryPreference?.let { preference ->
                    item {
                        SwitchPreference(
                            title = { Text(stringResource(Res.string.enable_leak_canary)) },
                            description = stringResource(Res.string.enable_leak_canary_description),
                            icon = { Icon(painterResource(Res.drawable.experiment), null) },
                            checked = preference.value,
                            onCheckedChange = { enabled ->
                                preference.value = enabled
                                LeakCanaryController.setEnabled(context, enabled)
                            },
                        )
                    }
                }

                item {
                    PreferenceEntry(
                        title = { Text(stringResource(Res.string.debug_logs)) },
                        description = stringResource(Res.string.view_debug_logs_description),
                        icon = {
                            Icon(
                                painter = painterResource(Res.drawable.manage_search),
                                contentDescription = null,
                            )
                        },
                        trailingContent = {
                            Icon(
                                painter = painterResource(Res.drawable.navigate_next),
                                contentDescription = null,
                            )
                        },
                        onClick = { navController.navigate("settings/logcat") },
                    )
                }
            }

            AnimatedVisibility(
                visible = showDevDebug,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Spacer(modifier = Modifier.height(8.dp))
                    DiscordDebugSection()
                }
            }

            AnimatedVisibility(
                visible = showNerdStats && playerConnection != null,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Spacer(modifier = Modifier.height(8.dp))
                    NerdStatsSection(playerConnection = playerConnection)
                }
            }

            Spacer(modifier = Modifier.height(SettingsDimensions.ScreenBottomPadding))
        }
    }
}

@Composable
private fun DiscordDebugSection() {
    val lastStartTs: Long? by DiscordPresenceManager.lastRpcStartTimeFlow.collectAsState(initial = null)
    val lastEndTs: Long? by DiscordPresenceManager.lastRpcEndTimeFlow.collectAsState(initial = null)
    val lastStart: String = lastStartTs?.let { makeTimeString(it) } ?: "—"
    val lastEnd: String = lastEndTs?.let { makeTimeString(it) } ?: "—"
    val isRunning = DiscordPresenceManager.isRunning()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        shape = CircleShape,
                        color =
                            if (isRunning) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.errorContainer
                            },
                        modifier = Modifier.size(48.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                painter = painterResource(Res.drawable.discord),
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                                tint =
                                    if (isRunning) {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onErrorContainer
                                    },
                            )
                        }
                    }
                    Column {
                        Text(
                            text = stringResource(Res.string.discord_integration),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text =
                                if (isRunning) {
                                    stringResource(Res.string.presence_manager_running)
                                } else {
                                    stringResource(Res.string.presence_manager_stopped)
                                },
                            style = MaterialTheme.typography.bodySmall,
                            color =
                                if (isRunning) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.error
                                },
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color =
                        if (isRunning) {
                            Color(0xFF43B581).copy(alpha = 0.2f)
                        } else {
                            MaterialTheme.colorScheme.error.copy(alpha = 0.2f)
                        },
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier =
                                Modifier
                                    .size(8.dp)
                                    .background(
                                        color = if (isRunning) Color(0xFF43B581) else MaterialTheme.colorScheme.error,
                                        shape = CircleShape,
                                    ),
                        )
                        Text(
                            text = if (isRunning) stringResource(Res.string.status_active) else stringResource(Res.string.status_inactive),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isRunning) Color(0xFF43B581) else MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                DebugTimestampItem(
                    label = stringResource(Res.string.last_start),
                    value = lastStart,
                    icon = Res.drawable.play,
                )
                DebugTimestampItem(
                    label = stringResource(Res.string.last_end),
                    value = lastEnd,
                    icon = Res.drawable.pause,
                )
            }
        }
    }
}

@Composable
private fun DebugTimestampItem(
    label: String,
    value: String,
    icon: Int,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.secondaryContainer,
            modifier = Modifier.size(36.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun NerdStatsSection(playerConnection: moe.rukamori.archivetune.playback.PlayerConnection?) {
    if (playerConnection == null) return

    val currentFormat by playerConnection.currentFormat.collectAsState(initial = null)
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val player = playerConnection.player

    var bufferPercentage by remember { mutableStateOf(0) }
    var bufferedPosition by remember { mutableStateOf(0L) }
    var currentPosition by remember { mutableStateOf(0L) }
    var playbackSpeed by remember { mutableStateOf(1.0f) }

    LaunchedEffect(Unit) {
        while (isActive) {
            bufferPercentage = player.bufferedPercentage
            bufferedPosition = player.bufferedPosition
            currentPosition = player.currentPosition
            playbackSpeed = player.playbackParameters.speed
            delay(500)
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(48.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(Res.drawable.graphic_eq),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
                Column {
                    Text(
                        text = stringResource(Res.string.nerd_stats),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = stringResource(Res.string.real_time_playback_stats),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            if (mediaMetadata != null) {
                NerdStatCard(
                    icon = Res.drawable.music_note,
                    label = stringResource(Res.string.track_label),
                    value = mediaMetadata?.title ?: stringResource(Res.string.no_track_playing),
                    accentColor = MaterialTheme.colorScheme.primary,
                )

                if (currentFormat != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        NerdStatChip(
                            icon = Res.drawable.graphic_eq,
                            label = stringResource(Res.string.codec_label),
                            value =
                                currentFormat?.mimeType?.substringAfter("/")?.uppercase()
                                    ?: stringResource(Res.string.unknown_codec),
                            modifier = Modifier.weight(1f),
                        )

                        val bitrateKbps = currentFormat?.bitrate?.let { it / 1000 } ?: 0
                        NerdStatChip(
                            icon = Res.drawable.speed,
                            label = stringResource(Res.string.bitrate_label),
                            value = if (bitrateKbps > 0) "$bitrateKbps kbps" else stringResource(Res.string.unknown_bitrate),
                            modifier = Modifier.weight(1f),
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        val sampleRateKhz = currentFormat?.sampleRate?.let { (it / 1000.0).roundToInt() } ?: 0
                        NerdStatChip(
                            icon = Res.drawable.waves,
                            label = stringResource(Res.string.sample_rate_label),
                            value = if (sampleRateKhz > 0) "$sampleRateKhz kHz" else stringResource(Res.string.unknown_sample_rate),
                            modifier = Modifier.weight(1f),
                        )

                        NerdStatChip(
                            icon = Res.drawable.storage,
                            label = stringResource(Res.string.content_length_label),
                            value =
                                currentFormat?.contentLength?.let {
                                    if (it > 0) {
                                        "${String.format("%.2f", it / 1024.0 / 1024.0)} MB"
                                    } else {
                                        stringResource(Res.string.unknown_content_length)
                                    }
                                } ?: stringResource(Res.string.unknown_content_length),
                            modifier = Modifier.weight(1f),
                        )
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            LinearWavyProgressIndicator(
                                modifier = Modifier.size(24.dp),
                            )
                            Text(
                                text = stringResource(Res.string.loading_format),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                val bufferDuration = ((bufferedPosition - currentPosition) / 1000.0).roundToInt()
                val bufferProgress by animateFloatAsState(
                    targetValue = bufferPercentage / 100f,
                    animationSpec = tween(300),
                    label = "buffer",
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(Res.string.buffer_health_label),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = "$bufferPercentage% ($bufferDuration sec ahead)",
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Medium,
                        )
                    }

                    LinearWavyProgressIndicator(
                        progress = { bufferProgress },
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                        color =
                            when {
                                bufferPercentage > 70 -> Color(0xFF43B581)
                                bufferPercentage > 30 -> MaterialTheme.colorScheme.tertiary
                                else -> MaterialTheme.colorScheme.error
                            },
                        trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    val playbackStateText =
                        when (player.playbackState) {
                            Player.STATE_IDLE -> stringResource(Res.string.playback_state_idle)
                            Player.STATE_BUFFERING -> stringResource(Res.string.playback_state_buffering)
                            Player.STATE_READY -> stringResource(Res.string.playback_state_ready)
                            Player.STATE_ENDED -> stringResource(Res.string.playback_state_ended)
                            else -> stringResource(Res.string.playback_state_unknown)
                        }

                    val stateColor =
                        when (player.playbackState) {
                            Player.STATE_READY -> Color(0xFF43B581)
                            Player.STATE_BUFFERING -> MaterialTheme.colorScheme.tertiary
                            Player.STATE_IDLE -> MaterialTheme.colorScheme.outline
                            else -> MaterialTheme.colorScheme.error
                        }

                    NerdStatChip(
                        icon = Res.drawable.status,
                        label = stringResource(Res.string.state_label),
                        value = playbackStateText,
                        modifier = Modifier.weight(1f),
                        valueColor = stateColor,
                    )

                    NerdStatChip(
                        icon = Res.drawable.slow_motion_video,
                        label = stringResource(Res.string.playback_speed_label),
                        value = "${playbackSpeed}x",
                        modifier = Modifier.weight(1f),
                    )
                }

                OutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                painter = painterResource(Res.drawable.token),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = stringResource(Res.string.media_id_label),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(
                            text = mediaMetadata?.id?.take(16)?.plus("...") ?: "N/A",
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                        )
                    }
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.music_note),
                            contentDescription = null,
                            modifier =
                                Modifier
                                    .size(48.dp)
                                    .alpha(0.5f),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = stringResource(Res.string.no_track_playing),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NerdStatCard(
    icon: Int,
    label: String,
    value: String,
    accentColor: Color,
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = accentColor.copy(alpha = 0.1f),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = CircleShape,
                color = accentColor.copy(alpha = 0.2f),
                modifier = Modifier.size(36.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(icon),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = accentColor,
                    )
                }
            }
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun NerdStatChip(
    icon: Int,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium,
                color = valueColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
