/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.rukamori.archivetune.ui.screens

import archivetunex.shared.generated.resources.*

import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import androidx.compose.runtime.Immutable

@Immutable
sealed class Screens(
    val titleId: StringResource,
    val iconIdInactive: DrawableResource,
    val iconIdActive: DrawableResource,
    val route: String,
) {
    object Home : Screens(
        titleId = Res.string.home,
        iconIdInactive = Res.drawable.home_outlined,
        iconIdActive = Res.drawable.home_filled,
        route = "home",
    )

    object Search : Screens(
        titleId = Res.string.search,
        iconIdInactive = Res.drawable.search,
        iconIdActive = Res.drawable.search,
        route = "search",
    )

    object Library : Screens(
        titleId = Res.string.filter_library,
        iconIdInactive = Res.drawable.library_outlined,
        iconIdActive = Res.drawable.library_filled,
        route = "library",
    )

    object MoodAndGenres : Screens(
        titleId = Res.string.mood_and_genres,
        iconIdInactive = Res.drawable.style,
        iconIdActive = Res.drawable.style,
        route = "mood_and_genres",
    )

    companion object {
        val MainScreens = listOf(Home, Search, Library)
        val TvMainScreens = listOf(Home, Search, Library)
    }
}
