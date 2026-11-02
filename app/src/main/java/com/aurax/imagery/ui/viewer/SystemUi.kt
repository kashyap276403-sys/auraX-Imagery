package com.aurax.imagery.ui.viewer

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

fun Context.findActivity(): Activity? {
    var current: Context = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}

/**
 * Hides the status and navigation bars while [hideSystemBars] is true (swiping from the edge
 * reveals them briefly). Bars are always restored when the viewer leaves composition.
 */
@Composable
fun ImmersiveEffect(hideSystemBars: Boolean) {
    val view = LocalView.current
    DisposableEffect(hideSystemBars) {
        val window = view.context.findActivity()?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        controller?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        // Let the picture run under a camera cutout in landscape instead of leaving a black strip beside it.
        val oldCutout = window?.attributes?.layoutInDisplayCutoutMode
        window?.attributes = window?.attributes?.also {
            it.layoutInDisplayCutoutMode = android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
        }
        if (hideSystemBars) {
            controller?.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            controller?.show(WindowInsetsCompat.Type.systemBars())
        }
        onDispose {
            controller?.show(WindowInsetsCompat.Type.systemBars())
            if (oldCutout != null) window?.attributes = window.attributes.also { it.layoutInDisplayCutoutMode = oldCutout }
        }
    }
}

/** Holds the screen orientation per [lock]; releases it when leaving the viewer. */
@Composable
fun OrientationLockEffect(lock: com.aurax.imagery.domain.player.OrientationLock) {
    val view = LocalView.current
    DisposableEffect(lock) {
        val activity = view.context.findActivity()
        activity?.requestedOrientation = when (lock) {
            com.aurax.imagery.domain.player.OrientationLock.Auto -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            com.aurax.imagery.domain.player.OrientationLock.Portrait -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            com.aurax.imagery.domain.player.OrientationLock.Landscape -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        }
        onDispose { }
    }
    DisposableEffect(Unit) {
        val activity = view.context.findActivity()
        onDispose { activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED }
    }
}

/** Puts the window brightness back to what it was when the viewer opened (gesture changes are temporary). */
@Composable
fun BrightnessRestoreEffect() {
    val view = LocalView.current
    DisposableEffect(Unit) {
        val window = view.context.findActivity()?.window
        val original = window?.attributes?.screenBrightness
        onDispose {
            if (window != null && original != null) {
                window.attributes = window.attributes.also { it.screenBrightness = original }
            }
        }
    }
}
