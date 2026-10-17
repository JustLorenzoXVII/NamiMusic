/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.rukamori.archivetune.ui.screens.settings

import archivetunex.shared.generated.resources.*

import kotlinx.coroutines.IO

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import moe.rukamori.archivetune.compat.LocalContext
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.navigation.NavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import moe.rukamori.archivetune.LocalPlayerAwareWindowInsets
import moe.rukamori.archivetune.constants.*
import moe.rukamori.archivetune.innertube.YouTube
import moe.rukamori.archivetune.ui.component.*
import moe.rukamori.archivetune.ui.utils.backToMain
import moe.rukamori.archivetune.utils.ProxyUtils
import moe.rukamori.archivetune.utils.rememberEnumPreference
import moe.rukamori.archivetune.utils.rememberPreference
import moe.rukamori.archivetune.compat.okhttp3.OkHttpClient
import moe.rukamori.archivetune.compat.okhttp3.Request
import moe.rukamori.archivetune.compat.Proxy
import moe.rukamori.archivetune.compat.TimeUnit

@Composable
fun InternetWarningBox(modifier: Modifier = Modifier) {
    Card(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(SettingsDimensions.BannerCardCornerRadius),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f),
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier =
                    Modifier
                        .size(SettingsDimensions.BannerIconSize)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.error.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(Res.drawable.error),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(SettingsDimensions.BannerIconInnerSize),
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = stringResource(Res.string.internet_warning_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
                Text(
                    text = stringResource(Res.string.internet_warning_doh),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f),
                )
                Text(
                    text = stringResource(Res.string.internet_warning_proxy),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun InternetSettings(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val (dnsOverHttpsEnabled, onDnsOverHttpsEnabledChange) = rememberPreference(key = EnableDnsOverHttpsKey, defaultValue = false)
    val (dnsProvider, onDnsProviderChange) = rememberPreference(key = DnsOverHttpsProviderKey, defaultValue = "Cloudflare")
    val (customDnsUrl, onCustomDnsUrlChange) = rememberPreference(key = stringPreferencesKey("customDnsUrl"), defaultValue = "https://")
    val (proxyEnabled, onProxyEnabledChange) = rememberPreference(key = ProxyEnabledKey, defaultValue = false)
    val (proxyType, onProxyTypeChange) = rememberEnumPreference(key = ProxyTypeKey, defaultValue = Proxy.Type.HTTP)
    val (proxyHost, onProxyHostChange) = rememberPreference(key = ProxyHostKey, defaultValue = "")
    val (proxyPort, onProxyPortChange) = rememberPreference(key = ProxyPortKey, defaultValue = 8080)
    val (proxyUsername, onProxyUsernameChange) = rememberPreference(key = ProxyUsernameKey, defaultValue = "")
    val (proxyPassword, onProxyPasswordChange) = rememberPreference(key = ProxyPasswordKey, defaultValue = "")
    val (streamBypassProxy, onStreamBypassProxyChange) = rememberPreference(key = StreamBypassProxyKey, defaultValue = false)

    var testingProxy by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<String?>(null) }

    val dnsProviders = remember { listOf("Cloudflare", "Google", "AdGuard", "Quad9", "Custom") }
    val proxyTypes = remember { listOf(Proxy.Type.HTTP, Proxy.Type.SOCKS) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.internet)) },
                navigationIcon = {
                    IconButton(
                        onClick = navController::navigateUp,
                        onLongClick = navController::backToMain,
                    ) {
                        Icon(
                            painterResource(Res.drawable.arrow_back),
                            contentDescription = null,
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        val topPadding = innerPadding.calculateTopPadding()

        Column(
            Modifier
                .padding(top = topPadding)
                .windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
                .verticalScroll(rememberScrollState())
                .padding(bottom = SettingsDimensions.ScreenBottomPadding),
        ) {
            InternetWarningBox()

            PreferenceGroup(title = stringResource(Res.string.dns_over_https)) {
                item {
                    SwitchPreference(
                        title = { Text(stringResource(Res.string.dns_over_https)) },
                        description = stringResource(Res.string.dns_over_https_desc),
                        icon = { Icon(painterResource(Res.drawable.security), null) },
                        checked = dnsOverHttpsEnabled,
                        onCheckedChange = onDnsOverHttpsEnabledChange,
                    )
                }

                item(visible = dnsOverHttpsEnabled) {
                    ListPreference(
                        title = { Text(stringResource(Res.string.dns_provider)) },
                        icon = { Icon(painterResource(Res.drawable.website), null) },
                        selectedValue = dnsProvider,
                        values = dnsProviders,
                        valueText = { it },
                        onValueSelected = onDnsProviderChange,
                    )
                }

                item(visible = dnsOverHttpsEnabled && dnsProvider == "Custom") {
                    EditTextPreference(
                        title = { Text(stringResource(Res.string.dns_custom_url)) },
                        value = customDnsUrl,
                        onValueChange = onCustomDnsUrlChange,
                    )
                }
            }

            PreferenceGroup(title = stringResource(Res.string.proxy)) {
                item {
                    SwitchPreference(
                        title = { Text(stringResource(Res.string.enable_proxy)) },
                        icon = { Icon(painterResource(Res.drawable.wifi_proxy), null) },
                        checked = proxyEnabled,
                        onCheckedChange = {
                            onProxyEnabledChange(it)
                            ProxyUtils.applyYouTubeProxy(it, proxyType, proxyHost, proxyPort, proxyUsername, proxyPassword)
                        },
                    )
                }

                item(visible = proxyEnabled) {
                    ListPreference(
                        title = { Text(stringResource(Res.string.proxy_type)) },
                        selectedValue = proxyType,
                        values = proxyTypes,
                        valueText = { it.name },
                        onValueSelected = {
                            onProxyTypeChange(it)
                            ProxyUtils.applyYouTubeProxy(proxyEnabled, it, proxyHost, proxyPort, proxyUsername, proxyPassword)
                        },
                    )
                }

                item(visible = proxyEnabled) {
                    EditTextPreference(
                        title = { Text(stringResource(Res.string.proxy_host)) },
                        value = proxyHost,
                        onValueChange = {
                            onProxyHostChange(it)
                            ProxyUtils.applyYouTubeProxy(proxyEnabled, proxyType, it, proxyPort, proxyUsername, proxyPassword)
                        },
                    )
                }

                item(visible = proxyEnabled) {
                    NumberEditTextPreference(
                        title = { Text(stringResource(Res.string.proxy_port)) },
                        value = proxyPort,
                        onValueChange = {
                            onProxyPortChange(it)
                            ProxyUtils.applyYouTubeProxy(proxyEnabled, proxyType, proxyHost, it, proxyUsername, proxyPassword)
                        },
                        isInputValid = { it.toIntOrNull() in 1..65535 },
                    )
                }
            }

            if (proxyEnabled) {
                PreferenceGroup(title = stringResource(Res.string.proxy_auth)) {
                    item {
                        EditTextPreference(
                            title = { Text(stringResource(Res.string.proxy_username)) },
                            value = proxyUsername,
                            onValueChange = {
                                onProxyUsernameChange(it)
                                ProxyUtils.applyYouTubeProxy(proxyEnabled, proxyType, proxyHost, proxyPort, it, proxyPassword)
                            },
                        )
                    }

                    item {
                        EditTextPreference(
                            title = { Text(stringResource(Res.string.proxy_password)) },
                            value = proxyPassword,
                            onValueChange = {
                                onProxyPasswordChange(it)
                                ProxyUtils.applyYouTubeProxy(proxyEnabled, proxyType, proxyHost, proxyPort, proxyUsername, it)
                            },
                        )
                    }

                    item {
                        SwitchPreference(
                            title = { Text(stringResource(Res.string.stream_bypass_proxy)) },
                            description = stringResource(Res.string.stream_bypass_proxy_desc),
                            icon = { Icon(painterResource(Res.drawable.wifi_proxy), null) },
                            checked = streamBypassProxy,
                            onCheckedChange = {
                                onStreamBypassProxyChange(it)
                                YouTube.streamBypassProxy = it
                            },
                        )
                    }

                    item {
                        PreferenceEntry(
                            title = { Text(stringResource(Res.string.test_proxy_connection)) },
                            icon = { Icon(painterResource(Res.drawable.check), null) },
                            onClick = {
                                if (testingProxy) return@PreferenceEntry
                                scope.launch(Dispatchers.IO) {
                                    testingProxy = true
                                    try {
                                        val proxy = ProxyUtils.createProxyOrNull(proxyType, proxyHost, proxyPort)
                                        if (proxy == null) {
                                            testResult =
                                                context.getString(
                                                    Res.string.proxy_connection_failed,
                                                    context.getString(Res.string.proxy_connection_invalid_configuration),
                                                )
                                            return@launch
                                        }
                                        val clientBuilder =
                                            OkHttpClient
                                                .Builder()
                                                .proxy(proxy)
                                                .connectTimeout(10, TimeUnit.SECONDS)
                                                .readTimeout(10, TimeUnit.SECONDS)

                                        if (proxyUsername.isNotBlank() && proxyPassword.isNotBlank()) {
                                            clientBuilder.proxyAuthenticator { _, response ->
                                                val credential = moe.rukamori.archivetune.compat.okhttp3.Credentials.basic(proxyUsername, proxyPassword)
                                                response.request
                                                    .newBuilder()
                                                    .header("Proxy-Authorization", credential)
                                                    .build()
                                            }
                                        }

                                        val client = clientBuilder.build()
                                        val request =
                                            Request
                                                .Builder()
                                                .url("https://music.youtube.com/generate_204")
                                                .build()
                                        client.newCall(request).execute().use { response ->
                                            testResult =
                                                if (response.isSuccessful || response.code == 204) {
                                                    context.getString(Res.string.proxy_connection_success)
                                                } else {
                                                    context.getString(Res.string.proxy_connection_failed, "HTTP ${response.code}")
                                                }
                                        }
                                    } catch (e: Exception) {
                                        testResult =
                                            context.getString(
                                                Res.string.proxy_connection_failed,
                                                e.message ?: context.getString(Res.string.error_unknown),
                                            )
                                    } finally {
                                        testingProxy = false
                                    }
                                }
                            },
                        )
                    }
                }
            }

        }
    }

    if (testingProxy) {
        DefaultDialog(
            onDismiss = { },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CircularWavyProgressIndicator(
                modifier = Modifier.size(48.dp),
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(16.dp))
            Text(stringResource(Res.string.testing_proxy_connection))
        }
    }

    if (testResult != null) {
        ActionPromptDialog(
            title = stringResource(Res.string.test_proxy_connection),
            onDismiss = { testResult = null },
            onConfirm = { testResult = null },
            content = {
                Text(testResult!!)
            },
        )
    }
}
