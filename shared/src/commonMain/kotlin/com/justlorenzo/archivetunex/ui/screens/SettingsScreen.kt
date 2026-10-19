@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

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
import androidx.compose.foundation.layout.PaddingValues
import com.justlorenzo.archivetunex.ui.components.readableOn
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.layout.offset
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
import archivetunex.shared.generated.resources.skip_previous
import archivetunex.shared.generated.resources.sliders
import archivetunex.shared.generated.resources.storage
import archivetunex.shared.generated.resources.tune
import com.justlorenzo.archivetunex.ui.ArtworkCorners
import com.justlorenzo.archivetunex.ui.ArtworkCornersKey
import com.justlorenzo.archivetunex.ui.ColoredHomeKey
import com.justlorenzo.archivetunex.ui.ColoredPlayerControlsKey
import com.justlorenzo.archivetunex.ui.CustomColorKey
import com.justlorenzo.archivetunex.ui.LocalActions
import com.justlorenzo.archivetunex.ui.LocalAtxStyle
import com.justlorenzo.archivetunex.ui.LocalNavigator
import com.justlorenzo.archivetunex.ui.MiniArtwork
import com.justlorenzo.archivetunex.ui.MiniArtworkKey
import com.justlorenzo.archivetunex.ui.MiniButtons
import com.justlorenzo.archivetunex.ui.MiniButtonsKey
import com.justlorenzo.archivetunex.ui.MiniLayout
import com.justlorenzo.archivetunex.ui.MiniLayoutKey
import com.justlorenzo.archivetunex.ui.MiniPlayerStyle
import com.justlorenzo.archivetunex.ui.MiniPlayerStyleKey
import com.justlorenzo.archivetunex.ui.MiniProgress
import com.justlorenzo.archivetunex.ui.MiniProgressKey
import com.justlorenzo.archivetunex.ui.MiniShowArtistKey
import com.justlorenzo.archivetunex.ui.PaletteStyleChoice
import com.justlorenzo.archivetunex.ui.PaletteStyleKey
import com.justlorenzo.archivetunex.ui.PlayButtonShape
import com.justlorenzo.archivetunex.ui.PlayButtonShapeKey
import com.justlorenzo.archivetunex.ui.PlayerBackground
import com.justlorenzo.archivetunex.ui.PlayerBackgroundKey
import com.justlorenzo.archivetunex.ui.PlayerLayout
import com.justlorenzo.archivetunex.ui.PlayerLayoutKey
import com.justlorenzo.archivetunex.ui.PresetColors
import com.justlorenzo.archivetunex.ui.PureBlackKey
import com.justlorenzo.archivetunex.ui.SettingsPage
import com.justlorenzo.archivetunex.ui.ShowRemainingTimeKey
import com.justlorenzo.archivetunex.ui.ShrinkArtworkOnPauseKey
import com.justlorenzo.archivetunex.ui.SliderStyleChoice
import com.justlorenzo.archivetunex.ui.SliderStyleKey
import com.justlorenzo.archivetunex.ui.TabLabels
import com.justlorenzo.archivetunex.ui.TabLabelsKey
import com.justlorenzo.archivetunex.ui.ThemeSource
import com.justlorenzo.archivetunex.ui.ThemeSourceKey
import com.justlorenzo.archivetunex.ui.toMaterialKolor
import com.justlorenzo.archivetunex.ui.toPreferenceInt
import com.materialkolor.dynamicColorScheme
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
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
            val primary = MaterialTheme.colorScheme.primary
            Box(Modifier.weight(1f)) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(
                            if (isSelected) primary.copy(alpha = 0.14f).compositeOver(MaterialTheme.colorScheme.surfaceContainerHigh) else MaterialTheme.colorScheme.surfaceContainerHigh,
                        ).border(
                            width = 2.dp,
                            color = if (isSelected) primary else Color.Transparent,
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
                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium,
                        color = if (isSelected) primary else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                // Spunta sulla scelta attiva
                if (isSelected) {
                    Box(
                        Modifier.align(Alignment.TopEnd).padding(8.dp).size(24.dp).clip(CircleShape).background(primary),
                        contentAlignment = Alignment.Center,
                    ) { AtxIcon(Res.drawable.check, null, size = 16.dp, tint = MaterialTheme.colorScheme.onPrimary) }
                }
            }
        }
    }
}

