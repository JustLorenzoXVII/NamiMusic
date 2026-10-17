package com.justlorenzo.archivetunex.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import archivetunex.shared.generated.resources.Res
import archivetunex.shared.generated.resources.arrow_back
import archivetunex.shared.generated.resources.check
import archivetunex.shared.generated.resources.close
import archivetunex.shared.generated.resources.delete
import archivetunex.shared.generated.resources.delete_history
import archivetunex.shared.generated.resources.download
import archivetunex.shared.generated.resources.info
import archivetunex.shared.generated.resources.language
import archivetunex.shared.generated.resources.location_on
import archivetunex.shared.generated.resources.manage_search
import archivetunex.shared.generated.resources.navigate_next
import archivetunex.shared.generated.resources.palette
import archivetunex.shared.generated.resources.play
import archivetunex.shared.generated.resources.security
import archivetunex.shared.generated.resources.skip_next
import archivetunex.shared.generated.resources.sliders
import archivetunex.shared.generated.resources.storage
import archivetunex.shared.generated.resources.tune
import com.justlorenzo.archivetunex.ui.AccentChoice
import com.justlorenzo.archivetunex.ui.AccentKey
import com.justlorenzo.archivetunex.ui.ArtworkCorners
import com.justlorenzo.archivetunex.ui.ArtworkCornersKey
import com.justlorenzo.archivetunex.ui.ColoredHomeKey
import com.justlorenzo.archivetunex.ui.ColoredPlayerControlsKey
import com.justlorenzo.archivetunex.ui.LocalActions
import com.justlorenzo.archivetunex.ui.LocalAtxStyle
import com.justlorenzo.archivetunex.ui.LocalNavigator
import com.justlorenzo.archivetunex.ui.MiniPlayerNextKey
import com.justlorenzo.archivetunex.ui.MiniPlayerProgressKey
import com.justlorenzo.archivetunex.ui.MiniPlayerStyle
import com.justlorenzo.archivetunex.ui.MiniPlayerStyleKey
import com.justlorenzo.archivetunex.ui.PlayButtonShape
import com.justlorenzo.archivetunex.ui.PlayButtonShapeKey
import com.justlorenzo.archivetunex.ui.PlayerBackground
import com.justlorenzo.archivetunex.ui.PlayerBackgroundKey
import com.justlorenzo.archivetunex.ui.SettingsPage
import com.justlorenzo.archivetunex.ui.ShowRemainingTimeKey
import com.justlorenzo.archivetunex.ui.ShrinkArtworkOnPauseKey
import com.justlorenzo.archivetunex.ui.SliderStyleChoice
import com.justlorenzo.archivetunex.ui.SliderStyleKey
import com.justlorenzo.archivetunex.ui.TabLabels
import com.justlorenzo.archivetunex.ui.TabLabelsKey
import com.justlorenzo.archivetunex.ui.components.AtxIcon
import com.justlorenzo.archivetunex.ui.components.ConfirmDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import moe.rukamori.archivetune.BuildConfig
import moe.rukamori.archivetune.compat.AppContext
import moe.rukamori.archivetune.compat.File
import moe.rukamori.archivetune.constants.AudioQuality
import moe.rukamori.archivetune.constants.AudioQualityKey
import moe.rukamori.archivetune.constants.ContentCountryKey
import moe.rukamori.archivetune.constants.ContentLanguageKey
import moe.rukamori.archivetune.constants.CountryCodeToName
import moe.rukamori.archivetune.constants.DarkModeKey
import moe.rukamori.archivetune.constants.LanguageCodeToName
import moe.rukamori.archivetune.constants.SYSTEM_DEFAULT
import moe.rukamori.archivetune.ui.screens.settings.DarkMode
import moe.rukamori.archivetune.utils.PreferenceStore
import moe.rukamori.archivetune.utils.dataStore
import moe.rukamori.archivetune.utils.rememberEnumPreference
import moe.rukamori.archivetune.utils.rememberPreference
import org.jetbrains.compose.resources.DrawableResource

