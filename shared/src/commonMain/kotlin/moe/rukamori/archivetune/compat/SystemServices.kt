package moe.rukamori.archivetune.compat

import kotlin.reflect.KClass

/** android.content.ClipData (solo testo e URI). */
class ClipData private constructor(
    val label: CharSequence?,
    val text: CharSequence?,
    val uri: Uri?,
) {
    companion object {
        fun newPlainText(
            label: CharSequence?,
            text: CharSequence?,
        ): ClipData = ClipData(label, text, null)

        fun newRawUri(
            label: CharSequence?,
            uri: Uri,
        ): ClipData = ClipData(label, null, uri)
    }
}

/** android.content.ClipboardManager: appunti di sistema. */
class ClipboardManager internal constructor() {
    fun setPrimaryClip(clip: ClipData) {
        platformSetClipboardText(clip.text?.toString() ?: clip.uri?.toString().orEmpty())
    }

    val primaryClipText: String?
        get() = platformGetClipboardText()
}

internal expect fun platformSetClipboardText(text: String)

internal expect fun platformGetClipboardText(): String?

private val clipboardManager = ClipboardManager()

/** Nomi dei servizi di sistema (Context.CLIPBOARD_SERVICE...). */
fun Context.getSystemService(name: String): Any? =
    when (name) {
        Context.CLIPBOARD_SERVICE -> clipboardManager
        Context.AUDIO_SERVICE -> AudioManager
        else -> null
    }

@Suppress("UNCHECKED_CAST")
fun <T : Any> Context.getSystemService(serviceClass: KClass<T>): T? =
    when (serviceClass) {
        ClipboardManager::class -> clipboardManager as T
        AudioManager::class -> AudioManager as T
        else -> null
    }

inline fun <reified T : Any> Context.getSystemService(): T? = getSystemService(T::class)


/** android.media.AudioManager: solo il volume, usato dai gesti dello schermo AOD. */
object AudioManager {
    const val STREAM_MUSIC = 3
    const val ADJUST_LOWER = -1
    const val ADJUST_SAME = 0
    const val ADJUST_RAISE = 1
    const val FLAG_SHOW_UI = 1

    fun adjustStreamVolume(
        streamType: Int,
        direction: Int,
        flags: Int,
    ) = platformAdjustStreamVolume(streamType, direction, flags)
}

/** Su iPhone le app non possono cambiare il volume di sistema: non fa nulla. */
internal expect fun platformAdjustStreamVolume(
    streamType: Int,
    direction: Int,
    flags: Int,
)

/** android.text.format.DateFormat: solo is24HourFormat. */
object DateFormat {
    fun is24HourFormat(context: Context): Boolean = platformIs24HourFormat()
}

internal expect fun platformIs24HourFormat(): Boolean
