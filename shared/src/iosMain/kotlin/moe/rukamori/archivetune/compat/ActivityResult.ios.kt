package moe.rukamori.archivetune.compat

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/**
 * Su iPhone i permessi Android non esistono (si considerano concessi); per i file
 * il selettore di documenti verrà collegato insieme a backup e import.
 */
@Suppress("UNCHECKED_CAST")
@Composable
actual fun <I, O> rememberLauncherForActivityResult(
    contract: ActivityResultContract<I, O>,
    onResult: (O) -> Unit,
): ManagedActivityResultLauncher<I, O> =
    remember(contract) {
        ManagedActivityResultLauncher { input ->
            val result: Any? =
                when (contract) {
                    is ActivityResultContracts.RequestPermission -> true
                    is ActivityResultContracts.RequestMultiplePermissions -> (input as Array<String>).associateWith { true }
                    is ActivityResultContracts.OpenMultipleDocuments -> emptyList<Uri>()
                    is ActivityResultContracts.StartActivityForResult -> ActivityResult(ActivityResult.RESULT_CANCELED, null)
                    else -> null
                }
            onResult(result as O)
        }
    }
