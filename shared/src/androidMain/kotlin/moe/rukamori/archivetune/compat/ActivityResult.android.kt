package moe.rukamori.archivetune.compat

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.activity.compose.rememberLauncherForActivityResult as androidRememberLauncher
import androidx.activity.result.contract.ActivityResultContracts as AndroidContracts

private fun android.net.Uri.toCompat(): Uri = Uri.parse(toString())

private fun Uri.toAndroid(): android.net.Uri = android.net.Uri.parse(toString())

@Suppress("UNCHECKED_CAST")
@Composable
actual fun <I, O> rememberLauncherForActivityResult(
    contract: ActivityResultContract<I, O>,
    onResult: (O) -> Unit,
): ManagedActivityResultLauncher<I, O> =
    when (contract) {
        is ActivityResultContracts.OpenDocument -> {
            val launcher = androidRememberLauncher(AndroidContracts.OpenDocument()) { uri -> onResult(uri?.toCompat() as O) }
            remember(launcher) { ManagedActivityResultLauncher { input -> launcher.launch(input as Array<String>) } }
        }

        is ActivityResultContracts.OpenMultipleDocuments -> {
            val launcher = androidRememberLauncher(AndroidContracts.OpenMultipleDocuments()) { uris -> onResult(uris.map { it.toCompat() } as O) }
            remember(launcher) { ManagedActivityResultLauncher { input -> launcher.launch(input as Array<String>) } }
        }

        is ActivityResultContracts.CreateDocument -> {
            val launcher = androidRememberLauncher(AndroidContracts.CreateDocument(contract.mimeType)) { uri -> onResult(uri?.toCompat() as O) }
            remember(launcher) { ManagedActivityResultLauncher { input -> launcher.launch(input as String) } }
        }

        is ActivityResultContracts.OpenDocumentTree -> {
            val launcher = androidRememberLauncher(AndroidContracts.OpenDocumentTree()) { uri -> onResult(uri?.toCompat() as O) }
            remember(launcher) { ManagedActivityResultLauncher { input -> launcher.launch((input as Uri?)?.toAndroid()) } }
        }

        is ActivityResultContracts.GetContent -> {
            val launcher = androidRememberLauncher(AndroidContracts.GetContent()) { uri -> onResult(uri?.toCompat() as O) }
            remember(launcher) { ManagedActivityResultLauncher { input -> launcher.launch(input as String) } }
        }

        is ActivityResultContracts.RequestPermission -> {
            val launcher = androidRememberLauncher(AndroidContracts.RequestPermission()) { granted -> onResult(granted as O) }
            remember(launcher) { ManagedActivityResultLauncher { input -> launcher.launch(input as String) } }
        }

        is ActivityResultContracts.RequestMultiplePermissions -> {
            val launcher = androidRememberLauncher(AndroidContracts.RequestMultiplePermissions()) { result -> onResult(result as O) }
            remember(launcher) { ManagedActivityResultLauncher { input -> launcher.launch(input as Array<String>) } }
        }

        is ActivityResultContracts.StartActivityForResult -> {
            val launcher =
                androidRememberLauncher(AndroidContracts.StartActivityForResult()) { result ->
                    onResult(ActivityResult(result.resultCode, null) as O)
                }
            remember(launcher) { ManagedActivityResultLauncher { input -> launcher.launch((input as Intent).toAndroidIntent()) } }
        }

        else -> remember { ManagedActivityResultLauncher { } }
    }
