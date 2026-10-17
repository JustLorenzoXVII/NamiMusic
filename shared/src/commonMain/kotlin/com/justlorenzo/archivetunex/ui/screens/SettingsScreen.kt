package com.justlorenzo.archivetunex.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import archivetunex.shared.generated.resources.Res
import archivetunex.shared.generated.resources.close
import archivetunex.shared.generated.resources.delete
import archivetunex.shared.generated.resources.delete_history
import archivetunex.shared.generated.resources.info
import archivetunex.shared.generated.resources.language
import archivetunex.shared.generated.resources.location_on
import archivetunex.shared.generated.resources.manage_search
import archivetunex.shared.generated.resources.storage
import archivetunex.shared.generated.resources.tune
import com.justlorenzo.archivetunex.ui.LocalActions
import com.justlorenzo.archivetunex.ui.LocalNavigator
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

/** Impostazioni: aspetto, riproduzione, contenuti, download e informazioni. */
@Composable
fun SettingsOverlay() {
    val actions = LocalActions.current
    val navigator = LocalNavigator.current
    var darkMode by rememberEnumPreference(DarkModeKey, DarkMode.AUTO)
    var audioQuality by rememberEnumPreference(AudioQualityKey, AudioQuality.HIGHEST)
    var contentLanguage by rememberPreference(ContentLanguageKey, SYSTEM_DEFAULT)
    var contentCountry by rememberPreference(ContentCountryKey, SYSTEM_DEFAULT)

    var dialog by remember { mutableStateOf<String?>(null) }
    val downloads by actions.downloads.downloads.collectAsState()
    var usedBytes by remember { mutableLongStateOf(0L) }
    LaunchedEffect(downloads) {
        usedBytes =
            withContext(Dispatchers.IO) {
                File(AppContext.filesDir, "downloads").listFiles()?.sumOf { if (it.isFile) it.length() else 0L } ?: 0L
            }
    }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { navigator.settingsVisible = false }) { AtxIcon(Res.drawable.close, "Chiudi") }
                Text("Impostazioni", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            }
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).navigationBarsPadding().padding(bottom = 24.dp)) {
                SettingsHeader("Aspetto")
                Text(
                    "Tema",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 8.dp),
                )
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
                    val options = listOf(DarkMode.AUTO to "Sistema", DarkMode.OFF to "Chiaro", DarkMode.ON to "Scuro")
                    options.forEachIndexed { index, (mode, label) ->
                        SegmentedButton(
                            selected = darkMode == mode,
                            onClick = { darkMode = mode },
                            shape = SegmentedButtonDefaults.itemShape(index, options.size),
                        ) { Text(label) }
                    }
                }

                SettingsHeader("Riproduzione")
                SettingsRow(Res.drawable.tune, "Qualità audio", audioQuality.label) { dialog = "quality" }

                SettingsHeader("Contenuti")
                SettingsRow(
                    Res.drawable.language,
                    "Lingua dei contenuti",
                    LanguageCodeToName[contentLanguage] ?: "Predefinita del sistema",
                ) { dialog = "language" }
                SettingsRow(
                    Res.drawable.location_on,
                    "Paese dei contenuti",
                    CountryCodeToName[contentCountry] ?: "Predefinito del sistema",
                ) { dialog = "country" }

                SettingsHeader("Download")
                SettingsRow(
                    Res.drawable.storage,
                    "Spazio occupato",
                    "${formatBytes(usedBytes)} · ${downloads.size} brani",
                ) {}
                SettingsRow(Res.drawable.delete, "Elimina tutti i download", "Libera spazio sul telefono") { dialog = "deleteDownloads" }

                SettingsHeader("Privacy")
                SettingsRow(Res.drawable.delete_history, "Cancella cronologia ricerche", null) { dialog = "clearHistory" }

                SettingsHeader("Informazioni")
                SettingsRow(Res.drawable.manage_search, "Registro errori", "Per capire cosa non funziona") {
                    navigator.settingsVisible = false
                    navigator.logsVisible = true
                }
                SettingsRow(Res.drawable.info, "ArchiveTuneX", "Motore ArchiveTune ${BuildConfig.VERSION_NAME}") {}
            }
        }
    }

    when (dialog) {
        "quality" ->
            ChoiceDialog(
                title = "Qualità audio",
                options = listOf(AudioQuality.HIGHEST, AudioQuality.HIGH, AudioQuality.AUTO, AudioQuality.LOW).map { it to it.label },
                selected = audioQuality,
                onSelect = { audioQuality = it },
                onDismiss = { dialog = null },
            )
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
        "deleteDownloads" ->
            ConfirmDialog(
                title = "Eliminare tutti i download?",
                text = "I brani scaricati vengono tolti dal telefono. Potrai riscaricarli quando vuoi.",
                confirmLabel = "Elimina",
                onDismiss = { dialog = null },
                onConfirm = { actions.downloads.removeAllDownloads() },
            )
        "clearHistory" ->
            ConfirmDialog(
                title = "Cancellare la cronologia delle ricerche?",
                text = "Le ricerche recenti non compariranno più nella scheda Cerca.",
                confirmLabel = "Cancella",
                onDismiss = { dialog = null },
                onConfirm = { actions.database.query { clearSearchHistory() } },
            )
    }
}

private fun formatBytes(bytes: Long): String {
    val mb = bytes / (1024.0 * 1024.0)
    return if (mb >= 1024) {
        "${(mb / 1024 * 10).toLong() / 10.0} GB"
    } else {
        "${(mb * 10).toLong() / 10.0} MB"
    }
}

@Composable
private fun SettingsHeader(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 8.dp),
    )
}

@Composable
private fun SettingsRow(
    icon: DrawableResource,
    title: String,
    subtitle: String?,
    onClick: () -> Unit,
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = subtitle?.let { { Text(it) } },
        leadingContent = { AtxIcon(icon, null) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.clickable(onClick = onClick),
    )
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
