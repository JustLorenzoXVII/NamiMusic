package moe.rukamori.archivetune.compat

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSDate
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSFileModificationDate
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSThread
import platform.Foundation.NSUserDomainMask
import platform.Foundation.timeIntervalSince1970

actual fun isMainThread(): Boolean = NSThread.isMainThread

internal actual fun platformFilesDirectory(): String =
    (NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, true).first() as String)

internal actual fun platformCacheDirectory(): String =
    (NSSearchPathForDirectoriesInDomains(NSCachesDirectory, NSUserDomainMask, true).first() as String)

internal actual fun showPlatformToast(
    text: String,
    long: Boolean,
): Boolean = false

@OptIn(ExperimentalForeignApi::class)
internal actual fun platformLastModified(path: String): Long {
    val attributes = NSFileManager.defaultManager.attributesOfItemAtPath(path, null) ?: return 0L
    val date = attributes[NSFileModificationDate] as? NSDate ?: return 0L
    return (date.timeIntervalSince1970 * 1000).toLong()
}

internal actual fun platformSetClipboardText(text: String) {
    platform.UIKit.UIPasteboard.generalPasteboard.string = text
}

internal actual fun platformGetClipboardText(): String? = platform.UIKit.UIPasteboard.generalPasteboard.string

internal actual fun platformPerformHaptic(feedbackConstant: Int) {
    val style =
        when (feedbackConstant) {
            HapticFeedbackConstants.LONG_PRESS, HapticFeedbackConstants.CONFIRM, HapticFeedbackConstants.REJECT ->
                platform.UIKit.UIImpactFeedbackStyle.UIImpactFeedbackStyleMedium
            else -> platform.UIKit.UIImpactFeedbackStyle.UIImpactFeedbackStyleLight
        }
    platform.UIKit.UIImpactFeedbackGenerator(style = style).impactOccurred()
}

internal actual fun platformRunOnMain(action: () -> Unit) {
    if (NSThread.isMainThread) {
        action()
    } else {
        platform.darwin.dispatch_async(platform.darwin.dispatch_get_main_queue()) { action() }
    }
}

internal actual fun platformRunOnMainDelayed(
    action: () -> Unit,
    delayMillis: Long,
) {
    platform.darwin.dispatch_after(
        platform.darwin.dispatch_time(platform.darwin.DISPATCH_TIME_NOW, delayMillis * 1_000_000),
        platform.darwin.dispatch_get_main_queue(),
    ) { action() }
}

internal actual fun platformSetKeepScreenOn(keepOn: Boolean) {
    platformRunOnMain { platform.UIKit.UIApplication.sharedApplication.idleTimerDisabled = keepOn }
}

actual fun openSystemEqualizerPanel(audioSessionId: Int) = Unit
