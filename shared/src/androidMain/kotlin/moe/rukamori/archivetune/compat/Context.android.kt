package moe.rukamori.archivetune.compat

import android.os.Looper
import com.justlorenzo.archivetunex.player.AndroidAppContext

actual fun isMainThread(): Boolean = Looper.getMainLooper().thread == Thread.currentThread()

internal actual fun platformFilesDirectory(): String = AndroidAppContext.context.filesDir.absolutePath

internal actual fun platformCacheDirectory(): String = AndroidAppContext.context.cacheDir.absolutePath

internal actual fun showPlatformToast(
    text: String,
    long: Boolean,
): Boolean {
    val show = {
        android.widget.Toast
            .makeText(AndroidAppContext.context, text, if (long) android.widget.Toast.LENGTH_LONG else android.widget.Toast.LENGTH_SHORT)
            .show()
    }
    if (isMainThread()) show() else android.os.Handler(Looper.getMainLooper()).post(show)
    return true
}

internal actual fun platformLastModified(path: String): Long = java.io.File(path).lastModified()

/** Il Context di Android vero, per il codice che ne ha bisogno (notifiche, file, intent). */
val Context.androidContext: android.content.Context
    get() = AndroidAppContext.context
