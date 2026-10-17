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

internal actual fun platformSetClipboardText(text: String) {
    val manager = AndroidAppContext.context.getSystemService(android.content.ClipboardManager::class.java) ?: return
    manager.setPrimaryClip(android.content.ClipData.newPlainText("text", text))
}

internal actual fun platformGetClipboardText(): String? {
    val manager = AndroidAppContext.context.getSystemService(android.content.ClipboardManager::class.java) ?: return null
    return manager.primaryClip?.getItemAt(0)?.text?.toString()
}

internal actual fun platformPerformHaptic(feedbackConstant: Int) {
    AndroidAppContext.activity?.get()?.window?.decorView?.performHapticFeedback(feedbackConstant)
}

private val mainHandler by lazy { android.os.Handler(Looper.getMainLooper()) }

internal actual fun platformRunOnMain(action: () -> Unit) {
    if (isMainThread()) action() else mainHandler.post(action)
}

internal actual fun platformRunOnMainDelayed(
    action: () -> Unit,
    delayMillis: Long,
) {
    mainHandler.postDelayed(action, delayMillis)
}

internal actual fun platformSetKeepScreenOn(keepOn: Boolean) {
    platformRunOnMain {
        val window = AndroidAppContext.activity?.get()?.window ?: return@platformRunOnMain
        if (keepOn) {
            window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
}

actual fun platformSetScreenBrightness(level: Float?) {
    platformRunOnMain {
        val window = AndroidAppContext.activity?.get()?.window ?: return@platformRunOnMain
        val lp = window.attributes
        lp.screenBrightness = level ?: android.view.WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
        window.attributes = lp
    }
}

actual fun openSystemEqualizerPanel(audioSessionId: Int) {
    val activity = AndroidAppContext.activity?.get() ?: return
    val intent =
        android.content.Intent(android.media.audiofx.AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL).apply {
            putExtra(android.media.audiofx.AudioEffect.EXTRA_AUDIO_SESSION, audioSessionId)
            putExtra(android.media.audiofx.AudioEffect.EXTRA_PACKAGE_NAME, activity.packageName)
            putExtra(android.media.audiofx.AudioEffect.EXTRA_CONTENT_TYPE, android.media.audiofx.AudioEffect.CONTENT_TYPE_MUSIC)
        }
    if (intent.resolveActivity(activity.packageManager) != null) {
        runCatching { activity.startActivityForResult(intent, 0) }
    }
}

actual object DeviceVolume {
    private val audioManager: android.media.AudioManager
        get() = AndroidAppContext.context.getSystemService(android.content.Context.AUDIO_SERVICE) as android.media.AudioManager

    private fun minVolume(): Int =
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) audioManager.getStreamMinVolume(android.media.AudioManager.STREAM_MUSIC) else 0

    actual fun fraction(): Float {
        val min = minVolume()
        val max = audioManager.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC).coerceAtLeast(min + 1)
        val volume = audioManager.getStreamVolume(android.media.AudioManager.STREAM_MUSIC)
        return ((volume - min).toFloat() / (max - min).toFloat()).coerceIn(0f, 1f)
    }

    actual fun setFraction(fraction: Float) {
        val min = minVolume()
        val max = audioManager.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC).coerceAtLeast(min + 1)
        val safe = fraction.takeIf(Float::isFinite)?.coerceIn(0f, 1f) ?: return
        val volume = (min + ((max - min) * safe).toInt()).coerceIn(min, max)
        audioManager.setStreamVolume(android.media.AudioManager.STREAM_MUSIC, volume, 0)
    }

    actual fun isMuted(): Boolean = audioManager.getStreamVolume(android.media.AudioManager.STREAM_MUSIC) <= minVolume()
}

internal actual fun platformHasPermission(permission: String): Boolean =
    androidx.core.content.ContextCompat.checkSelfPermission(AndroidAppContext.context, permission) ==
        android.content.pm.PackageManager.PERMISSION_GRANTED

actual val Context.platformContext: coil3.PlatformContext
    get() = AndroidAppContext.context

internal actual fun platformSecureRandomBytes(bytes: ByteArray) {
    java.security.SecureRandom().nextBytes(bytes)
}

internal actual fun platformUsableSpace(path: String): Long = java.io.File(path).usableSpace

internal actual fun platformTotalSpace(path: String): Long = java.io.File(path).totalSpace

internal actual fun platformAdjustStreamVolume(
    streamType: Int,
    direction: Int,
    flags: Int,
) {
    val context = com.justlorenzo.archivetunex.player.AndroidAppContext.context
    val audioManager = context.getSystemService(android.content.Context.AUDIO_SERVICE) as? android.media.AudioManager ?: return
    audioManager.adjustStreamVolume(streamType, direction, flags)
}

internal actual fun platformIs24HourFormat(): Boolean =
    android.text.format.DateFormat.is24HourFormat(com.justlorenzo.archivetunex.player.AndroidAppContext.context)

internal actual fun platformRestartApp() {
    val context = com.justlorenzo.archivetunex.player.AndroidAppContext.context
    val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
    if (launchIntent != null) {
        launchIntent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK)
        context.startActivity(launchIntent)
    }
}
