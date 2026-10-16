package moe.rukamori.archivetune.compat

import platform.Foundation.NSURL
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIDevice
import platform.UIKit.UIViewController
import platform.UIKit.UIWindow
import platform.UIKit.UIWindowScene

internal actual fun platformStartActivity(intent: Intent) {
    val target = intent.chooserTarget ?: intent
    when (target.action) {
        Intent.ACTION_SEND, Intent.ACTION_SEND_MULTIPLE -> {
            val items = mutableListOf<Any>()
            target.extras.getString(Intent.EXTRA_TEXT)?.let { items += it }
            target.extras.getString(Intent.EXTRA_STREAM)?.let { path ->
                val url = if (path.startsWith("/")) NSURL.fileURLWithPath(path) else NSURL.URLWithString(path)
                url?.let { items += it }
            }
            if (items.isEmpty()) return
            val controller = UIActivityViewController(activityItems = items, applicationActivities = null)
            topViewController()?.presentViewController(controller, animated = true, completion = null)
        }

        Intent.ACTION_WEB_SEARCH -> {
            val query = target.extras.getString("query").orEmpty()
            openUrl("https://www.google.com/search?q=" + Uri.encode(query))
        }

        else -> target.data?.let { openUrl(it.toString()) }
    }
}

private fun openUrl(url: String) {
    val nsUrl = NSURL.URLWithString(url) ?: return
    UIApplication.sharedApplication.openURL(nsUrl, options = emptyMap<Any?, Any>(), completionHandler = null)
}

/** Il controller in primo piano, per presentare fogli di condivisione e simili. */
fun topViewController(): UIViewController? {
    val window =
        UIApplication.sharedApplication.connectedScenes
            .filterIsInstance<UIWindowScene>()
            .flatMap { scene -> scene.windows.filterIsInstance<UIWindow>() }
            .firstOrNull { it.isKeyWindow() }
            ?: UIApplication.sharedApplication.keyWindow
    var controller = window?.rootViewController
    while (controller?.presentedViewController != null) {
        controller = controller.presentedViewController
    }
    return controller
}

internal actual fun platformSdkInt(): Int = 0

internal actual fun platformOsVersion(): String = UIDevice.currentDevice.systemVersion

internal actual fun platformManufacturer(): String = "Apple"

internal actual fun platformModel(): String = UIDevice.currentDevice.model
