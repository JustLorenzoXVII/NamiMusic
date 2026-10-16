package moe.rukamori.archivetune.compat

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo

/** android.content.res.Configuration: orientamento e dimensioni dello schermo in dp. */
class Configuration(
    val screenWidthDp: Int,
    val screenHeightDp: Int,
    val uiMode: Int = UI_MODE_TYPE_NORMAL,
) {
    val orientation: Int
        get() = if (screenWidthDp > screenHeightDp) ORIENTATION_LANDSCAPE else ORIENTATION_PORTRAIT

    val smallestScreenWidthDp: Int
        get() = minOf(screenWidthDp, screenHeightDp)

    val locales: List<Locale>
        get() = listOf(Locale.getDefault())

    companion object {
        const val ORIENTATION_UNDEFINED = 0
        const val ORIENTATION_PORTRAIT = 1
        const val ORIENTATION_LANDSCAPE = 2
        const val UI_MODE_TYPE_MASK = 0x0f
        const val UI_MODE_TYPE_NORMAL = 0x01
        const val UI_MODE_TYPE_TELEVISION = 0x04
        const val UI_MODE_NIGHT_MASK = 0x30
        const val UI_MODE_NIGHT_YES = 0x20
    }
}

/**
 * Come androidx.compose.ui.platform.LocalConfiguration: qui si legge con [LocalConfiguration].current
 * e viene calcolata dalla finestra corrente.
 */
object LocalConfiguration {
    val current: Configuration
        @OptIn(ExperimentalComposeUiApi::class)
        @Composable
        get() {
            val size = LocalWindowInfo.current.containerSize
            val density = LocalDensity.current.density
            return Configuration(
                screenWidthDp = (size.width / density).toInt(),
                screenHeightDp = (size.height / density).toInt(),
            )
        }
}

/** android.view.HapticFeedbackConstants. */
object HapticFeedbackConstants {
    const val LONG_PRESS = 0
    const val VIRTUAL_KEY = 1
    const val KEYBOARD_TAP = 3
    const val KEYBOARD_PRESS = 3
    const val CLOCK_TICK = 4
    const val CONTEXT_CLICK = 6
    const val KEYBOARD_RELEASE = 7
    const val VIRTUAL_KEY_RELEASE = 8
    const val TEXT_HANDLE_MOVE = 9
    const val GESTURE_START = 12
    const val GESTURE_END = 13
    const val CONFIRM = 16
    const val REJECT = 17
    const val TOGGLE_ON = 21
    const val TOGGLE_OFF = 22
    const val GESTURE_THRESHOLD_ACTIVATE = 23
    const val GESTURE_THRESHOLD_DEACTIVATE = 24
    const val DRAG_START = 25
    const val SEGMENT_TICK = 26
    const val SEGMENT_FREQUENT_TICK = 27
}

/** android.view.View, per quanto serve all'interfaccia: vibrazione e post. */
class View internal constructor() {
    var isHapticFeedbackEnabled: Boolean = true

    fun performHapticFeedback(feedbackConstant: Int): Boolean {
        if (!isHapticFeedbackEnabled) return false
        platformPerformHaptic(feedbackConstant)
        return true
    }

    fun performHapticFeedback(
        feedbackConstant: Int,
        @Suppress("UNUSED_PARAMETER") flags: Int,
    ): Boolean = performHapticFeedback(feedbackConstant)

    fun post(action: () -> Unit): Boolean {
        platformRunOnMain(action)
        return true
    }

    fun postDelayed(
        action: () -> Unit,
        delayMillis: Long,
    ): Boolean {
        platformRunOnMainDelayed(action, delayMillis)
        return true
    }

    var keepScreenOn: Boolean = false
        set(value) {
            field = value
            platformSetKeepScreenOn(value)
        }

    val context: Context
        get() = AppContext
}

private val sharedView = View()

/** Come androidx.compose.ui.platform.LocalView. */
val LocalView: ProvidableCompositionLocal<View> = staticCompositionLocalOf { sharedView }

/** android.view.ViewConfiguration. */
object ViewConfiguration {
    fun getDoubleTapTimeout(): Int = 300

    fun getLongPressTimeout(): Int = 400

    fun getTapTimeout(): Int = 100
}

/** android.view.WindowManager (solo i flag usati: schermo sempre acceso). */
object WindowManager {
    object LayoutParams {
        const val FLAG_KEEP_SCREEN_ON = 0x00000080
        const val FLAG_SECURE = 0x00002000
    }
}

/** La finestra dell'app (android.view.Window). */
class Window internal constructor() {
    fun addFlags(flags: Int) {
        if (flags and WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON != 0) platformSetKeepScreenOn(true)
    }

    fun clearFlags(flags: Int) {
        if (flags and WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON != 0) platformSetKeepScreenOn(false)
    }
}

/** android.app.Activity: l'unica attività è l'app stessa ([AppContext]). */
abstract class Activity : Context() {
    val window: Window = Window()

    fun runOnUiThread(action: () -> Unit) = platformRunOnMain(action)
}

/** java.text.BreakIterator.getCharacterInstance: divide il testo in caratteri visibili (grafemi). */
class BreakIterator private constructor() {
    private var text: String = ""
    private var boundaries: List<Int> = emptyList()
    private var index = 0

    fun setText(newText: String) {
        text = newText
        boundaries = graphemeBoundaries(newText)
        index = 0
    }

    fun first(): Int {
        index = 0
        return boundaries.firstOrNull() ?: 0
    }

    fun next(): Int {
        if (index + 1 >= boundaries.size) return DONE
        index++
        return boundaries[index]
    }

    fun current(): Int = boundaries.getOrElse(index) { DONE }

    fun last(): Int {
        index = boundaries.lastIndex
        return boundaries.lastOrNull() ?: 0
    }

    companion object {
        const val DONE = -1

        fun getCharacterInstance(): BreakIterator = BreakIterator()

        fun getCharacterInstance(
            @Suppress("UNUSED_PARAMETER") locale: Locale,
        ): BreakIterator = BreakIterator()

        fun getWordInstance(
            @Suppress("UNUSED_PARAMETER") locale: Locale = Locale.ROOT,
        ): BreakIterator = BreakIterator()

        /** Approssimazione dei grafemi: tiene insieme surrogati, segni combinanti, ZWJ e selettori di variante. */
        private fun graphemeBoundaries(text: String): List<Int> {
            val result = mutableListOf(0)
            var i = 0
            while (i < text.length) {
                var next = i + if (text[i].isHighSurrogate() && i + 1 < text.length) 2 else 1
                while (next < text.length) {
                    val c = text[next]
                    val combining =
                        c in '̀'..'ͯ' || c in '︀'..'️' || c == '‍' ||
                            c in '᪰'..'᫿' || c in '⃐'..'⃿' ||
                            (c.isLowSurrogate()) ||
                            (text[next - 1] == '‍')
                    if (!combining) break
                    next += if (c.isHighSurrogate() && next + 1 < text.length) 2 else 1
                }
                result += next
                i = next
            }
            return result
        }
    }
}

internal expect fun platformPerformHaptic(feedbackConstant: Int)

internal expect fun platformRunOnMain(action: () -> Unit)

internal expect fun platformRunOnMainDelayed(
    action: () -> Unit,
    delayMillis: Long,
)

internal expect fun platformSetKeepScreenOn(keepOn: Boolean)

/** Apre il pannello dell'equalizzatore di sistema (solo Android). */
expect fun openSystemEqualizerPanel(audioSessionId: Int)
