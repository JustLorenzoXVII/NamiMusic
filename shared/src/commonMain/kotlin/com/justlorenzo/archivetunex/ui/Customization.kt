package com.justlorenzo.archivetunex.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import moe.rukamori.archivetune.compat.AppContext
import moe.rukamori.archivetune.extensions.toEnum
import moe.rukamori.archivetune.utils.dataStore

// region Tema

/** Da dove prende i colori l'app (sfondi compresi, non solo l'accento). */
enum class ThemeSource(
    val label: String,
) {
    System("Colori di sistema"),
    Album("Colori della copertina"),
    Custom("Colore personalizzato"),
}

/** Come la tavolozza Material viene costruita dal colore di partenza. */
enum class PaletteStyleChoice(
    val label: String,
) {
    TonalSpot("Tonale"),
    Vibrant("Vivace"),
    Expressive("Espressiva"),
    Fidelity("Fedele"),
    Content("Contenuto"),
    Rainbow("Arcobaleno"),
    FruitSalad("Macedonia"),
    Neutral("Neutra"),
}

/** Colori pronti per il colore personalizzato. */
val PresetColors =
    listOf(
        Color(0xFFFF8FB8),
        Color(0xFFFA4058),
        Color(0xFFFF7A2F),
        Color(0xFFF5B800),
        Color(0xFF8BC34A),
        Color(0xFF1DB954),
        Color(0xFF00BFA6),
        Color(0xFF00A8E8),
        Color(0xFF3D7BFF),
        Color(0xFF6C5CE7),
        Color(0xFF8B5CF6),
        Color(0xFFD946EF),
        Color(0xFFFF5FA2),
        Color(0xFF8D6E63),
        Color(0xFF607D8B),
    )

