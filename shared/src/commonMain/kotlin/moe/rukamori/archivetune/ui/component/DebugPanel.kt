/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.rukamori.archivetune.ui.component

import archivetunex.shared.generated.resources.*

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.painterResource

/**
 * Returns a `Material3SettingsItem` that can be placed inside a `Material3SettingsGroup`.
 * The caller should supply composables or values for the dynamic content.
 */
@Composable
fun DebugPanelItem(
    title: @Composable () -> Unit,
    description: (@Composable () -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
): Material3SettingsItem =
    Material3SettingsItem(
        icon = painterResource(Res.drawable.info),
        title = title,
        description = description,
        trailingContent = trailingContent,
        isHighlighted = true,
    )