private val AudioQuality.label: String
    get() =
        when (this) {
            AudioQuality.HIGHEST -> "Massima"
            AudioQuality.HIGH -> "Alta"
            AudioQuality.AUTO -> "Automatica"
            AudioQuality.LOW -> "Bassa (risparmia dati)"
        }

private fun <T> save(
    key: Preferences.Key<T>,
    value: T,
) = PreferenceStore.launchEdit(AppContext.dataStore) { this[key] = value }

private fun <E : Enum<E>> saveEnum(
    key: Preferences.Key<String>,
    value: E,
) = save(key, value.name)

/** Impostazioni: una pagina con le categorie e una pagina per ognuna. */
@Composable
fun SettingsOverlay() {
    val navigator = LocalNavigator.current
    val page = navigator.settingsPage

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        AnimatedContent(
            targetState = page,
            transitionSpec = {
                if (targetState != null) {
                    (slideInHorizontally { it / 3 } + fadeIn()) togetherWith fadeOut()
                } else {
                    fadeIn() togetherWith (slideOutHorizontally { it / 3 } + fadeOut())
                }
            },
            label = "impostazioni",
        ) { current ->
            when (current) {
                null -> SettingsHome()
                SettingsPage.Appearance -> AppearancePage()
                SettingsPage.Player -> PlayerPage()
                SettingsPage.MiniPlayer -> MiniPlayerPage()
                SettingsPage.Playback -> PlaybackPage()
                SettingsPage.Content -> ContentPage()
                SettingsPage.Storage -> StoragePage()
                SettingsPage.Privacy -> PrivacyPage()
                SettingsPage.About -> AboutPage()
            }
        }
    }
}

// region Struttura

/** Pagina con titolo grande e pulsante indietro/chiudi. */
@Composable
private fun SettingsScaffold(
    title: String,
    overline: String?,
    isHome: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    val navigator = LocalNavigator.current
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        IconButton(
            onClick = { if (isHome) navigator.settingsVisible = false else navigator.settingsPage = null },
            modifier = Modifier.padding(4.dp),
        ) { AtxIcon(if (isHome) Res.drawable.close else Res.drawable.arrow_back, if (isHome) "Chiudi" else "Indietro") }
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).navigationBarsPadding().padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(Modifier.padding(horizontal = 24.dp)) {
                if (overline != null) {
                    Text(
                        overline.uppercase(),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Text(title, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black, letterSpacing = (-1.5).sp)
            }
            content()
        }
    }
}

/** Gruppo di opzioni dentro una card arrotondata. */
@Composable
private fun SettingsGroup(
    title: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(Modifier.padding(horizontal = 16.dp)) {
        if (title != null) {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 12.dp, bottom = 8.dp, top = 8.dp),
            )
        }
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(MaterialTheme.colorScheme.surfaceContainer),
            content = content,
        )
    }
}

/** Icona in un "biscotto" colorato. */
@Composable
private fun CookieIcon(
    icon: DrawableResource,
    color: Color,
    size: androidx.compose.ui.unit.Dp = 44.dp,
) {
    Box(
        Modifier.size(size).clip(MaterialShapes.Cookie9Sided.toShape()).background(color),
        contentAlignment = Alignment.Center,
    ) { AtxIcon(icon, null, size = size * 0.5f, tint = Color.White) }
}