/** Angoli delle copertine. */
enum class ArtworkCorners(
    val label: String,
    val radius: Dp,
) {
    Small("Squadrati", 8.dp),
    Medium("Arrotondati", 20.dp),
    Large("Molto arrotondati", 36.dp),
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

// region Player

/** Disposizione del player a schermo intero. */
enum class PlayerLayout(
    val label: String,
) {
    Card("Copertina"),
    Vinyl("Vinile"),
    Stack("Pila"),
    Cassette("Cassetta"),
    Poster("Poster"),
}

/** Sfondo del player a schermo intero. */
enum class PlayerBackground(
    val label: String,
) {
    Glow("Bagliore"),
    Gradient("Sfumatura"),
    Blur("Copertina sfocata"),
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

/** Pulsanti del player: gruppo connesso (come Fuji) o Play a forma scelta. */
enum class PlayButtonShape(
    val label: String,
) {
    Group("Gruppo connesso"),
    CookieSpin("Biscotto che ruota"),
    Cookie("Biscotto"),
    Circle("Cerchio"),
    Squircle("Quadrato arrotondato"),
}

// endregion

// region Mini player

/** Forma e posizione del mini player. */
enum class MiniLayout(
    val label: String,
) {
    Joined("Unito alla barra"),
    Floating("Fluttuante"),
    Pill("Pillola"),
    Large("Grande"),
}

/** Colore del mini player. */
enum class MiniPlayerStyle(
    val label: String,
) {
    Tinted("Tinta copertina"),
    Gradient("Sfumatura"),
    Accent("Colore pieno"),
    Neutral("Neutro"),
}

/** Come il mini player mostra l'avanzamento. */
enum class MiniProgress(
    val label: String,
) {
    Line("Linea"),
    Wave("Onda"),
    Frame("Cornice"),
    Fill("Riempimento"),
    Ring("Anello"),
    None("Nessuno"),
}

/** Forma della copertina nel mini player. */
enum class MiniArtwork(
    val label: String,
) {
    Rounded("Arrotondata"),
    Circle("Cerchio"),
    Cookie("Biscotto"),
    Vinyl("Vinile che gira"),
}

/** Pulsanti del mini player. */
enum class MiniButtons(
    val label: String,
) {
    PlayOnly("Solo Play"),
    PlayNext("Play e successivo"),
    All("Tutti e tre"),
}

// endregion

// region Chiavi (preferenze di ArchiveTuneX, separate da quelle di ArchiveTune)

val ThemeSourceKey = stringPreferencesKey("atx_theme_source")
val CustomColorKey = intPreferencesKey("atx_custom_color")
val PaletteStyleKey = stringPreferencesKey("atx_palette_style")
val PureBlackKey = booleanPreferencesKey("atx_pure_black")
val ArtworkCornersKey = stringPreferencesKey("atx_artwork_corners")
val TabLabelsKey = stringPreferencesKey("atx_tab_labels")
val ColoredHomeKey = booleanPreferencesKey("atx_colored_home")

val PlayerLayoutKey = stringPreferencesKey("atx_player_layout")
val PlayerBackgroundKey = stringPreferencesKey("atx_player_background")
val SliderStyleKey = stringPreferencesKey("atx_slider_style")
val PlayButtonShapeKey = stringPreferencesKey("atx_play_button_shape")
val ShrinkArtworkOnPauseKey = booleanPreferencesKey("atx_shrink_artwork_on_pause")
val ShowRemainingTimeKey = booleanPreferencesKey("atx_show_remaining_time")
val ColoredPlayerControlsKey = booleanPreferencesKey("atx_colored_player_controls")

val MiniLayoutKey = stringPreferencesKey("atx_mini_layout_v2")
val MiniPlayerStyleKey = stringPreferencesKey("atx_mini_player_style")
val MiniProgressKey = stringPreferencesKey("atx_mini_progress")
val MiniArtworkKey = stringPreferencesKey("atx_mini_artwork")
val MiniButtonsKey = stringPreferencesKey("atx_mini_buttons")
val MiniShowArtistKey = booleanPreferencesKey("atx_mini_show_artist")

// endregion

/** Tutte le scelte di stile dell'app, lette insieme. */
@Immutable
data class AtxStyle(
    val themeSource: ThemeSource = ThemeSource.Custom,
    val customColor: Color = Color(0xFFFF8FB8),
    val paletteStyle: PaletteStyleChoice = PaletteStyleChoice.TonalSpot,
    val pureBlack: Boolean = false,
    val corners: ArtworkCorners = ArtworkCorners.Large,
    val tabLabels: TabLabels = TabLabels.Selected,
    val coloredHome: Boolean = true,
    val playerLayout: PlayerLayout = PlayerLayout.Card,
    val playerBackground: PlayerBackground = PlayerBackground.Glow,
    val slider: SliderStyleChoice = SliderStyleChoice.Wavy,
    val playButton: PlayButtonShape = PlayButtonShape.Group,
    val shrinkArtworkOnPause: Boolean = true,
    val showRemainingTime: Boolean = true,
    val coloredPlayerControls: Boolean = true,
    val miniLayout: MiniLayout = MiniLayout.Joined,
    val miniPlayer: MiniPlayerStyle = MiniPlayerStyle.Tinted,
    val miniProgress: MiniProgress = MiniProgress.Wave,
    val miniArtwork: MiniArtwork = MiniArtwork.Rounded,
    val miniButtons: MiniButtons = MiniButtons.PlayNext,
    val miniShowArtist: Boolean = true,
)

private fun Preferences.toAtxStyle(): AtxStyle {
    val d = AtxStyle()
    return AtxStyle(
        themeSource = this[ThemeSourceKey].toEnum(d.themeSource),
        customColor = this[CustomColorKey]?.let { Color(it) } ?: d.customColor,
        paletteStyle = this[PaletteStyleKey].toEnum(d.paletteStyle),
        pureBlack = this[PureBlackKey] ?: d.pureBlack,
        corners = this[ArtworkCornersKey].toEnum(d.corners),
        tabLabels = this[TabLabelsKey].toEnum(d.tabLabels),
        coloredHome = this[ColoredHomeKey] ?: d.coloredHome,
        playerLayout = this[PlayerLayoutKey].toEnum(d.playerLayout),
        playerBackground = this[PlayerBackgroundKey].toEnum(d.playerBackground),
        slider = this[SliderStyleKey].toEnum(d.slider),
        playButton = this[PlayButtonShapeKey].toEnum(d.playButton),
        shrinkArtworkOnPause = this[ShrinkArtworkOnPauseKey] ?: d.shrinkArtworkOnPause,
        showRemainingTime = this[ShowRemainingTimeKey] ?: d.showRemainingTime,
        coloredPlayerControls = this[ColoredPlayerControlsKey] ?: d.coloredPlayerControls,
        miniLayout = this[MiniLayoutKey].toEnum(d.miniLayout),
        miniPlayer = this[MiniPlayerStyleKey].toEnum(d.miniPlayer),
        miniProgress = this[MiniProgressKey].toEnum(d.miniProgress),
        miniArtwork = this[MiniArtworkKey].toEnum(d.miniArtwork),
        miniButtons = this[MiniButtonsKey].toEnum(d.miniButtons),
        miniShowArtist = this[MiniShowArtistKey] ?: d.miniShowArtist,
    )
}

/** Colore come Int per le preferenze. */
fun Color.toPreferenceInt(): Int = toArgb()

@Composable
fun rememberAtxStyle(): AtxStyle {
    val flow = remember { AppContext.dataStore.data.map { it.toAtxStyle() }.distinctUntilChanged() }
    val style by flow.collectAsState(AtxStyle())
    return style
}

val LocalAtxStyle = staticCompositionLocalOf { AtxStyle() }

fun PaletteStyleChoice.toMaterialKolor(): com.materialkolor.PaletteStyle =
    when (this) {
        PaletteStyleChoice.TonalSpot -> com.materialkolor.PaletteStyle.TonalSpot
        PaletteStyleChoice.Vibrant -> com.materialkolor.PaletteStyle.Vibrant
        PaletteStyleChoice.Expressive -> com.materialkolor.PaletteStyle.Expressive
        PaletteStyleChoice.Fidelity -> com.materialkolor.PaletteStyle.Fidelity
        PaletteStyleChoice.Content -> com.materialkolor.PaletteStyle.Content
        PaletteStyleChoice.Rainbow -> com.materialkolor.PaletteStyle.Rainbow
        PaletteStyleChoice.FruitSalad -> com.materialkolor.PaletteStyle.FruitSalad
        PaletteStyleChoice.Neutral -> com.materialkolor.PaletteStyle.Neutral
    }
