package com.justlorenzo.archivetunex.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.toShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import archivetunex.shared.generated.resources.palette
import archivetunex.shared.generated.resources.person
import archivetunex.shared.generated.resources.play
import com.justlorenzo.archivetunex.ui.SettingsPage
import com.justlorenzo.archivetunex.ui.components.ConfirmDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import archivetunex.shared.generated.resources.Res
import archivetunex.shared.generated.resources.close
import archivetunex.shared.generated.resources.login
import archivetunex.shared.generated.resources.logout
import archivetunex.shared.generated.resources.manage_search
import archivetunex.shared.generated.resources.settings
import com.justlorenzo.archivetunex.ui.LocalNavigator
import com.justlorenzo.archivetunex.ui.components.AtxIcon
import com.justlorenzo.archivetunex.ui.components.rememberAccount
import moe.rukamori.archivetune.App
import moe.rukamori.archivetune.compat.AppContext
import moe.rukamori.archivetune.ui.screens.WebNavigator
import moe.rukamori.archivetune.ui.screens.YouTubeLoginWebView
import moe.rukamori.archivetune.utils.GlobalLog
import moe.rukamori.archivetune.viewmodels.LoginScreenState
import moe.rukamori.archivetune.viewmodels.LoginViewModel
import org.koin.compose.viewmodel.koinViewModel

/** Foglio dell'account: profilo in grande e scorciatoie a riquadri. */
@Composable
fun AccountSheet() {
    val navigator = LocalNavigator.current
    val account = rememberAccount()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var confirmLogout by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = { navigator.accountVisible = false },
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Profilo: avatar a "biscotto" con l'iniziale e il nome in grande
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(
                    Modifier
                        .size(72.dp)
                        .clip(MaterialShapes.Cookie9Sided.toShape())
                        .background(
                            if (account != null) {
                                Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)))
                            } else {
                                Brush.linearGradient(listOf(MaterialTheme.colorScheme.surfaceContainerHighest, MaterialTheme.colorScheme.surfaceContainerHigh))
                            },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (account != null) {
                        Text(
                            account.name.first().uppercase(),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    } else {
                        AtxIcon(Res.drawable.person, null, size = 34.dp, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        account?.name ?: "Ospite",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                    )
                    Text(
                        account?.email ?: "Non collegato a YouTube Music",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            }

            if (account == null) {
                // Invito al login: una card colorata invece di un semplice pulsante
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(28.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .clickable {
                            navigator.accountVisible = false
                            navigator.loginVisible = true
                        }.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        "Collega YouTube Music",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Text(
                        "La tua Home, le tue playlist e i \"Mi piace\" sincronizzati.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                    )
                    Row(
                        Modifier
                            .padding(top = 8.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(MaterialTheme.colorScheme.primary)
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        AtxIcon(Res.drawable.login, null, size = 18.dp, tint = MaterialTheme.colorScheme.onPrimary)
                        Text("Accedi con Google", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Scorciatoie a riquadri
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ShortcutTile(Res.drawable.palette, "Personalizza", Color(0xFFFA4058), Modifier.weight(1f)) {
                    navigator.openSettings(SettingsPage.Appearance)
                }
                ShortcutTile(Res.drawable.play, "Player", Color(0xFF8B5CF6), Modifier.weight(1f)) {
                    navigator.openSettings(SettingsPage.Player)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ShortcutTile(Res.drawable.settings, "Impostazioni", Color(0xFF3D7BFF), Modifier.weight(1f)) {
                    navigator.openSettings()
                }
                if (account != null) {
                    ShortcutTile(Res.drawable.logout, "Esci", Color(0xFF6B7280), Modifier.weight(1f)) { confirmLogout = true }
                } else {
                    ShortcutTile(Res.drawable.manage_search, "Registro errori", Color(0xFF6B7280), Modifier.weight(1f)) {
                        navigator.accountVisible = false
                        navigator.logsVisible = true
                    }
                }
            }
        }
    }

    if (confirmLogout) {
        ConfirmDialog(
            title = "Uscire dall'account?",
            text = "La Home e le playlist torneranno quelle senza account. I brani scaricati restano.",
            confirmLabel = "Esci",
            onDismiss = { confirmLogout = false },
            onConfirm = {
                App.Companion.forgetAccount(AppContext)
                navigator.accountVisible = false
            },
        )
    }
}

/** Riquadro tonale con icona a "biscotto" colorato. */
@Composable
private fun ShortcutTile(
    icon: org.jetbrains.compose.resources.DrawableResource,
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Column(
        modifier
            .height(112.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Box(
            Modifier.size(40.dp).clip(MaterialShapes.Cookie9Sided.toShape()).background(color),
            contentAlignment = Alignment.Center,
        ) { AtxIcon(icon, null, size = 20.dp, tint = Color.White) }
        Text(label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

/** Login con Google: la pagina di Google dentro l'app, come in ArchiveTune. */
@Composable
fun LoginOverlay() {
    val navigator = LocalNavigator.current
    val viewModel: LoginViewModel = koinViewModel()
    val state by viewModel.screenState.collectAsState()
    val webNavigator = remember { WebNavigator() }

    LaunchedEffect(state) {
        if (state is LoginScreenState.Success) navigator.loginVisible = false
    }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { navigator.loginVisible = false }) { AtxIcon(Res.drawable.close, "Chiudi") }
                Text(
                    "Accedi con Google",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                if (state is LoginScreenState.Loading) {
                    androidx.compose.material3.LoadingIndicator(Modifier.padding(end = 12.dp))
                }
            }
            Box(Modifier.weight(1f).fillMaxWidth()) {
                YouTubeLoginWebView(
                    startUrl = "https://accounts.google.com/ServiceLogin?continue=https%3A%2F%2Fmusic.youtube.com",
                    navigator = webNavigator,
                    onCookiesCaptured = viewModel::onCookiesCaptured,
                    onVisitorData = viewModel::onVisitorDataExtracted,
                    onDataSyncId = viewModel::onDataSyncIdExtracted,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            if (state is LoginScreenState.Error) {
                Text(
                    "Accesso non riuscito. Riprova.",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp).navigationBarsPadding(),
                )
            }
        }
    }
}

/** Registro degli errori dell'app, da copiare e mandare. */
@Composable
fun LogsOverlay() {
    val navigator = LocalNavigator.current
    val logs by GlobalLog.logs.collectAsState()
    val clipboard = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { navigator.logsVisible = false }) { AtxIcon(Res.drawable.close, "Chiudi") }
                Text("Registro errori", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                TextButton(onClick = {
                    clipboard.setText(AnnotatedString(logs.joinToString("\n") { GlobalLog.format(it) }))
                    copied = true
                }) { Text(if (copied) "Copiato" else "Copia") }
            }
            SelectionContainer(Modifier.weight(1f)) {
                LazyColumn(Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
                    items(logs.asReversed()) { entry ->
                        Text(
                            GlobalLog.format(entry),
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(vertical = 4.dp),
                        )
                    }
                }
            }
        }
    }
}
