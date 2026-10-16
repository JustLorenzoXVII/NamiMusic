/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.rukamori.archivetune.appicon

import archivetunex.shared.generated.resources.*

import kotlinx.coroutines.IO

import moe.rukamori.archivetune.compat.Context
import moe.rukamori.archivetune.compat.Build
import org.jetbrains.compose.resources.DrawableResource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

data class AppIcon(
    val id: String,
    val name: String?,
    val author: String?,
    val githubAuthorUrl: String?,
    val previewDrawableResId: DrawableResource,
    val isDefault: Boolean,
)

data class AppIconCatalog(
    val icons: List<AppIcon>,
    val selectedIconId: String,
)

/**
 * Icone alternative dell'app. Su Android l'originale cambiava gli alias del launcher;
 * in ArchiveTuneX (iPhone) c'è solo l'icona predefinita.
 */
class AppIconRepository constructor(
    private val context: Context,
) {
    private val catalog =
        AppIconCatalog(
            icons =
                listOf(
                    AppIcon(
                        id = DefaultIconId,
                        name = null,
                        author = null,
                        githubAuthorUrl = null,
                        previewDrawableResId = Res.drawable.app_icon_small,
                        isDefault = true,
                    ),
                ),
            selectedIconId = DefaultIconId,
        )

    suspend fun loadCatalog(): AppIconCatalog = catalog

    suspend fun restoreLauncherEntry() = Unit

    suspend fun selectIcon(iconId: String): AppIconCatalog = catalog

    companion object {
        const val DefaultIconId = "default"
    }
}
