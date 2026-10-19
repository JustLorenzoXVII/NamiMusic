package com.justlorenzo.archivetunex.ui.screens

import com.justlorenzo.archivetunex.ui.tr
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.text.style.TextOverflow
import archivetunex.shared.generated.resources.download
import archivetunex.shared.generated.resources.favorite
import archivetunex.shared.generated.resources.navigate_next
import archivetunex.shared.generated.resources.playlist_play
import com.justlorenzo.archivetunex.ui.LocalActions
import com.justlorenzo.archivetunex.ui.Route
import com.justlorenzo.archivetunex.ui.Tab
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import moe.rukamori.archivetune.compat.media3.Download
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.toShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import archivetunex.shared.generated.resources.person
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

/**
 * Foglio dell'account: il profilo in una card colorata, i propri numeri (preferiti, playlist,
 * scaricati) e poche voci in lista. Le opzioni di personalizzazione stanno nelle Impostazioni.
 */
@Composable
fun AccountSheet() {
    val navigator = LocalNavigator.current
    val actions = LocalActions.current
    val account = rememberAccount()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var confirmLogout by remember { mutableStateOf(false) }
    val likedCount by remember { actions.database.likedSongsCount() }.collectAsState(0)
    val playlistCount by remember { actions.database.playlistsByCreateDateAsc().map { it.size } }.collectAsState(0)
    val downloadedCount by remember {
        actions.downloads.downloads.map { map -> map.count { it.value.state == Download.STATE_COMPLETED } }.distinctUntilChanged()
    }.collectAsState(0)

    fun openLibrary(route: Route?) {
        navigator.accountVisible = false
        navigator.selectTab(Tab.Library)
        route?.let(navigator::open)
    }

    val primary = MaterialTheme.colorScheme.primary
    val onPrimary = MaterialTheme.colorScheme.onPrimary

    ModalBottomSheet(
        onDismissRequest = { navigator.accountVisible = false },
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // ---------- Profilo ----------
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(32.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(primary, primary.copy(alpha = 0.55f).compositeOver(MaterialTheme.colorScheme.tertiary)),
                        ),
                    ),
            ) {
                // Forme decorative sullo sfondo della card
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 40.dp, y = (-30).dp)
                        .size(150.dp)
                        .rotate(18f)
                        .clip(MaterialShapes.Cookie12Sided.toShape())
                        .background(onPrimary.copy(alpha = 0.10f)),
                )
                Box(
                    Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = (-70).dp, y = 36.dp)
                        .size(80.dp)
                        .clip(MaterialShapes.Pill.toShape())
                        .background(onPrimary.copy(alpha = 0.08f)),
                )
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Box(
                        Modifier.size(68.dp).clip(MaterialShapes.Cookie9Sided.toShape()).background(onPrimary.copy(alpha = 0.92f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (account != null) {
                            Text(
                                account.name.first().uppercase(),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Black,
                                color = primary,
                            )
                        } else {
                            AtxIcon(Res.drawable.person, null, size = 34.dp, tint = primary)
                        }
                    }
                    Column {
                        Text(
                            account?.name ?: tr("Ospite"),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Black,
                            color = onPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            account?.email ?: tr("Non collegato a YouTube Music"),
                            style = MaterialTheme.typography.bodyMedium,
                            color = onPrimary.copy(alpha = 0.8f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    if (account == null) {
                        Row(
                            Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(onPrimary)
                                .clickable {
                                    navigator.accountVisible = false
                                    navigator.loginVisible = true
                                }.padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            AtxIcon(Res.drawable.login, null, size = 18.dp, tint = primary)
                            Text(tr("Accedi con Google"), color = primary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // ---------- I tuoi numeri ----------
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatTile(likedCount, tr("Preferiti"), Res.drawable.favorite, Modifier.weight(1f)) { openLibrary(Route.Liked) }
                StatTile(playlistCount, tr("Playlist"), Res.drawable.playlist_play, Modifier.weight(1f)) { openLibrary(null) }
                StatTile(downloadedCount, tr("Scaricati"), Res.drawable.download, Modifier.weight(1f)) { openLibrary(Route.Downloads) }
            }

            // ---------- Voci ----------
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                SheetRow(Res.drawable.settings, tr("Impostazioni"), tr("Aspetto, player, riproduzione e altro"), RowPosition.First) {
                    navigator.openSettings()
                }
                SheetRow(
                    Res.drawable.manage_search,
                    tr("Registro errori"),
                    tr("Per capire cosa non va"),
                    if (account != null) RowPosition.Middle else RowPosition.Last,
                ) {
                    navigator.accountVisible = false
                    navigator.logsVisible = true
                }
                if (account != null) {
                    SheetRow(Res.drawable.logout, tr("Esci dall'account"), null, RowPosition.Last, danger = true) { confirmLogout = true }
                }
            }
        }
    }

    if (confirmLogout) {
        ConfirmDialog(
            title = tr("Uscire dall'account?"),
            text = tr("La Home e le playlist torneranno quelle senza account. I brani scaricati restano."),
            confirmLabel = tr("Esci"),
            onDismiss = { confirmLogout = false },
            onConfirm = {
                App.Companion.forgetAccount(AppContext)
                navigator.accountVisible = false
            },
        )
    }
}

/** Numero grande con icona: tocca per aprire la sezione della libreria. */
@Composable
private fun StatTile(
    count: Int,
    label: String,
    icon: org.jetbrains.compose.resources.DrawableResource,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Column(
        modifier
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        AtxIcon(icon, null, size = 20.dp, tint = MaterialTheme.colorScheme.primary)
        Text(count.toString(), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
    }
}

private enum class RowPosition { First, Middle, Last }

/** Voce del foglio: angoli grandi solo all'inizio e alla fine del gruppo (stile Expressive). */
@Composable
private fun SheetRow(
    icon: org.jetbrains.compose.resources.DrawableResource,
    title: String,
    subtitle: String?,
    position: RowPosition,
    danger: Boolean = false,
    onClick: () -> Unit,
) {
    val big = 24.dp
    val small = 6.dp
    val shape =
        when (position) {
            RowPosition.First -> RoundedCornerShape(topStart = big, topEnd = big, bottomStart = small, bottomEnd = small)
            RowPosition.Middle -> RoundedCornerShape(small)
            RowPosition.Last -> RoundedCornerShape(topStart = small, topEnd = small, bottomStart = big, bottomEnd = big)
        }
    val color = if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(if (danger) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            AtxIcon(
                icon,
                null,
                size = 20.dp,
                tint = if (danger) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (!danger) AtxIcon(Res.drawable.navigate_next, null, size = 22.dp, tint = MaterialTheme.colorScheme.onSurfaceVariant)
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
                IconButton(onClick = { navigator.loginVisible = false }) { AtxIcon(Res.drawable.close, tr("Chiudi")) }
                Text(
                    tr("Accedi con Google"),
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
                    tr("Accesso non riuscito. Riprova."),
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
                IconButton(onClick = { navigator.logsVisible = false }) { AtxIcon(Res.drawable.close, tr("Chiudi")) }
                Text(tr("Registro errori"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                TextButton(onClick = {
                    clipboard.setText(AnnotatedString(logs.joinToString("\n") { GlobalLog.format(it) }))
                    copied = true
                }) { Text(if (copied) tr("Copiato") else tr("Copia")) }
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
