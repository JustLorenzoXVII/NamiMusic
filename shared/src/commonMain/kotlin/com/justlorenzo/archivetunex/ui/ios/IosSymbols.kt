package com.justlorenzo.archivetunex.ui.ios

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap
import archivetunex.shared.generated.resources.Res
import archivetunex.shared.generated.resources.add
import archivetunex.shared.generated.resources.airplay
import archivetunex.shared.generated.resources.album
import archivetunex.shared.generated.resources.all_inclusive
import archivetunex.shared.generated.resources.arrow_back
import archivetunex.shared.generated.resources.arrow_downward
import archivetunex.shared.generated.resources.arrow_top_left
import archivetunex.shared.generated.resources.arrow_upward
import archivetunex.shared.generated.resources.artist
import archivetunex.shared.generated.resources.atx_home
import archivetunex.shared.generated.resources.cast
import archivetunex.shared.generated.resources.check
import archivetunex.shared.generated.resources.clear_all
import archivetunex.shared.generated.resources.close
import archivetunex.shared.generated.resources.delete
import archivetunex.shared.generated.resources.delete_history
import archivetunex.shared.generated.resources.download
import archivetunex.shared.generated.resources.drag_handle
import archivetunex.shared.generated.resources.edit
import archivetunex.shared.generated.resources.expand_less
import archivetunex.shared.generated.resources.expand_more
import archivetunex.shared.generated.resources.explicit
import archivetunex.shared.generated.resources.favorite
import archivetunex.shared.generated.resources.favorite_border
import archivetunex.shared.generated.resources.grid_view
import archivetunex.shared.generated.resources.history
import archivetunex.shared.generated.resources.home_filled
import archivetunex.shared.generated.resources.info
import archivetunex.shared.generated.resources.language
import archivetunex.shared.generated.resources.library_add
import archivetunex.shared.generated.resources.library_add_check
import archivetunex.shared.generated.resources.library_filled
import archivetunex.shared.generated.resources.list
import archivetunex.shared.generated.resources.location_on
import archivetunex.shared.generated.resources.login
import archivetunex.shared.generated.resources.logout
import archivetunex.shared.generated.resources.lyrics
import archivetunex.shared.generated.resources.manage_search
import archivetunex.shared.generated.resources.more_horiz
import archivetunex.shared.generated.resources.more_vert
import archivetunex.shared.generated.resources.music_note
import archivetunex.shared.generated.resources.navigate_next
import archivetunex.shared.generated.resources.palette
import archivetunex.shared.generated.resources.pause
import archivetunex.shared.generated.resources.person
import archivetunex.shared.generated.resources.play
import archivetunex.shared.generated.resources.playlist_add
import archivetunex.shared.generated.resources.playlist_play
import archivetunex.shared.generated.resources.queue_music
import archivetunex.shared.generated.resources.radio
import archivetunex.shared.generated.resources.refresh
import archivetunex.shared.generated.resources.remove
import archivetunex.shared.generated.resources.repeat
import archivetunex.shared.generated.resources.repeat_one
import archivetunex.shared.generated.resources.search
import archivetunex.shared.generated.resources.security
import archivetunex.shared.generated.resources.settings
import archivetunex.shared.generated.resources.share
import archivetunex.shared.generated.resources.shuffle
import archivetunex.shared.generated.resources.skip_next
import archivetunex.shared.generated.resources.skip_previous
import archivetunex.shared.generated.resources.sliders
import archivetunex.shared.generated.resources.sort
import archivetunex.shared.generated.resources.stats
import archivetunex.shared.generated.resources.storage
import archivetunex.shared.generated.resources.tune
import org.jetbrains.compose.resources.DrawableResource

/**
 * Le icone dell'app (Material Symbols) e le corrispondenti SF Symbols di Apple: col tema iOS
 * le icone le disegna il sistema, identiche a quelle delle app di Apple.
 */
