package com.justlorenzo.archivetunex.ui.components

import com.justlorenzo.archivetunex.ui.tr
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import archivetunex.shared.generated.resources.Res
import archivetunex.shared.generated.resources.person
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import moe.rukamori.archivetune.compat.AppContext
import moe.rukamori.archivetune.constants.AccountEmailKey
import moe.rukamori.archivetune.constants.AccountNameKey
import moe.rukamori.archivetune.utils.dataStore
import moe.rukamori.archivetune.utils.toPlaybackAuthState

/** Account YouTube collegato (nome ed email), o null se non si è fatto il login. */
data class AccountSummary(
    val name: String,
    val email: String?,
)

@Composable
fun rememberAccount(): AccountSummary? {
    val flow =
        remember {
            AppContext.dataStore.data
                .map { prefs ->
                    if (!prefs.toPlaybackAuthState().hasLoginCookie) {
                        null
                    } else {
                        AccountSummary(
                            name = prefs[AccountNameKey]?.takeIf(String::isNotBlank) ?: tr("Account YouTube"),
                            email = prefs[AccountEmailKey],
                        )
                    }
                }.distinctUntilChanged()
        }
    val account by flow.collectAsState(null)
    return account
}

/** Pulsante tondo in alto a destra: iniziale del nome, o l'icona se non si è collegati. */
@Composable
fun AccountButton(
    account: AccountSummary?,
    onClick: () -> Unit,
) {
    IconButton(onClick = onClick) {
        Box(
            Modifier.size(36.dp).clip(CircleShape).background(
                if (account != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
            ),
            contentAlignment = Alignment.Center,
        ) {
            if (account != null) {
                Text(
                    account.name.first().uppercase(),
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                )
            } else {
                AtxIcon(Res.drawable.person, tr("Account"), size = 22.dp, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
