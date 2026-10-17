package moe.rukamori.archivetune.compat

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSDate
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSFileModificationDate
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSThread
import platform.Foundation.NSUserDomainMask
import platform.Foundation.timeIntervalSince1970
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSLocale
import platform.Foundation.currentLocale
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.outputVolume

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

private var savedScreenBrightness: Double? = null

actual fun platformSetScreenBrightness(level: Float?) {
    platformRunOnMain {
        val screen = platform.UIKit.UIScreen.mainScreen
        if (level == null) {
            savedScreenBrightness?.let { screen.brightness = it }
            savedScreenBrightness = null
        } else {
            if (savedScreenBrightness == null) savedScreenBrightness = screen.brightness
            screen.brightness = level.toDouble()
        }
    }
}

actual fun openSystemEqualizerPanel(audioSessionId: Int) = Unit

actual object DeviceVolume {
    actual fun fraction(): Float = AVAudioSession.sharedInstance().outputVolume

    actual fun setFraction(fraction: Float) = Unit

    actual fun isMuted(): Boolean = fraction() <= 0f
}

internal actual fun platformHasPermission(permission: String): Boolean = true

actual val Context.platformContext: coil3.PlatformContext
    get() = coil3.PlatformContext.INSTANCE

@OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
internal actual fun platformSecureRandomBytes(bytes: ByteArray) {
    if (bytes.isEmpty()) return
    bytes.usePinned { pinned ->
        platform.Security.SecRandomCopyBytes(platform.Security.kSecRandomDefault, bytes.size.toULong(), pinned.addressOf(0))
    }
}

@OptIn(ExperimentalForeignApi::class)
internal actual fun platformUsableSpace(path: String): Long {
    val attributes = NSFileManager.defaultManager.attributesOfFileSystemForPath(path, null) ?: return 0L
    return (attributes[platform.Foundation.NSFileSystemFreeSize] as? platform.Foundation.NSNumber)?.longLongValue ?: 0L
}

@OptIn(ExperimentalForeignApi::class)
internal actual fun platformTotalSpace(path: String): Long {
    val attributes = NSFileManager.defaultManager.attributesOfFileSystemForPath(path, null) ?: return 0L
    return (attributes[platform.Foundation.NSFileSystemSize] as? platform.Foundation.NSNumber)?.longLongValue ?: 0L
}

internal actual fun platformAdjustStreamVolume(
    streamType: Int,
    direction: Int,
    flags: Int,
) = Unit

internal actual fun platformIs24HourFormat(): Boolean {
    val format =
        NSDateFormatter.dateFormatFromTemplate("j", 0u, NSLocale.currentLocale)
            ?: return true
    return !format.contains('a')
}

/** Su iPhone un'app non può riavviarsi da sola: dopo il ripristino si chiude e va riaperta. */
internal actual fun platformRestartApp() = Unit

actual fun platformMaxRefreshRate(): Float? =
    platform.UIKit.UIScreen.mainScreen.maximumFramesPerSecond.toFloat()
