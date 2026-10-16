package moe.rukamori.archivetune.compat

import androidx.compose.runtime.Composable

/** androidx.activity.result: contratti per scegliere file, chiedere permessi, aprire schermate. */
abstract class ActivityResultContract<I, O>

class ActivityResult(
    val resultCode: Int,
    val data: Intent?,
) {
    companion object {
        const val RESULT_OK = -1
        const val RESULT_CANCELED = 0
    }
}

object ActivityResultContracts {
    class OpenDocument : ActivityResultContract<Array<String>, Uri?>()

    class OpenMultipleDocuments : ActivityResultContract<Array<String>, List<Uri>>()

    class CreateDocument(
        val mimeType: String = "*/*",
    ) : ActivityResultContract<String, Uri?>()

    class OpenDocumentTree : ActivityResultContract<Uri?, Uri?>()

    class GetContent : ActivityResultContract<String, Uri?>()

    class RequestPermission : ActivityResultContract<String, Boolean>()

    class RequestMultiplePermissions : ActivityResultContract<Array<String>, Map<String, Boolean>>()

    class StartActivityForResult : ActivityResultContract<Intent, ActivityResult>()
}

class ManagedActivityResultLauncher<I, O>(
    private val onLaunch: (I) -> Unit,
) {
    fun launch(input: I) = onLaunch(input)

    fun unregister() = Unit
}

/** Come androidx.activity.compose.rememberLauncherForActivityResult. */
@Composable
expect fun <I, O> rememberLauncherForActivityResult(
    contract: ActivityResultContract<I, O>,
    onResult: (O) -> Unit,
): ManagedActivityResultLauncher<I, O>