private val symbols: Map<DrawableResource, String> by lazy {
    mapOf(
        Res.drawable.add to "plus",
        Res.drawable.airplay to "airplayaudio",
        Res.drawable.album to "square.stack",
        Res.drawable.all_inclusive to "infinity",
        Res.drawable.arrow_back to "chevron.backward",
        Res.drawable.arrow_downward to "arrow.down",
        Res.drawable.arrow_top_left to "arrow.up.backward",
        Res.drawable.arrow_upward to "arrow.up",
        Res.drawable.artist to "music.microphone",
        Res.drawable.atx_home to "house.fill",
        Res.drawable.cast to "airplayaudio",
        Res.drawable.check to "checkmark",
        Res.drawable.clear_all to "xmark.circle",
        Res.drawable.close to "xmark",
        Res.drawable.delete to "trash",
        Res.drawable.delete_history to "trash",
        Res.drawable.download to "arrow.down.circle",
        Res.drawable.drag_handle to "line.3.horizontal",
        Res.drawable.edit to "pencil",
        Res.drawable.expand_less to "chevron.up",
        Res.drawable.expand_more to "chevron.down",
        Res.drawable.explicit to "e.square.fill",
        Res.drawable.favorite to "heart.fill",
        Res.drawable.favorite_border to "heart",
        Res.drawable.grid_view to "square.grid.2x2",
        Res.drawable.history to "clock.arrow.circlepath",
        Res.drawable.home_filled to "house.fill",
        Res.drawable.info to "info.circle",
        Res.drawable.language to "globe",
        Res.drawable.library_add to "plus.circle",
        Res.drawable.library_add_check to "checkmark.circle.fill",
        Res.drawable.library_filled to "square.stack.fill",
        Res.drawable.list to "list.bullet",
        Res.drawable.location_on to "location.fill",
        Res.drawable.login to "person.crop.circle.badge.plus",
        Res.drawable.logout to "rectangle.portrait.and.arrow.right",
        Res.drawable.lyrics to "quote.bubble",
        Res.drawable.manage_search to "text.magnifyingglass",
        Res.drawable.more_horiz to "ellipsis",
        Res.drawable.more_vert to "ellipsis",
        Res.drawable.music_note to "music.note",
        Res.drawable.navigate_next to "chevron.forward",
        Res.drawable.palette to "paintpalette",
        Res.drawable.pause to "pause.fill",
        Res.drawable.person to "person.crop.circle",
        Res.drawable.play to "play.fill",
        Res.drawable.playlist_add to "text.badge.plus",
        Res.drawable.playlist_play to "music.note.list",
        Res.drawable.queue_music to "list.bullet",
        Res.drawable.radio to "dot.radiowaves.left.and.right",
        Res.drawable.refresh to "arrow.clockwise",
        Res.drawable.remove to "minus.circle",
        Res.drawable.repeat to "repeat",
        Res.drawable.repeat_one to "repeat.1",
        Res.drawable.search to "magnifyingglass",
        Res.drawable.security to "lock.shield",
        Res.drawable.settings to "gearshape",
        Res.drawable.share to "square.and.arrow.up",
        Res.drawable.shuffle to "shuffle",
        Res.drawable.skip_next to "forward.fill",
        Res.drawable.skip_previous to "backward.fill",
        Res.drawable.sliders to "slider.horizontal.3",
        Res.drawable.sort to "arrow.up.arrow.down",
        Res.drawable.stats to "chart.bar.fill",
        Res.drawable.storage to "internaldrive",
        Res.drawable.tune to "slider.horizontal.3",
    )
}

/** Il nome della SF Symbol per un'icona dell'app (null se non c'è un equivalente). */
fun iosSymbolFor(icon: DrawableResource): String? = symbols[icon]

/**
 * L'immagine di una SF Symbol disegnata dal sistema, bianca (si colora con la tinta dell'icona).
 * [pointSize] è la grandezza in punti; null dove le SF Symbols non esistono (Android).
 */
@Composable
expect fun rememberSystemSymbol(
    name: String,
    pointSize: Float,
    bold: Boolean = false,
): ImageBitmap?
