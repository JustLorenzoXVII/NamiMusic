package moe.rukamori.archivetune.compat

import android.app.Activity
import android.view.WindowManager
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import java.lang.ref.WeakReference

actual object SystemBars {
    private var activityRef: WeakReference<Activity>? = null

    /** Chiamato da MainActivity di Android all'avvio. */
    fun attach(activity: Activity) {
        activityRef = WeakReference(activity)
        WindowCompat.setDecorFitsSystemWindows(activity.window, false)
    }

    private fun withActivity(block: (Activity) -> Unit) {
        activityRef?.get()?.let { activity -> activity.runOnUiThread { block(activity) } }
    }

    actual fun setDarkContent(dark: Boolean) =
        withActivity { activity ->
            val controller = WindowCompat.getInsetsController(activity.window, activity.window.decorView)
            controller.isAppearanceLightStatusBars = dark
            controller.isAppearanceLightNavigationBars = dark
        }

    actual fun setStatusBarsHidden(hidden: Boolean) =
        withActivity { activity ->
            val controller = WindowCompat.getInsetsController(activity.window, activity.window.decorView)
            if (hidden) {
                controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                controller.hide(WindowInsetsCompat.Type.statusBars())
            } else {
                controller.show(WindowInsetsCompat.Type.statusBars())
            }
        }

    actual fun setImmersive(enabled: Boolean) =
        withActivity { activity ->
            val controller = WindowCompat.getInsetsController(activity.window, activity.window.decorView)
            if (enabled) {
                controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                controller.hide(WindowInsetsCompat.Type.systemBars())
                activity.setShowWhenLocked(true)
                activity.setTurnScreenOn(true)
            } else {
                controller.show(WindowInsetsCompat.Type.systemBars())
                controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_DEFAULT
                activity.setShowWhenLocked(false)
                activity.setTurnScreenOn(false)
            }
        }

    actual fun setKeepScreenOn(enabled: Boolean) =
        withActivity { activity ->
            if (enabled) {
                activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            } else {
                activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }

    actual fun setSecure(enabled: Boolean) =
        withActivity { activity ->
            if (enabled) {
                activity.window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
            } else {
                activity.window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            }
        }
}
