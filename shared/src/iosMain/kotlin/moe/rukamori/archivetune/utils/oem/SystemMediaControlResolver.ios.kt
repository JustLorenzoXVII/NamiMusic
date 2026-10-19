package moe.rukamori.archivetune.utils.oem

import kotlinx.cinterop.ExperimentalForeignApi
import moe.rukamori.archivetune.compat.Context
import platform.AVKit.AVRoutePickerView
import platform.CoreGraphics.CGRectMake
import platform.UIKit.UIApplication
import platform.UIKit.UIButton
import platform.UIKit.UIControlEventTouchUpInside

/**
 * Su iPhone "Trasmetti" apre il menu AirPlay di sistema (altoparlanti, AirPods, Apple TV…):
 * si crea un AVRoutePickerView invisibile e si "tocca" il suo pulsante.
 */
@OptIn(ExperimentalForeignApi::class)
actual object SystemMediaControlResolver {
    private var picker: AVRoutePickerView? = null

    actual fun openMediaOutputSwitcher(context: Context) {
        val window = UIApplication.sharedApplication.keyWindow ?: return
        val view = picker ?: AVRoutePickerView(frame = CGRectMake(0.0, 0.0, 1.0, 1.0)).also {
            it.setAlpha(0.01)
            window.addSubview(it)
            picker = it
        }
        view.subviews
            .firstOrNull { it is UIButton }
            ?.let { (it as UIButton).sendActionsForControlEvents(UIControlEventTouchUpInside) }
    }
}
