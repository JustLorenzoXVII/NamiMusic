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
