package com.justlorenzo.archivetunex.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import moe.rukamori.archivetune.compat.AppContext
import moe.rukamori.archivetune.extensions.toEnum
import moe.rukamori.archivetune.utils.dataStore

// region Opzioni

/** Colore d'accento dell'app. */
enum class AccentChoice(
    val label: String,
    val color: Color?,
) {
    Fuji("Fuji", Color(0xFFFA4058)),
    Dynamic("Dalla copertina", null),
    Orange("Arancio", Color(0xFFFF7A2F)),
    Yellow("Giallo", Color(0xFFF5B800)),
    Green("Verde", Color(0xFF1DB954)),
    Teal("Acqua", Color(0xFF00BFA6)),
    Blue("Blu", Color(0xFF3D7BFF)),
    Purple("Viola", Color(0xFF8B5CF6)),
    Pink("Rosa", Color(0xFFFF5FA2)),
}

/** Angoli delle copertine. */
enum class ArtworkCorners(
    val label: String,
    val radius: Dp,
) {
    Small("Squadrati", 8.dp),
    Medium("Arrotondati", 20.dp),
    Large("Molto arrotondati", 36.dp),
}

/** Sfondo del player a schermo intero. */
enum class PlayerBackground(
    val label: String,
) {
    Gradient("Sfumatura"),
    Blur("Copertina sfocata"),
    Solid("Colore pieno"),
    Plain("Tema"),
}

/** Barra di avanzamento del player. */
enum class SliderStyleChoice(
    val label: String,
) {
    Wavy("Ondulata"),
    Thick("Spessa"),
    Classic("Classica"),
}

/** Forma del pulsante Play. */
enum class PlayButtonShape(
    val label: String,
) {
    CookieSpin("Biscotto che ruota"),
    Cookie("Biscotto"),
    Circle("Cerchio"),
    Squircle("Quadrato arrotondato"),
}

/** Aspetto del mini player. */
enum class MiniPlayerStyle(
    val label: String,
) {
    Tinted("Tinta copertina"),
    Accent("Colore pieno"),
    Neutral("Neutro"),
}

/** Nomi delle schede in basso. */
enum class TabLabels(
    val label: String,
) {
    Selected("Solo quella attiva"),
    Always("Sempre"),
    Never("Mai"),
}

// endregion

// region Chiavi (preferenze di ArchiveTuneX, separate da quelle di ArchiveTune)

val AccentKey = stringPreferencesKey("atx_accent")
val ArtworkCornersKey = stringPreferencesKey("atx_artwork_corners")
val PlayerBackgroundKey = stringPreferencesKey("atx_player_background")
val SliderStyleKey = stringPreferencesKey("atx_slider_style")
val PlayButtonShapeKey = stringPreferencesKey("atx_play_button_shape")
val ShrinkArtworkOnPauseKey = booleanPreferencesKey("atx_shrink_artwork_on_pause")
val ShowRemainingTimeKey = booleanPreferencesKey("atx_show_remaining_time")
val ColoredPlayerControlsKey = booleanPreferencesKey("atx_colored_player_controls")
val MiniPlayerStyleKey = stringPreferencesKey("atx_mini_player_style")
val MiniPlayerProgressKey = booleanPreferencesKey("atx_mini_player_progress")
val MiniPlayerNextKey = booleanPreferencesKey("atx_mini_player_next")
val TabLabelsKey = stringPreferencesKey("atx_tab_labels")
val ColoredHomeKey = booleanPreferencesKey("atx_colored_home")

// endregion

/** Tutte le scelte di stile dell'app, lette insieme. */
@Immutable
data class AtxStyle(
    val accent: AccentChoice = AccentChoice.Fuji,
    val corners: ArtworkCorners = ArtworkCorners.Large,
    val playerBackground: PlayerBackground = PlayerBackground.Gradient,
    val slider: SliderStyleChoice = SliderStyleChoice.Wavy,
    val playButton: PlayButtonShape = PlayButtonShape.CookieSpin,
    val shrinkArtworkOnPause: Boolean = true,
    val showRemainingTime: Boolean = true,
    val coloredPlayerControls: Boolean = true,
    val miniPlayer: MiniPlayerStyle = MiniPlayerStyle.Tinted,
    val miniPlayerProgress: Boolean = true,
    val miniPlayerNext: Boolean = true,
    val tabLabels: TabLabels = TabLabels.Selected,
    val coloredHome: Boolean = true,
)

private fun Preferences.toAtxStyle(): AtxStyle {
    val defaults = AtxStyle()
    return AtxStyle(
        accent = this[AccentKey].toEnum(defaults.accent),
        corners = this[ArtworkCornersKey].toEnum(defaults.corners),
        playerBackground = this[PlayerBackgroundKey].toEnum(defaults.playerBackground),
        slider = this[SliderStyleKey].toEnum(defaults.slider),
        playButton = this[PlayButtonShapeKey].toEnum(defaults.playButton),
        shrinkArtworkOnPause = this[ShrinkArtworkOnPauseKey] ?: defaults.shrinkArtworkOnPause,
        showRemainingTime = this[ShowRemainingTimeKey] ?: defaults.showRemainingTime,
        coloredPlayerControls = this[ColoredPlayerControlsKey] ?: defaults.coloredPlayerControls,
        miniPlayer = this[MiniPlayerStyleKey].toEnum(defaults.miniPlayer),
        miniPlayerProgress = this[MiniPlayerProgressKey] ?: defaults.miniPlayerProgress,
        miniPlayerNext = this[MiniPlayerNextKey] ?: defaults.miniPlayerNext,
        tabLabels = this[TabLabelsKey].toEnum(defaults.tabLabels),
        coloredHome = this[ColoredHomeKey] ?: defaults.coloredHome,
    )
}

@Composable
fun rememberAtxStyle(): AtxStyle {
    val flow = remember { AppContext.dataStore.data.map { it.toAtxStyle() }.distinctUntilChanged() }
    val style by flow.collectAsState(AtxStyle())
    return style
}

val LocalAtxStyle = staticCompositionLocalOf { AtxStyle() }
