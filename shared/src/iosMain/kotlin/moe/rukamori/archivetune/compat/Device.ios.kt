package moe.rukamori.archivetune.compat

import platform.UIKit.UIDevice
import platform.UIKit.UIDeviceBatteryState

actual object BatteryState {
    actual fun level(): Int {
        UIDevice.currentDevice.batteryMonitoringEnabled = true
        val level = UIDevice.currentDevice.batteryLevel
        return if (level < 0f) -1 else (level * 100).toInt()
    }

    actual fun isCharging(): Boolean {
        UIDevice.currentDevice.batteryMonitoringEnabled = true
        val state = UIDevice.currentDevice.batteryState
        return state == UIDeviceBatteryState.UIDeviceBatteryStateCharging || state == UIDeviceBatteryState.UIDeviceBatteryStateFull
    }
}

/** Su iPhone la prossimità spegne già lo schermo da sola: non serve. */
actual fun observeProximity(onCovered: (Boolean) -> Unit): () -> Unit = {}

actual fun observeShake(onShake: () -> Unit): () -> Unit = {}
