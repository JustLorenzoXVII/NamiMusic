/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.rukamori.archivetune.ui.screens

import moe.rukamori.archivetune.compat.putIfAbsent

import archivetunex.shared.generated.resources.*

import moe.rukamori.archivetune.compat.Intent
import moe.rukamori.archivetune.compat.startActivity
import moe.rukamori.archivetune.compat.Uri
import moe.rukamori.archivetune.compat.Toast
import moe.rukamori.archivetune.compat.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import moe.rukamori.archivetune.compat.LocalContext
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import moe.rukamori.archivetune.LocalPlayerAwareWindowInsets
import moe.rukamori.archivetune.innertube.utils.hasCompleteYouTubeLoginCookies
import moe.rukamori.archivetune.ui.component.IconButton
import moe.rukamori.archivetune.ui.utils.backToMain
import moe.rukamori.archivetune.viewmodels.LoginScreenState
import moe.rukamori.archivetune.viewmodels.LoginViewModel

const val LOGIN_ROUTE = "login"
const val LOGIN_URL_ARGUMENT = "url"

fun buildLoginRoute(startUrl: String? = null): String {
    val resolvedUrl = startUrl?.trim().takeUnless { it.isNullOrBlank() } ?: return LOGIN_ROUTE
    return "$LOGIN_ROUTE?$LOGIN_URL_ARGUMENT=${Uri.encode(resolvedUrl)}"
}

private const val DEFAULT_LOGIN_URL = "https://accounts.google.com/ServiceLogin?continue=https%3A%2F%2Fmusic.youtube.com"
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    navController: NavController,
    startUrl: String? = null,
    onLoginComplete: (() -> Unit)? = null,
    onNavigateBack: (() -> Unit)? = null,
    viewModel: LoginViewModel = koinViewModel(),
) {
    val context = LocalContext.current

    val screenState by viewModel.screenState.collectAsStateWithLifecycle()
    val loginSuccessMessage = stringResource(Res.string.login_success)
    val webNavigator = remember { WebNavigator() }

    LaunchedEffect(screenState, loginSuccessMessage) {
        if (screenState is LoginScreenState.Success) {
            Toast.makeText(context, loginSuccessMessage, Toast.LENGTH_SHORT).show()
            if (onLoginComplete != null) {
                onLoginComplete()
            } else {
                navController.navigateUp()
            }
        }
    }

    YouTubeLoginWebView(
        startUrl = startUrl?.takeIf { it.isNotBlank() } ?: DEFAULT_LOGIN_URL,
        navigator = webNavigator,
        onCookiesCaptured = viewModel::onCookiesCaptured,
        onVisitorData = viewModel::onVisitorDataExtracted,
        onDataSyncId = viewModel::onDataSyncIdExtracted,
        modifier =
            Modifier
                .windowInsetsPadding(LocalPlayerAwareWindowInsets.current)
                .fillMaxSize(),
    )

    TopAppBar(
        title = { Text(stringResource(Res.string.login)) },
        navigationIcon = {
            IconButton(
                onClick = {
                    if (onNavigateBack != null) {
                        onNavigateBack()
                    } else {
                        navController.navigateUp()
                    }
                },
                onLongClick = {
                    if (onNavigateBack != null) {
                        onNavigateBack()
                    } else {
                        navController.backToMain()
                    }
                },
            ) {
                Icon(
                    painterResource(Res.drawable.arrow_back),
                    contentDescription = null,
                )
            }
        },
    )

    BackHandler(enabled = onNavigateBack != null || webNavigator.canGoBack) {
        if (webNavigator.canGoBack) {
            webNavigator.goBack()
        } else {
            onNavigateBack?.invoke()
        }
    }
}