// endregion

// region Pagine

@Composable
private fun SettingsHome() {
    val navigator = LocalNavigator.current
    SettingsScaffold("Impostazioni", overline = "Nami", isHome = true) {
        SettingsGroup("Personalizza") {
            NavRow(Res.drawable.palette, Color(0xFFFF8FB8), "Aspetto", "Tema, colore d'accento, copertine, schede") { navigator.settingsPage = SettingsPage.Appearance }
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
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f

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
            SwitchRow("Nero puro", "Nel tema scuro lo sfondo è nero (schermi OLED)", style.pureBlack) { save(PureBlackKey, it) }
        }

        SettingsGroup("Colori dell'app") {
            PreviewChoices(
                options = ThemeSource.entries,
                selected = style.themeSource,
                label = { it.label },
                onSelect = { saveEnum(ThemeSourceKey, it) },
            ) { source -> ThemeSourcePreview(source, style.customColor, dark) }
            Text(
                when (style.themeSource) {
                    ThemeSource.System -> "Su Android 12 o successivi usa i colori dello sfondo del telefono. Su iPhone, che non li ha, usa il colore personalizzato."
                    ThemeSource.Album -> "Sfondi, pulsanti e schede cambiano con la copertina del brano che stai ascoltando."
                    ThemeSource.Custom -> "Tutta l'app, sfondi compresi, nasce dal colore che scegli qui sotto."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 16.dp),
            )
        }

        if (style.themeSource == ThemeSource.Custom) {
            SettingsGroup("Il tuo colore") {
                ColorPicker(style.customColor) { save(CustomColorKey, it.toPreferenceInt()) }
            }
        }

        // Con i colori di sistema decide il telefono: la tavolozza non serve
        if (style.themeSource != ThemeSource.System) {
            SettingsGroup("Stile dei colori") {
                PreviewChoices(
                    options = PaletteStyleChoice.entries.filter { it.visible },
                    selected = style.paletteStyle,
                    label = { it.label },
                    onSelect = { saveEnum(PaletteStyleKey, it) },
                ) { palette -> PalettePreview(style.customColor, palette, dark) }
                Text(
                    style.paletteStyle.description.ifBlank { "Scegli quanto devono essere accesi i colori dell'app." },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 16.dp),
                )
            }
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
                        Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary)),
                    ),
                )
            }
        }
        // Vale solo per le schede fluttuanti: con la barra unita (stile Fuji) i nomi ci sono sempre
        if (style.miniLayout != com.justlorenzo.archivetunex.ui.MiniLayout.Joined) {
            SettingsGroup("Schede in basso") {
                PreviewChoices(
                    options = TabLabels.entries,
                    selected = style.tabLabels,
                    label = { it.label },
                    onSelect = { saveEnum(TabLabelsKey, it) },
                ) { labels -> TabsPreview(labels) }
            }
        }
        SettingsGroup {
            SwitchRow("Home colorata", "Le card della Home prendono i colori delle copertine", style.coloredHome) { save(ColoredHomeKey, it) }
        }
    }
}

/** Anteprima di una fonte di colori: tre "schermi" in miniatura. */
@Composable
private fun ThemeSourcePreview(
    source: ThemeSource,
    custom: Color,
    dark: Boolean,
) {
    val seeds =
        when (source) {
            ThemeSource.System -> listOf(Color(0xFF7FA6C9), Color(0xFF9BC59D))
            ThemeSource.Album -> listOf(Color(0xFFFF7A59), Color(0xFF6C5CE7))
            ThemeSource.Custom -> listOf(custom, custom)
        }
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        seeds.forEach { seed ->
            val scheme = remember(seed, dark) { dynamicColorScheme(seedColor = seed, isDark = dark, contrastLevel = 0.0) }
            MiniScreen(scheme.background, scheme.primary, scheme.surfaceContainerHigh)
        }
    }
}

