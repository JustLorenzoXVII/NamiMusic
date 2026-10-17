package com.justlorenzo.archivetunex.ui.screens

import androidx.compose.foundation.clickable
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

/** Foglio dell'account: login con Google, logout e registro degli errori. */
@Composable
fun AccountSheet() {
    val navigator = LocalNavigator.current
    val account = rememberAccount()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(onDismissRequest = { navigator.accountVisible = false }, sheetState = sheetState) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                account?.name ?: "Non sei collegato",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                account?.email ?: "Collega il tuo account Google per vedere la tua Home, le tue playlist e sincronizzare i \"Mi piace\".",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            if (account == null) {
                Button(
                    onClick = {
                        navigator.accountVisible = false
                        navigator.loginVisible = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    AtxIcon(Res.drawable.login, null, size = 20.dp)
                    Text("Accedi con Google", modifier = Modifier.padding(start = 8.dp))
                }
            } else {
                OutlinedButton(
                    onClick = {
                        App.Companion.forgetAccount(AppContext)
                        navigator.accountVisible = false
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    AtxIcon(Res.drawable.logout, null, size = 20.dp)
                    Text("Esci dall'account", modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
        HorizontalDivider(Modifier.padding(vertical = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
        ListItem(
            headlineContent = { Text("Impostazioni") },
            supportingContent = { Text("Tema, qualità audio, contenuti, download") },
            leadingContent = { AtxIcon(Res.drawable.settings, null) },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            modifier =
                Modifier.clickable {
                    navigator.accountVisible = false
                    navigator.settingsVisible = true
                },
        )
        ListItem(
            headlineContent = { Text("Registro errori") },
            supportingContent = { Text("Per capire cosa non funziona") },
            leadingContent = { AtxIcon(Res.drawable.manage_search, null) },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            modifier =
                Modifier.clickable {
                    navigator.accountVisible = false
                    navigator.logsVisible = true
                },
        )
        Spacer(Modifier.height(24.dp))
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
