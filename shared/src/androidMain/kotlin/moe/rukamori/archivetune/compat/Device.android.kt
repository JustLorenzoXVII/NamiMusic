package moe.rukamori.archivetune.compat

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.justlorenzo.archivetunex.player.AndroidAppContext
import kotlin.math.abs

actual object BatteryState {
    private fun intent(): android.content.Intent? =
        AndroidAppContext.context.registerReceiver(null, android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED))

    actual fun level(): Int {
        val intent = intent() ?: return -1
        val level = intent.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, -1)
        return if (level >= 0 && scale > 0) level * 100 / scale else -1
    }

    actual fun isCharging(): Boolean {
        val status = intent()?.getIntExtra(android.os.BatteryManager.EXTRA_STATUS, -1) ?: return false
        return status == android.os.BatteryManager.BATTERY_STATUS_CHARGING || status == android.os.BatteryManager.BATTERY_STATUS_FULL
    }
}

private val sensorManager: SensorManager?
    get() = AndroidAppContext.context.getSystemService(android.content.Context.SENSOR_SERVICE) as? SensorManager

actual fun observeProximity(onCovered: (Boolean) -> Unit): () -> Unit {
    val manager = sensorManager ?: return {}
    val sensor = manager.getDefaultSensor(Sensor.TYPE_PROXIMITY) ?: return {}
    val listener =
        object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val distance = event.values.getOrNull(0) ?: return
                val maxRange = sensor.maximumRange
                onCovered(maxRange > 0f && distance <= (maxRange * 0.1f).coerceAtLeast(1.0f))
            }

            override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
        }
    manager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_NORMAL)
    return { manager.unregisterListener(listener) }
}

actual fun observeShake(onShake: () -> Unit): () -> Unit {
    val manager = sensorManager ?: return {}
    val accelerometer = manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) ?: return {}
    var hasBaseline = false
    var lastShakeTime = 0L
    var lastX = 0f
    var lastY = 0f
    var lastZ = 0f
    val listener =
        object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]
                if (!hasBaseline) {
                    lastX = x
                    lastY = y
                    lastZ = z
                    hasBaseline = true
                    return
                }
                val delta = abs(x - lastX) + abs(y - lastY) + abs(z - lastZ)
                lastX = x
                lastY = y
                lastZ = z
                val now = java.lang.System.currentTimeMillis()
                if (delta > 18f && now - lastShakeTime > 1000L) {
                    lastShakeTime = now
                    onShake()
                }
            }

            override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
        }
    manager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_UI)
    return { manager.unregisterListener(listener) }
}