/** Schermo in miniatura: sfondo, una card e un pulsante. */
@Composable
private fun MiniScreen(
    background: Color,
    primary: Color,
    container: Color,
) {
    Column(
        Modifier.width(40.dp).height(72.dp).clip(RoundedCornerShape(10.dp)).background(background)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp)).padding(5.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(Modifier.fillMaxWidth().height(22.dp).clip(RoundedCornerShape(5.dp)).background(container))
        Box(Modifier.fillMaxWidth(0.7f).height(5.dp).clip(RoundedCornerShape(3.dp)).background(container))
        Spacer(Modifier.weight(1f))
        Box(Modifier.size(14.dp).clip(CircleShape).background(primary))
    }
}

/** Una piccola schermata dell'app con i colori che la tavolozza genera: sfondo, card, pulsante ed etichetta. */
@Composable
private fun PalettePreview(
    seed: Color,
    palette: PaletteStyleChoice,
    dark: Boolean,
) {
    val scheme = remember(seed, palette, dark) { dynamicColorScheme(seedColor = seed, isDark = dark, contrastLevel = 0.0, style = palette.toMaterialKolor()) }
    Column(
        Modifier
            .width(120.dp)
            .height(80.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(scheme.background)
            .border(1.dp, scheme.outlineVariant, RoundedCornerShape(16.dp))
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        // Card con copertina e due righe di testo
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(scheme.surfaceContainerHigh).padding(5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Box(Modifier.size(20.dp).clip(RoundedCornerShape(5.dp)).background(Brush.linearGradient(listOf(scheme.primary, scheme.tertiary))))
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Box(Modifier.width(44.dp).height(5.dp).clip(CircleShape).background(scheme.onSurface))
                Box(Modifier.width(30.dp).height(4.dp).clip(CircleShape).background(scheme.onSurfaceVariant))
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            // Pulsante principale
            Box(Modifier.width(42.dp).height(18.dp).clip(CircleShape).background(scheme.primary), contentAlignment = Alignment.Center) {
                Box(Modifier.width(22.dp).height(4.dp).clip(CircleShape).background(scheme.onPrimary))
            }
            // Etichetta
            Box(Modifier.width(30.dp).height(18.dp).clip(CircleShape).background(scheme.secondaryContainer))
            Box(Modifier.size(18.dp).clip(MaterialShapes.Cookie9Sided.toShape()).background(scheme.tertiary))
        }
    }
}

/** Selettore del colore: colori pronti con il nome, oppure "Su misura" con tonalità e tono. */
@Composable
private fun ColorPicker(
    color: Color,
    onChange: (Color) -> Unit,
) {
    val presetName = PresetColors.firstOrNull { it.first.toArgb() == color.toArgb() }?.second
    var customOpen by remember { mutableStateOf(presetName == null) }
    val hsv = remember(color) { color.toHsv() }
    var hue by remember(color) { mutableStateOf(hsv[0]) }
    // Tono: 0 = pastello (poco saturo, chiaro), 1 = intenso (saturo, un po' più scuro)
    var tone by remember(color) { mutableStateOf(((hsv[1] - 0.3f) / 0.65f).coerceIn(0f, 1f)) }
    fun toneColor(h: Float, t: Float) = Color.hsv(h, 0.3f + 0.65f * t, 1f - 0.25f * t)
    val preview = if (customOpen) toneColor(hue, tone) else color

    Column(Modifier.padding(vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Colore scelto in grande, con il nome
        Row(
            Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(Modifier.size(52.dp).clip(MaterialShapes.Cookie9Sided.toShape()).background(preview))
            Column {
                Text(if (customOpen) "Su misura" else presetName ?: "Su misura", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                Text("Tutta l'app nasce da questo colore", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        // Colori pronti in una fila che scorre
        androidx.compose.foundation.lazy.LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(PresetColors.size) { i ->
                val (preset, _) = PresetColors[i]
                val selected = !customOpen && preset.toArgb() == color.toArgb()
                Box(
                    Modifier
                        .size(48.dp)
                        .clip(if (selected) MaterialShapes.Cookie9Sided.toShape() else CircleShape)
                        .background(preset)
                        .clickable {
                            customOpen = false
                            onChange(preset)
                        },
                    contentAlignment = Alignment.Center,
                ) { if (selected) AtxIcon(Res.drawable.check, null, size = 22.dp, tint = preset.readableOn()) }
            }
            item {
                // "Su misura": cerchio con tutte le tonalità
                Box(
                    Modifier
                        .size(48.dp)
                        .clip(if (customOpen) MaterialShapes.Cookie9Sided.toShape() else CircleShape)
                        .background(Brush.sweepGradient((0..6).map { Color.hsv(it * 60f % 360f, 0.7f, 0.95f) }))
                        .clickable { customOpen = true },
                    contentAlignment = Alignment.Center,
                ) {
                    Box(Modifier.size(22.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerHigh), contentAlignment = Alignment.Center) {
                        Text("+", fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }
        // Su misura: solo due cursori, il colore si applica quando lasci il dito
        androidx.compose.animation.AnimatedVisibility(visible = customOpen) {
            Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                GradientSlider(
                    "Tonalità",
                    hue / 360f,
                    Brush.horizontalGradient((0..6).map { toneColor(it * 60f % 360f, tone) }),
                    onChange = { hue = it * 360f },
                    onDone = { onChange(toneColor(hue, tone)) },
                )
                GradientSlider(
                    "Tono: da pastello a intenso",
                    tone,
                    Brush.horizontalGradient(listOf(toneColor(hue, 0f), toneColor(hue, 1f))),
                    onChange = { tone = it },
                    onDone = { onChange(toneColor(hue, tone)) },
                )
            }
        }
    }
}

/** Cursore con la sfumatura dei colori dietro. */
@Composable
private fun GradientSlider(
    label: String,
    value: Float,
    track: Brush,
    onChange: (Float) -> Unit,
    onDone: () -> Unit = {},
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Box(contentAlignment = Alignment.Center) {
            Box(Modifier.fillMaxWidth().height(14.dp).clip(RoundedCornerShape(7.dp)).background(track))
            androidx.compose.material3.Slider(
                value = value,
                onValueChange = onChange,
                onValueChangeFinished = onDone,
                colors =
                    androidx.compose.material3.SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = Color.Transparent,
                        inactiveTrackColor = Color.Transparent,
                        activeTickColor = Color.Transparent,
                        inactiveTickColor = Color.Transparent,
                    ),
            )
        }
    }
}

/** Colore in tonalità (0-360), saturazione e luminosità (0-1). */
private fun Color.toHsv(): FloatArray {
    val max = maxOf(red, green, blue)
    val min = minOf(red, green, blue)
    val delta = max - min
    val hue =
        when {
            delta == 0f -> 0f
            max == red -> 60f * (((green - blue) / delta) % 6f)
            max == green -> 60f * (((blue - red) / delta) + 2f)
            else -> 60f * (((red - green) / delta) + 4f)
        }.let { if (it < 0) it + 360f else it }
    val saturation = if (max == 0f) 0f else delta / max
    return floatArrayOf(hue, saturation, max)
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
        SettingsGroup("Disposizione") {
            PreviewChoices(
                options = PlayerLayout.entries,
                selected = style.playerLayout,
                label = { it.label },
                onSelect = { saveEnum(PlayerLayoutKey, it) },
            ) { layout -> PlayerLayoutPreview(layout) }
        }
        // Il Poster ha già il suo sfondo (il colore della copertina): lì la scelta non serve
        if (style.playerLayout != PlayerLayout.Poster) {
            SettingsGroup("Sfondo · ${style.playerLayout.label}") {
                PreviewChoices(
                    options = PlayerBackground.entries,
                    selected = style.playerBackground,
                    label = { it.label },
                    onSelect = { saveEnum(PlayerBackgroundKey, it) },
                ) { background -> PlayerBackgroundPreview(background, style.playerLayout) }
            }
        }
        SettingsGroup("Barra di avanzamento") {
            PreviewChoices(
                options = SliderStyleChoice.entries,
                selected = style.slider,
                label = { it.label },
                onSelect = { saveEnum(SliderStyleKey, it) },
            ) { slider -> SliderPreview(slider) }
        }
        SettingsGroup("Comandi") {
            PreviewChoices(
                options = PlayButtonShape.entries,
                selected = style.playButton,
                label = { it.label },
                onSelect = { saveEnum(PlayButtonShapeKey, it) },
            ) { shape ->
                if (shape == PlayButtonShape.Group) {
                    val c = MaterialTheme.colorScheme.primary
                    Row(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.width(24.dp).height(40.dp).clip(RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp, topEnd = 6.dp, bottomEnd = 6.dp)).background(c.copy(alpha = 0.2f)))
                        Box(Modifier.width(40.dp).height(48.dp).clip(RoundedCornerShape(16.dp)).background(c), contentAlignment = Alignment.Center) {
                            AtxIcon(Res.drawable.play, null, size = 26.dp, tint = MaterialTheme.colorScheme.onPrimary)
                        }
                        Box(Modifier.width(24.dp).height(40.dp).clip(RoundedCornerShape(topStart = 6.dp, bottomStart = 6.dp, topEnd = 20.dp, bottomEnd = 20.dp)).background(c.copy(alpha = 0.2f)))
                    }
                    return@PreviewChoices
                }
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
            SwitchRow("Copertina che si rimpicciolisce", "In pausa la copertina si fa più piccola (stili Copertina e Pila)", style.shrinkArtworkOnPause) {
                save(ShrinkArtworkOnPauseKey, it)
            }
            SwitchRow("Tempo rimanente", "A destra il tempo che manca invece della durata", style.showRemainingTime) {
                save(ShowRemainingTimeKey, it)
            }
        }
    }
}

/** Disegno in miniatura di ogni disposizione del player. */
@Composable
private fun PlayerLayoutPreview(
    layout: PlayerLayout,
    framed: Boolean = true,
) {
    val primary = MaterialTheme.colorScheme.primary
    val muted = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
    val art = Brush.linearGradient(listOf(Color(0xFFFF7A59), Color(0xFFC2185B)))
    Column(
        if (framed) {
            Modifier.width(52.dp).height(84.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.background)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
        } else {
            Modifier.width(52.dp).height(84.dp)
        },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        when (layout) {
            PlayerLayout.Card -> {
                Spacer(Modifier.height(10.dp))
                Box(Modifier.size(32.dp).shadow(4.dp, RoundedCornerShape(7.dp)).clip(RoundedCornerShape(7.dp)).background(art))
            }
            PlayerLayout.Vinyl -> {
                Spacer(Modifier.height(12.dp))
                Box(Modifier.width(44.dp).height(26.dp)) {
                    Box(Modifier.offset(x = 16.dp).size(26.dp).clip(CircleShape).background(Color(0xFF111114)), contentAlignment = Alignment.Center) {
                        Box(Modifier.size(9.dp).clip(CircleShape).background(art))
                    }
                    Box(Modifier.size(26.dp).clip(RoundedCornerShape(5.dp)).background(art))
                }
            }
            PlayerLayout.Stack -> {
                Spacer(Modifier.height(12.dp))
                Box(Modifier.size(36.dp)) {
                    Box(Modifier.offset(x = 9.dp, y = (-3).dp).size(26.dp).rotate(9f).clip(RoundedCornerShape(6.dp)).background(Color(0xFF7B5CFF).copy(alpha = 0.6f)))
                    Box(Modifier.offset(x = 5.dp, y = (-1).dp).size(26.dp).rotate(4.5f).clip(RoundedCornerShape(6.dp)).background(Color(0xFF3FA7FF).copy(alpha = 0.8f)))
                    Box(Modifier.offset(y = 4.dp).size(26.dp).clip(RoundedCornerShape(6.dp)).background(art))
                }
            }
            PlayerLayout.Cassette -> {
                Spacer(Modifier.height(14.dp))
                Column(
                    Modifier.width(42.dp).height(27.dp).clip(RoundedCornerShape(5.dp)).background(Color(0xFFE6C9A8)).padding(3.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(2.dp)).background(art))
                    Spacer(Modifier.height(2.dp))
                    Box(Modifier.width(22.dp).height(6.dp).clip(CircleShape).background(Color(0xFF3B2A1A)))
                }
            }
            PlayerLayout.Poster -> {
                Box(Modifier.fillMaxWidth().height(30.dp).background(art))
                Box(Modifier.padding(start = 5.dp, top = 3.dp).align(Alignment.Start).width(38.dp).height(8.dp).clip(RoundedCornerShape(2.dp)).background(MaterialTheme.colorScheme.onSurface))
                Box(Modifier.padding(start = 5.dp, top = 2.dp).align(Alignment.Start).width(26.dp).height(8.dp).clip(RoundedCornerShape(2.dp)).background(primary))
            }
        }
        Spacer(Modifier.weight(1f))
        Column(
            Modifier
                .padding(4.dp)
                .padding(3.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Box(Modifier.width(36.dp).height(3.dp).clip(RoundedCornerShape(2.dp)).background(muted))
            Box(Modifier.size(11.dp).clip(CircleShape).background(primary))
        }
    }
}

@Composable
private fun playShapeOf(shape: PlayButtonShape): Shape =
    when (shape) {
        PlayButtonShape.CookieSpin, PlayButtonShape.Cookie -> MaterialShapes.Cookie9Sided.toShape()
        PlayButtonShape.Circle, PlayButtonShape.Group -> CircleShape
        PlayButtonShape.Squircle -> RoundedCornerShape(20.dp)
    }

@Composable
private fun PlayerBackgroundPreview(
    background: PlayerBackground,
    layout: PlayerLayout,
) {
    val accent = MaterialTheme.colorScheme.primary
    val base = MaterialTheme.colorScheme.background
    Box(
        Modifier
            .width(54.dp)
            .height(84.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                when (background) {
                    PlayerBackground.Glow ->
                        Brush.radialGradient(
                            listOf(accent.copy(alpha = 0.55f).compositeOver(accent.copy(alpha = 0.25f).compositeOver(base)), accent.copy(alpha = 0.25f).compositeOver(base)),
                            center = androidx.compose.ui.geometry.Offset(80f, 260f),
                            radius = 220f,
                        )
                    PlayerBackground.Gradient -> Brush.verticalGradient(listOf(accent.copy(alpha = 0.7f).compositeOver(base), base))
                    PlayerBackground.Blur ->
                        Brush.radialGradient(listOf(accent.copy(alpha = 0.6f).compositeOver(base), Color(0xFF7B5CFF).copy(alpha = 0.5f).compositeOver(base), base))
                    PlayerBackground.Plain -> Brush.verticalGradient(listOf(base, base))
                },
            ).border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.TopCenter,
    ) {
        // Lo stile scelto disegnato sopra lo sfondo
        PlayerLayoutPreview(layout, framed = false)
    }
}

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
            Box(Modifier.fillMaxWidth().padding(vertical = 16.dp)) { MiniPlayerPreview(style) }
        }
        SettingsGroup("Forma") {
            PreviewChoices(
                options = MiniLayout.entries,
                selected = style.miniLayout,
                label = { it.label },
                onSelect = { saveEnum(MiniLayoutKey, it) },
            ) { layout -> MiniShapePreview(layout) }
        }
        SettingsGroup("Colore") {
            PreviewChoices(
                options = MiniPlayerStyle.entries,
                selected = style.miniPlayer,
                label = { it.label },
                onSelect = { saveEnum(MiniPlayerStyleKey, it) },
            ) { mini -> Box(Modifier.fillMaxWidth().height(36.dp).clip(RoundedCornerShape(12.dp)).background(miniBrush(mini))) }
        }
        SettingsGroup("Copertina") {
            PreviewChoices(
                options = MiniArtwork.entries,
                selected = style.miniArtwork,
                label = { it.label },
                onSelect = { saveEnum(MiniArtworkKey, it) },
            ) { artwork -> MiniArtworkPreview(artwork, 48.dp) }
        }
        SettingsGroup("Avanzamento") {
            PreviewChoices(
                options = MiniProgress.entries,
                selected = style.miniProgress,
                label = { it.label },
                onSelect = { saveEnum(MiniProgressKey, it) },
            ) { progress -> MiniProgressPreview(progress) }
        }
        SettingsGroup("Pulsanti") {
            PreviewChoices(
                options = MiniButtons.entries,
                selected = style.miniButtons,
                label = { it.label },
                onSelect = { saveEnum(MiniButtonsKey, it) },
            ) { buttons ->
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    if (buttons == MiniButtons.All) AtxIcon(Res.drawable.skip_previous, null, size = 26.dp)
                    AtxIcon(Res.drawable.play, null, size = 30.dp)
                    if (buttons != MiniButtons.PlayOnly) AtxIcon(Res.drawable.skip_next, null, size = 26.dp)
                }
            }
        }
        SettingsGroup("Dettagli") {
            SwitchRow("Mostra l'artista", "Sotto il titolo del brano", style.miniShowArtist) { save(MiniShowArtistKey, it) }
        }
    }
}

