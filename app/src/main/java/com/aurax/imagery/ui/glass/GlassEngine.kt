package com.aurax.imagery.ui.glass

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleResumeEffect
import dev.chrisbanes.haze.HazeState
import kotlin.math.roundToInt

/**
 * The HazeState of the nearest enclosing backdrop. Whatever is marked `hazeSource` with this
 * state is what glass panels blur. Null means "no backdrop here", and panels fall back to a
 * plain translucent fill.
 */
val LocalHazeState = staticCompositionLocalOf<HazeState?> { null }

/** Device tilt in -1..1 on both axes, or null when motion effects are off or unavailable. */
val LocalGlassTilt = staticCompositionLocalOf<State<Offset>?> { null }

/** True when the system has animations switched off (Settings > Accessibility > Remove animations). */
@Composable
fun rememberReducedMotion(): Boolean {
    val context = LocalContext.current
    return remember {
        runCatching {
            Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
        }.getOrDefault(false)
    }
}

/**
 * Low-pass-filtered accelerometer tilt, listened to only while the screen is resumed and only
 * when [enabled]. Values are quantised so a still phone does not trigger constant redraws.
 * Returns null when disabled or when the device has no accelerometer.
 */
@Composable
fun rememberGlassTilt(enabled: Boolean): State<Offset>? {
    val context = LocalContext.current
    val tilt = remember { mutableStateOf(Offset.Zero) }
    if (!enabled) return null
    val sensorManager = remember { context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager }
    val accelerometer = remember(sensorManager) { sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) }
    if (sensorManager == null || accelerometer == null) return null

    LifecycleResumeEffect(sensorManager) {
        var fx = 0f
        var fy = 0f
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                // x grows when the right edge tilts up, y when the top tilts away; normalise by g.
                val x = (-event.values[0] / 9.81f).coerceIn(-1f, 1f)
                val y = (event.values[1] / 9.81f).coerceIn(-1f, 1f)
                fx += (x - fx) * 0.15f
                fy += (y - fy) * 0.15f
                val q = Offset(quantise(fx), quantise(fy))
                if (q != tilt.value) tilt.value = q
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_UI)
        onPauseOrDispose { sensorManager.unregisterListener(listener) }
    }
    return tilt
}

private fun quantise(v: Float): Float = (v * 25f).roundToInt() / 25f