@Composable
private fun NavRow(
    icon: DrawableResource,
    color: Color,
    title: String,
    subtitle: String?,
    onClick: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        CookieIcon(icon, color)
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
        AtxIcon(Res.drawable.navigate_next, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SwitchRow(
    title: String,
    subtitle: String?,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().clickable { onChange(!checked) }.padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun ValueRow(
    icon: DrawableResource?,
    title: String,
    value: String?,
    onClick: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (icon != null) AtxIcon(icon, null, tint = MaterialTheme.colorScheme.primary)
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            if (value != null) Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Scelte con anteprima: ogni opzione è una card con un disegno che la mostra. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> PreviewChoices(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    preview: @Composable (T) -> Unit,
) {
    FlowRow(
        Modifier.fillMaxWidth().padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        maxItemsInEachRow = 2,
    ) {
        options.forEach { option ->
            val isSelected = option == selected
            Column(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(22.dp))
                    .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh)
                    .border(
                        width = if (isSelected) 2.dp else 0.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                        shape = RoundedCornerShape(22.dp),
                    ).clickable { onSelect(option) }
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(Modifier.fillMaxWidth().height(84.dp), contentAlignment = Alignment.Center) { preview(option) }
                Text(
                    label(option),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

// endregion

// region Pagine

@Composable
private fun SettingsHome() {
    val navigator = LocalNavigator.current
    SettingsScaffold("Impostazioni", overline = "ArchiveTuneX", isHome = true) {
        SettingsGroup("Personalizza") {
            NavRow(Res.drawable.palette, Color(0xFFFA4058), "Aspetto", "Tema, colore d'accento, copertine, schede") { navigator.settingsPage = SettingsPage.Appearance }
            NavRow(Res.drawable.play, Color(0xFF8B5CF6), "Player", "Sfondo, barra, pulsante Play, dettagli") { navigator.settingsPage = SettingsPage.Player }
            NavRow(Res.drawable.sliders, Color(0xFF00A88F), "Mini player", "Stile, avanzamento, pulsanti") { navigator.settingsPage = SettingsPage.MiniPlayer }
        }
        SettingsGroup("Musica") {
            NavRow(Res.drawable.tune, Color(0xFFFF7A2F), "Riproduzione", "Qualità audio") { navigator.settingsPage = SettingsPage.Playback }
            NavRow(Res.drawable.language, Color(0xFF3D7BFF), "Contenuti", "Lingua e paese di YouTube Music") { navigator.settingsPage = SettingsPage.Content }
            NavRow(Res.drawable.storage, Color(0xFF1DB954), "Download e spazio", "Spazio occupato, elimina i download") { navigator.settingsPage = SettingsPage.Storage }
        }
        SettingsGroup("Altro") {
            NavRow(Res.drawable.security, Color(0xFF6B7280), "Privacy", "Cronologia delle ricerche") { navigator.settingsPage = SettingsPage.Privacy }
            NavRow(Res.drawable.info, Color(0xFF6B7280), "Informazioni", "Versione e registro errori") { navigator.settingsPage = SettingsPage.About }
        }
    }
}

@Composable
private fun AppearancePage() {
    val style = LocalAtxStyle.current
    var darkMode by rememberEnumPreference(DarkModeKey, DarkMode.AUTO)

    SettingsScaffold("Aspetto", overline = "Personalizza") {
        SettingsGroup("Tema") {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(16.dp)) {
                val options = listOf(DarkMode.AUTO to "Sistema", DarkMode.OFF to "Chiaro", DarkMode.ON to "Scuro")
                options.forEachIndexed { index, (mode, label) ->
                    SegmentedButton(
                        selected = darkMode == mode,
                        onClick = { darkMode = mode },
                        shape = SegmentedButtonDefaults.itemShape(index, options.size),
                    ) { Text(label) }
                }
            }
        }
        SettingsGroup("Colore d'accento") {
            AccentSwatches(style.accent) { saveEnum(AccentKey, it) }
            Text(
                if (style.accent == AccentChoice.Dynamic) {
                    "L'app prende il colore della copertina del brano che stai ascoltando."
                } else {
                    "Pulsanti, schede e dettagli usano questo colore."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 16.dp),
            )
        }
        SettingsGroup("Angoli delle copertine") {
            PreviewChoices(
                options = ArtworkCorners.entries,
                selected = style.corners,
                label = { it.label },
                onSelect = { saveEnum(ArtworkCornersKey, it) },
            ) { corners ->
                Box(
                    Modifier.size(64.dp).clip(RoundedCornerShape(corners.radius * 0.6f)).background(
                        Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))),
                    ),
                )
            }
        }
        SettingsGroup("Schede in basso") {
            PreviewChoices(
                options = TabLabels.entries,
                selected = style.tabLabels,
                label = { it.label },
                onSelect = { saveEnum(TabLabelsKey, it) },
            ) { labels -> TabsPreview(labels) }
        }
        SettingsGroup {
            SwitchRow(
                "Home colorata",
                "Le card della Home prendono i colori delle copertine",
                style.coloredHome,
            ) { save(ColoredHomeKey, it) }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AccentSwatches(
    selected: AccentChoice,
    onSelect: (AccentChoice) -> Unit,
) {
    FlowRow(
        Modifier.fillMaxWidth().padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AccentChoice.entries.forEach { choice ->
            val isSelected = choice == selected
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(64.dp)) {
                Box(
                    Modifier
                        .size(52.dp)
                        .clip(if (isSelected) MaterialShapes.Cookie9Sided.toShape() else CircleShape)
                        .background(
                            choice.color?.let { Brush.linearGradient(listOf(it, it)) }
                                ?: Brush.sweepGradient(listOf(Color(0xFFFA4058), Color(0xFFF5B800), Color(0xFF1DB954), Color(0xFF3D7BFF), Color(0xFF8B5CF6), Color(0xFFFA4058))),
                        ).clickable { onSelect(choice) },
                    contentAlignment = Alignment.Center,
                ) {
                    if (isSelected) AtxIcon(Res.drawable.check, null, tint = Color.White)
                }
                Text(
                    choice.label,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun TabsPreview(labels: TabLabels) {
    Row(
        Modifier.clip(RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.surfaceContainerHighest).padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        listOf(true, false, false).forEach { active ->
            val show = labels == TabLabels.Always || (labels == TabLabels.Selected && active)
            Row(
                Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (active) MaterialTheme.colorScheme.primary else Color.Transparent)
                    .padding(horizontal = 7.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(10.dp).clip(CircleShape).background(if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant))
                if (show) {
                    Box(
                        Modifier.padding(start = 4.dp).width(16.dp).height(5.dp).clip(RoundedCornerShape(3.dp))
                            .background(if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant),
                    )
                }
            }
        }
    }
}

@Composable
private fun PlayerPage() {
    val style = LocalAtxStyle.current
    SettingsScaffold("Player", overline = "Personalizza") {
        SettingsGroup("Sfondo") {
            PreviewChoices(
                options = PlayerBackground.entries,
                selected = style.playerBackground,
                label = { it.label },
                onSelect = { saveEnum(PlayerBackgroundKey, it) },
            ) { background -> PlayerBackgroundPreview(background) }
        }
        SettingsGroup("Barra di avanzamento") {
            PreviewChoices(
                options = SliderStyleChoice.entries,
                selected = style.slider,
                label = { it.label },
                onSelect = { saveEnum(SliderStyleKey, it) },
            ) { slider -> SliderPreview(slider) }
        }
        SettingsGroup("Pulsante Play") {
            PreviewChoices(
                options = PlayButtonShape.entries,
                selected = style.playButton,
                label = { it.label },
                onSelect = { saveEnum(PlayButtonShapeKey, it) },
            ) { shape ->
                Box(
                    Modifier.size(60.dp).clip(playShapeOf(shape)).background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center,
                ) { AtxIcon(Res.drawable.play, null, size = 30.dp, tint = MaterialTheme.colorScheme.onPrimary) }
            }
        }
        SettingsGroup("Dettagli") {
            SwitchRow("Comandi colorati", "Pulsanti e barra prendono il colore della copertina", style.coloredPlayerControls) {
                save(ColoredPlayerControlsKey, it)
            }
            SwitchRow("Copertina che si rimpicciolisce", "In pausa la copertina si fa più piccola", style.shrinkArtworkOnPause) {
                save(ShrinkArtworkOnPauseKey, it)
            }
            SwitchRow("Tempo rimanente", "A destra il tempo che manca invece della durata", style.showRemainingTime) {
                save(ShowRemainingTimeKey, it)
            }
        }
    }
}

@Composable
private fun playShapeOf(shape: PlayButtonShape): Shape =
    when (shape) {
        PlayButtonShape.CookieSpin, PlayButtonShape.Cookie -> MaterialShapes.Cookie9Sided.toShape()
        PlayButtonShape.Circle -> CircleShape
        PlayButtonShape.Squircle -> RoundedCornerShape(20.dp)
    }

@Composable
private fun PlayerBackgroundPreview(background: PlayerBackground) {
    val accent = MaterialTheme.colorScheme.primary
    val base = MaterialTheme.colorScheme.background
    Box(
        Modifier
            .width(54.dp)
            .height(84.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                when (background) {
                    PlayerBackground.Gradient -> Brush.verticalGradient(listOf(accent.copy(alpha = 0.7f).compositeOverSafe(base), base))
                    PlayerBackground.Blur ->
                        Brush.radialGradient(listOf(accent.copy(alpha = 0.6f).compositeOverSafe(base), Color(0xFF7B5CFF).copy(alpha = 0.5f).compositeOverSafe(base), base))
                    PlayerBackground.Solid -> Brush.verticalGradient(listOf(accent.copy(alpha = 0.6f).compositeOverSafe(base), accent.copy(alpha = 0.6f).compositeOverSafe(base)))
                    PlayerBackground.Plain -> Brush.verticalGradient(listOf(base, base))
                },
            ).border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.size(34.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)))
    }
}

private fun Color.compositeOverSafe(background: Color): Color = compositeOver(background)

@Composable
private fun SliderPreview(slider: SliderStyleChoice) {
    val accent = MaterialTheme.colorScheme.primary
    Box(Modifier.width(120.dp).height(24.dp), contentAlignment = Alignment.CenterStart) {
        when (slider) {
            SliderStyleChoice.Wavy ->
                moe.rukamori.archivetune.ui.player.WavySliderExpressive(
                    value = { 0.55f },
                    onValueChange = {},
                    activeTrackColor = accent,
                    thumbColor = accent,
                    isPlaying = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            SliderStyleChoice.Thick ->
                Box(Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)).background(MaterialTheme.colorScheme.surfaceContainerHighest)) {
                    Box(Modifier.fillMaxWidth(0.55f).height(10.dp).clip(RoundedCornerShape(5.dp)).background(accent))
                }
            SliderStyleChoice.Classic ->
                Box(Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)).background(MaterialTheme.colorScheme.surfaceContainerHighest)) {
                    Box(Modifier.fillMaxWidth(0.55f).height(4.dp).background(accent))
                }
        }
    }
}

@Composable
private fun MiniPlayerPage() {
    val style = LocalAtxStyle.current
    SettingsScaffold("Mini player", overline = "Personalizza") {
        SettingsGroup("Anteprima") {
            Box(Modifier.fillMaxWidth().padding(vertical = 16.dp)) {
                MiniPlayerPreview(style.miniPlayer, style.miniPlayerProgress, style.miniPlayerNext)
            }
        }
        SettingsGroup("Stile") {
            PreviewChoices(
                options = MiniPlayerStyle.entries,
                selected = style.miniPlayer,
                label = { it.label },
                onSelect = { saveEnum(MiniPlayerStyleKey, it) },
            ) { mini ->
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(miniColor(mini)),
                )
            }
        }
        SettingsGroup("Dettagli") {
            SwitchRow("Barra di avanzamento", "Una linea sotto il mini player", style.miniPlayerProgress) { save(MiniPlayerProgressKey, it) }
            SwitchRow("Pulsante brano successivo", null, style.miniPlayerNext) { save(MiniPlayerNextKey, it) }
        }
    }
}

@Composable
private fun miniColor(mini: MiniPlayerStyle): Color {
    val accent = MaterialTheme.colorScheme.primary
    return when (mini) {
        MiniPlayerStyle.Tinted -> accent.copy(alpha = 0.28f).compositeOverSafe(MaterialTheme.colorScheme.surfaceContainerHigh)
        MiniPlayerStyle.Accent -> accent
        MiniPlayerStyle.Neutral -> MaterialTheme.colorScheme.surfaceContainerHighest
    }
}

/** Mini player finto, per vedere subito l'effetto delle scelte. */
@Composable
private fun MiniPlayerPreview(
    mini: MiniPlayerStyle,
    progress: Boolean,
    next: Boolean,
) {
    val container = miniColor(mini)
    val content = if (mini == MiniPlayerStyle.Accent) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    Box(Modifier.padding(horizontal = 16.dp).fillMaxWidth().clip(RoundedCornerShape(26.dp)).background(container)) {
        Row(
            Modifier.fillMaxWidth().height(68.dp).padding(start = 10.dp, end = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(
                    Brush.linearGradient(listOf(Color(0xFFFF7A59), Color(0xFFC2185B))),
                ),
            )
            Column(Modifier.weight(1f)) {
                Text("Titolo del brano", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = content)
                Text("Artista", style = MaterialTheme.typography.bodyMedium, color = content.copy(alpha = 0.7f))
            }
            AtxIcon(Res.drawable.play, null, size = 30.dp, tint = content)
            if (next) AtxIcon(Res.drawable.skip_next, null, size = 28.dp, tint = content)
        }
        if (progress) {
            Box(
                Modifier.align(Alignment.BottomStart).padding(horizontal = 22.dp).fillMaxWidth(0.45f).height(3.dp).clip(RoundedCornerShape(2.dp))
                    .background(if (mini == MiniPlayerStyle.Accent) content else MaterialTheme.colorScheme.primary),
            )
        }
    }
}

@Composable
private fun PlaybackPage() {
    var audioQuality by rememberEnumPreference(AudioQualityKey, AudioQuality.HIGHEST)
    var choosing by remember { mutableStateOf(false) }
    SettingsScaffold("Riproduzione", overline = "Musica") {
        SettingsGroup {
            ValueRow(Res.drawable.tune, "Qualità audio", audioQuality.label) { choosing = true }
        }
    }
    if (choosing) {
        ChoiceDialog(
            title = "Qualità audio",
            options = listOf(AudioQuality.HIGHEST, AudioQuality.HIGH, AudioQuality.AUTO, AudioQuality.LOW).map { it to it.label },
            selected = audioQuality,
            onSelect = { audioQuality = it },
            onDismiss = { choosing = false },
        )
    }
}

@Composable
private fun ContentPage() {
    var contentLanguage by rememberPreference(ContentLanguageKey, SYSTEM_DEFAULT)
    var contentCountry by rememberPreference(ContentCountryKey, SYSTEM_DEFAULT)
    var dialog by remember { mutableStateOf<String?>(null) }
    SettingsScaffold("Contenuti", overline = "Musica") {
        SettingsGroup {
            ValueRow(Res.drawable.language, "Lingua dei contenuti", LanguageCodeToName[contentLanguage] ?: "Predefinita del sistema") { dialog = "language" }
            ValueRow(Res.drawable.location_on, "Paese dei contenuti", CountryCodeToName[contentCountry] ?: "Predefinito del sistema") { dialog = "country" }
        }
    }
    when (dialog) {
        "language" ->
            ChoiceDialog(
                title = "Lingua dei contenuti",
                options = listOf(SYSTEM_DEFAULT to "Predefinita del sistema") + LanguageCodeToName.entries.map { it.key to it.value },
                selected = contentLanguage,
                onSelect = { contentLanguage = it },
                onDismiss = { dialog = null },
            )
        "country" ->
            ChoiceDialog(
                title = "Paese dei contenuti",
                options = listOf(SYSTEM_DEFAULT to "Predefinito del sistema") + CountryCodeToName.entries.map { it.key to it.value },
                selected = contentCountry,
                onSelect = { contentCountry = it },
                onDismiss = { dialog = null },
            )
    }
}

@Composable
private fun StoragePage() {
    val actions = LocalActions.current
    val downloads by actions.downloads.downloads.collectAsState()
    var usedBytes by remember { mutableLongStateOf(0L) }
    var confirm by remember { mutableStateOf(false) }
    LaunchedEffect(downloads) {
        usedBytes =
            withContext(Dispatchers.IO) {
                File(AppContext.filesDir, "downloads").listFiles()?.sumOf { if (it.isFile) it.length() else 0L } ?: 0L
            }
    }
    SettingsScaffold("Download e spazio", overline = "Musica") {
        SettingsGroup {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(formatBytes(usedBytes), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black)
                Text("occupati da ${downloads.size} brani scaricati", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        SettingsGroup {
            ValueRow(Res.drawable.delete, "Elimina tutti i download", "Libera spazio sul telefono") { confirm = true }
        }
    }
    if (confirm) {
        ConfirmDialog(
            title = "Eliminare tutti i download?",
            text = "I brani scaricati vengono tolti dal telefono. Potrai riscaricarli quando vuoi.",
            confirmLabel = "Elimina",
            onDismiss = { confirm = false },
            onConfirm = { actions.downloads.removeAllDownloads() },
        )
    }
}

@Composable
private fun PrivacyPage() {
    val actions = LocalActions.current
    var confirm by remember { mutableStateOf(false) }
    SettingsScaffold("Privacy", overline = "Altro") {
        SettingsGroup {
            ValueRow(Res.drawable.delete_history, "Cancella cronologia ricerche", "Le ricerche recenti spariscono dalla scheda Cerca") { confirm = true }
        }
    }
    if (confirm) {
        ConfirmDialog(
            title = "Cancellare la cronologia delle ricerche?",
            text = "Le ricerche recenti non compariranno più nella scheda Cerca.",
            confirmLabel = "Cancella",
            onDismiss = { confirm = false },
            onConfirm = { actions.database.query { clearSearchHistory() } },
        )
    }
}

@Composable
private fun AboutPage() {
    val navigator = LocalNavigator.current
    SettingsScaffold("Informazioni", overline = "Altro") {
        SettingsGroup {
            ValueRow(Res.drawable.info, "ArchiveTuneX", "Motore ArchiveTune ${BuildConfig.VERSION_NAME}") {}
            ValueRow(Res.drawable.manage_search, "Registro errori", "Per capire cosa non funziona") {
                navigator.settingsVisible = false
                navigator.logsVisible = true
            }
        }
    }
}

// endregion

private fun formatBytes(bytes: Long): String {
    val mb = bytes / (1024.0 * 1024.0)
    return if (mb >= 1024) {
        "${(mb / 1024 * 10).toLong() / 10.0} GB"
    } else {
        "${(mb * 10).toLong() / 10.0} MB"
    }
}

@Composable
private fun <T> ChoiceDialog(
    title: String,
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            LazyColumn(Modifier.heightIn(max = 420.dp)) {
                items(options) { (value, label) ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSelect(value)
                                onDismiss()
                            }.padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        RadioButton(selected = value == selected, onClick = {
                            onSelect(value)
                            onDismiss()
                        })
                        Text(label, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Chiudi") } },
    )
}