@Composable
private fun miniBrush(mini: MiniPlayerStyle): Brush {
    val accent = MaterialTheme.colorScheme.primary
    val surface = MaterialTheme.colorScheme.surfaceContainerHigh
    return when (mini) {
        MiniPlayerStyle.Tinted -> Brush.linearGradient(listOf(accent.copy(alpha = 0.3f).compositeOver(surface), accent.copy(alpha = 0.3f).compositeOver(surface)))
        MiniPlayerStyle.Gradient -> Brush.horizontalGradient(listOf(accent.copy(alpha = 0.7f).compositeOver(surface), MaterialTheme.colorScheme.tertiary.copy(alpha = 0.35f).compositeOver(surface)))
        MiniPlayerStyle.Accent -> Brush.linearGradient(listOf(accent, accent))
        MiniPlayerStyle.Neutral -> Brush.linearGradient(listOf(MaterialTheme.colorScheme.surfaceContainerHighest, MaterialTheme.colorScheme.surfaceContainerHighest))
    }
}

@Composable
private fun MiniShapePreview(layout: MiniLayout) {
    val shape: Shape =
        when (layout) {
            MiniLayout.Joined -> RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
            MiniLayout.Floating -> RoundedCornerShape(12.dp)
            MiniLayout.Pill -> RoundedCornerShape(50)
            MiniLayout.Large -> RoundedCornerShape(14.dp)
        }
    // Sotto: la barra delle schede, per far vedere quanto si allarga il mini player
    val width =
        when (layout) {
            MiniLayout.Joined -> 1f
            MiniLayout.Pill -> 0.62f
            MiniLayout.Floating -> 0.8f
            MiniLayout.Large -> 0.98f
        }
    val height = if (layout == MiniLayout.Large) 36.dp else 24.dp
    if (layout == MiniLayout.Joined) {
        // Un solo blocco: mini player sopra, schede sotto
        Column(Modifier.fillMaxWidth().clip(shape).background(MaterialTheme.colorScheme.primary).padding(horizontal = 8.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(Modifier.fillMaxWidth().height(18.dp).clip(RoundedCornerShape(6.dp)).background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.35f)))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                repeat(3) { Box(Modifier.size(width = 18.dp, height = 10.dp).clip(RoundedCornerShape(5.dp)).background(MaterialTheme.colorScheme.onPrimary.copy(alpha = if (it == 0) 0.9f else 0.4f))) }
            }
        }
        return
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Box(Modifier.fillMaxWidth(width).height(height).clip(shape).background(MaterialTheme.colorScheme.primary))
        Box(Modifier.fillMaxWidth(0.62f).height(16.dp).clip(RoundedCornerShape(50)).background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)))
    }
}

