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
    private val rawLabel: String,
) {
    System("Colori di sistema"),
    Album("Colori della copertina"),
    Custom("Colore personalizzato");

    val label: String get() = tr(rawLabel)
}

/** Come la tavolozza Material viene costruita dal colore di partenza. */
enum class PaletteStyleChoice(
    private val rawLabel: String,
    private val rawDescription: String = "",
    val visible: Boolean = false,
) {
    TonalSpot("Morbida", "Tinte delicate del tuo colore: elegante e riposante", true),
    Vibrant("Vivace", "Colori pieni e saturi: tutto più acceso", true),
    Expressive("Audace", "Abbina al tuo colore tinte diverse: più personalità", true),
    Fidelity("Fedele"),
    Content("Contenuto"),
    Rainbow("Arcobaleno"),
    FruitSalad("Macedonia"),
    Neutral("Sobria", "Quasi grigia, con solo un tocco del tuo colore", true);

    val label: String get() = tr(rawLabel)
    val description: String get() = tr(rawDescription)
}

/** Colori pronti per il colore personalizzato, ognuno con il suo nome. */
val PresetColors =
    listOf(
        Color(0xFFFF8FB8) to "Sakura",
        Color(0xFFE5446D) to "Ciliegia",
        Color(0xFFFF7A6B) to "Corallo",
        Color(0xFFF5B23C) to "Ambra",
        Color(0xFFA6D84A) to "Lime",
        Color(0xFF4FD1A5) to "Menta",
        Color(0xFF2FC4B6) to "Laguna",
        Color(0xFF4AA8FF) to "Cielo",
        Color(0xFF6C6CF0) to "Indaco",
        Color(0xFFA88BFA) to "Lavanda",
    )

/** Angoli delle copertine. */
enum class ArtworkCorners(
    private val rawLabel: String,
    val radius: Dp,
) {
    Small("Squadrati", 8.dp),
    Medium("Arrotondati", 20.dp),
    Large("Molto arrotondati", 36.dp);

    val label: String get() = tr(rawLabel)
}

/** Nomi delle schede in basso. */
enum class TabLabels(
    private val rawLabel: String,
) {
    Selected("Solo quella attiva"),
    Always("Sempre"),
    Never("Mai");

    val label: String get() = tr(rawLabel)
}

// endregion

// region Player

/** Disposizione del player a schermo intero. */
enum class PlayerLayout(
    private val rawLabel: String,
) {
    Card("Copertina"),
    Vinyl("Vinile"),
    Stack("Pila"),
    Cassette("Cassetta"),
    Poster("Poster");

    val label: String get() = tr(rawLabel)
}

/** Sfondo del player a schermo intero. */
enum class PlayerBackground(
    private val rawLabel: String,
) {
    Glow("Bagliore"),
    Gradient("Sfumatura"),
    Blur("Copertina sfocata"),
    Plain("Tema");

    val label: String get() = tr(rawLabel)
}

/** Barra di avanzamento del player. */
enum class SliderStyleChoice(
    private val rawLabel: String,
) {
    Wavy("Ondulata"),
    Thick("Spessa"),
    Classic("Classica");

    val label: String get() = tr(rawLabel)
}

/** Pulsanti del player: gruppo connesso (come Fuji) o Play a forma scelta. */
enum class PlayButtonShape(
    private val rawLabel: String,
) {
    Group("Gruppo connesso"),
    CookieSpin("Biscotto che ruota"),
    Cookie("Biscotto"),
    Circle("Cerchio"),
    Squircle("Quadrato arrotondato");

    val label: String get() = tr(rawLabel)
}

// endregion

// region Mini player

/** Forma e posizione del mini player. */
enum class MiniLayout(
    private val rawLabel: String,
) {
    Joined("Unito alla barra"),
    Floating("Fluttuante"),
    Pill("Pillola"),
    Large("Grande");

    val label: String get() = tr(rawLabel)
}

/** Colore del mini player. */
enum class MiniPlayerStyle(
    private val rawLabel: String,
) {
    Tinted("Tinta copertina"),
    Gradient("Sfumatura"),
    Accent("Colore pieno"),
    Neutral("Neutro");

    val label: String get() = tr(rawLabel)
}

/** Come il mini player mostra l'avanzamento. */
enum class MiniProgress(
    private val rawLabel: String,
) {
    Line("Linea"),
    Wave("Onda"),
    Frame("Cornice"),
    Fill("Riempimento"),
    Ring("Anello"),
    None("Nessuno");

    val label: String get() = tr(rawLabel)
}

/** Forma della copertina nel mini player. */
enum class MiniArtwork(
    private val rawLabel: String,
) {
    Rounded("Arrotondata"),
    Circle("Cerchio"),
    Cookie("Biscotto"),
    Vinyl("Vinile che gira");

    val label: String get() = tr(rawLabel)
}

/** Pulsanti del mini player. */
enum class MiniButtons(
    private val rawLabel: String,
) {
    PlayOnly("Solo Play"),
    PlayNext("Play e successivo"),
    All("Tutti e tre");

    val label: String get() = tr(rawLabel)
}

// endregion

// region Chiavi (preferenze di ArchiveTuneX, separate da quelle di ArchiveTune)

val AppLanguageKey = stringPreferencesKey("atx_app_language")
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

internal fun Preferences.toAtxStyle(): AtxStyle {
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
fun rememberAtxStyle(initial: AtxStyle = AtxStyle()): AtxStyle {
    val flow = remember { AppContext.dataStore.data.map { it.toAtxStyle() }.distinctUntilChanged() }
    val style by flow.collectAsState(initial)
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
