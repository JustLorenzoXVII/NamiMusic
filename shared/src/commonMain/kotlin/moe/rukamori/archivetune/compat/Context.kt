package moe.rukamori.archivetune.compat

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.PluralStringResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getPluralString

/**
 * Sostituto multipiattaforma di android.content.Context per il codice di ArchiveTune:
 * stringhe, cartelle dell'app, nome del pacchetto. C'è un solo contesto, [AppContext].
 */
abstract class Context {
    open val applicationContext: Context
        get() = AppContext

    val packageName: String
        get() = PACKAGE_NAME

    /** Cartella dei file privati dell'app (come Context.filesDir). */
    val filesDir: File
        get() = File(platformFilesDirectory())

    /** Cartella della cache (come Context.cacheDir). */
    val cacheDir: File
        get() = File(platformCacheDirectory())

    fun getString(resource: StringResource): String = runBlocking { org.jetbrains.compose.resources.getString(resource) }

    fun getString(
        resource: StringResource,
        vararg formatArgs: Any,
    ): String = runBlocking { org.jetbrains.compose.resources.getString(resource, *formatArgs) }

    fun getQuantityString(
        resource: PluralStringResource,
        quantity: Int,
        vararg formatArgs: Any,
    ): String = runBlocking { getPluralString(resource, quantity, *formatArgs) }

    companion object {
        const val PACKAGE_NAME = "com.justlorenzo.archivetunex"
        const val CLIPBOARD_SERVICE = "clipboard"
        const val AUDIO_SERVICE = "audio"
        const val MODE_PRIVATE = 0
    }
}

/** L'unico contesto dell'app. */
object AppContext : Activity() {
    override val applicationContext: Context
        get() = this
}

/** Come androidx.compose.ui.platform.LocalContext. */
val LocalContext: ProvidableCompositionLocal<Context> = staticCompositionLocalOf { AppContext }

/** Come android.widget.Toast: su Android usa il Toast di sistema, su iPhone un avviso in basso nell'app. */
class Toast private constructor(
    private val text: String,
    private val duration: Int,
) {
    fun show() {
        if (!showPlatformToast(text, duration == LENGTH_LONG)) {
            ToastCenter.post(text, duration == LENGTH_LONG)
        }
    }

    companion object {
        const val LENGTH_SHORT = 0
        const val LENGTH_LONG = 1

        fun makeText(
            context: Context,
            text: CharSequence,
            duration: Int,
        ): Toast = Toast(text.toString(), duration)

        fun makeText(
            context: Context,
            resource: StringResource,
            duration: Int,
        ): Toast = Toast(context.getString(resource), duration)
    }
}

/** Avvisi da mostrare con [ToastHost] sulle piattaforme senza Toast di sistema. */
object ToastCenter {
    data class Message(
        val text: String,
        val long: Boolean,
    )

    private val _messages = MutableSharedFlow<Message>(extraBufferCapacity = 16)
    val messages: SharedFlow<Message> = _messages.asSharedFlow()

    fun post(
        text: String,
        long: Boolean,
    ) {
        _messages.tryEmit(Message(text, long))
    }
}

/** Mostra gli avvisi di [Toast] in basso, sopra l'interfaccia (serve su iPhone). */
@Composable
fun ToastHost(modifier: Modifier = Modifier) {
    var current by remember { mutableStateOf<ToastCenter.Message?>(null) }
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        ToastCenter.messages.collect { message ->
            current = message
            visible = true
            delay(if (message.long) 3500 else 2000)
            visible = false
            delay(250)
        }
    }
    Box(modifier.fillMaxSize().navigationBarsPadding().padding(bottom = 96.dp), contentAlignment = Alignment.BottomCenter) {
        AnimatedVisibility(visible = visible, enter = fadeIn(), exit = fadeOut()) {
            Text(
                text = current?.text.orEmpty(),
                color = MaterialTheme.colorScheme.inverseOnSurface,
                style = MaterialTheme.typography.bodyMedium,
                modifier =
                    Modifier
                        .padding(horizontal = 24.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.inverseSurface)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
            )
        }
    }
}

/** true se è il thread principale (come Looper.getMainLooper().thread == Thread.currentThread()). */
expect fun isMainThread(): Boolean

internal expect fun platformFilesDirectory(): String

internal expect fun platformCacheDirectory(): String

/** Su Android mostra il Toast di sistema e restituisce true; su iPhone false (lo mostra [ToastHost]). */
internal expect fun showPlatformToast(
    text: String,
    long: Boolean,
): Boolean

/** Context.getDatabasePath: il file del database dell'app. */
fun Context.getDatabasePath(name: String): File = File(moe.rukamori.archivetune.db.platformDatabasePath(name))

/** File.resolveSibling di Kotlin/JVM. */
fun File.resolveSibling(name: String): File = File(parentFile, name)

/** Il contesto della piattaforma richiesto da Coil (Context Android / PlatformContext iOS). */
expect val Context.platformContext: coil3.PlatformContext