@Composable
private fun MiniArtworkPreview(
    artwork: MiniArtwork,
    size: androidx.compose.ui.unit.Dp,
) {
    val art = Brush.linearGradient(listOf(Color(0xFFFF7A59), Color(0xFFC2185B)))
    when (artwork) {
        MiniArtwork.Vinyl ->
            Box(Modifier.size(size).clip(CircleShape).background(Color(0xFF111114)), contentAlignment = Alignment.Center) {
                Box(Modifier.size(size * 0.42f).clip(CircleShape).background(art))
            }
        else -> {
            val shape: Shape =
                when (artwork) {
                    MiniArtwork.Circle -> CircleShape
                    MiniArtwork.Cookie -> MaterialShapes.Cookie9Sided.toShape()
                    else -> RoundedCornerShape(size * 0.28f)
                }
            Box(Modifier.size(size).clip(shape).background(art))
        }
    }
}

@Composable
private fun MiniProgressPreview(progress: MiniProgress) {
    val primary = MaterialTheme.colorScheme.primary
    Box(Modifier.fillMaxWidth().height(36.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceContainerHighest)) {
        when (progress) {
            MiniProgress.Line ->
                Box(Modifier.align(Alignment.BottomStart).padding(horizontal = 8.dp).fillMaxWidth(0.5f).height(3.dp).background(primary))
            MiniProgress.Wave ->
                androidx.compose.material3.LinearWavyProgressIndicator(
                    progress = { 0.5f },
                    color = primary,
                    trackColor = primary.copy(alpha = 0.2f),
                    modifier = Modifier.align(Alignment.BottomCenter).padding(horizontal = 8.dp, vertical = 4.dp).fillMaxWidth().height(6.dp),
                )
            MiniProgress.Frame ->
                Box(Modifier.padding(4.dp).size(28.dp).border(3.dp, primary, RoundedCornerShape(8.dp)))
            MiniProgress.Fill -> Box(Modifier.fillMaxWidth(0.5f).height(36.dp).background(primary.copy(alpha = 0.25f)))
            MiniProgress.Ring ->
                androidx.compose.material3.CircularProgressIndicator(
                    progress = { 0.6f },
                    modifier = Modifier.padding(4.dp).size(28.dp),
                    color = primary,
                    strokeWidth = 3.dp,
                    trackColor = Color.Transparent,
                )
            MiniProgress.None -> Unit
        }
    }
}

/** Mini player finto che riflette tutte le scelte, per vedere subito l'effetto. */
@Composable
private fun MiniPlayerPreview(style: com.justlorenzo.archivetunex.ui.AtxStyle) {
    val accent = MaterialTheme.colorScheme.primary
    val content =
        if (style.miniPlayer == MiniPlayerStyle.Accent) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    val (shape, padding, height, artSize) =
        when (style.miniLayout) {
            MiniLayout.Joined -> PreviewGeometry(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp), 0.dp, 72.dp, 48.dp)
            MiniLayout.Floating -> PreviewGeometry(RoundedCornerShape(26.dp), 16.dp, 68.dp, 48.dp)
            MiniLayout.Pill -> PreviewGeometry(RoundedCornerShape(50), 32.dp, 58.dp, 42.dp)
            MiniLayout.Large -> PreviewGeometry(RoundedCornerShape(30.dp), 16.dp, 92.dp, 72.dp)
        }
    Box(Modifier.padding(horizontal = padding).fillMaxWidth().clip(shape).background(miniBrush(style.miniPlayer))) {
        if (style.miniProgress == MiniProgress.Fill) Box(Modifier.fillMaxWidth(0.45f).height(height).background(accent.copy(alpha = 0.2f)))
        Row(
            Modifier.fillMaxWidth().height(height).padding(start = 10.dp, end = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                MiniArtworkPreview(
                    if (style.miniLayout == MiniLayout.Pill && style.miniArtwork == MiniArtwork.Rounded) MiniArtwork.Circle else style.miniArtwork,
                    artSize,
                )
                if (style.miniProgress == MiniProgress.Ring) {
                    androidx.compose.material3.CircularProgressIndicator(
                        progress = { 0.45f },
                        modifier = Modifier.size(artSize + 6.dp),
                        color = accent,
                        strokeWidth = 3.dp,
                        trackColor = Color.Transparent,
                    )
                }
            }
            Column(Modifier.weight(1f)) {
                Text("Titolo del brano", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = content)
                if (style.miniShowArtist) Text("Artista", style = MaterialTheme.typography.bodyMedium, color = content.copy(alpha = 0.7f))
            }
            if (style.miniButtons == MiniButtons.All) AtxIcon(Res.drawable.skip_previous, null, size = 26.dp, tint = content)
            AtxIcon(Res.drawable.play, null, size = 30.dp, tint = content)
            if (style.miniButtons != MiniButtons.PlayOnly) AtxIcon(Res.drawable.skip_next, null, size = 28.dp, tint = content)
        }
        if (style.miniProgress == MiniProgress.Line) {
            Box(
                Modifier.align(Alignment.BottomStart).padding(horizontal = 22.dp).fillMaxWidth(0.45f).height(3.dp).clip(RoundedCornerShape(2.dp))
                    .background(if (style.miniPlayer == MiniPlayerStyle.Accent) content else accent),
            )
        }
    }
}

private data class PreviewGeometry(
    val shape: Shape,
    val padding: androidx.compose.ui.unit.Dp,
    val height: androidx.compose.ui.unit.Dp,
    val artSize: androidx.compose.ui.unit.Dp,
)

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
            ValueRow(Res.drawable.info, "Nami", "Motore ArchiveTune ${BuildConfig.VERSION_NAME}") {}
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
