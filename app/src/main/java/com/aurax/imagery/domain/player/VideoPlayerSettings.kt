package com.aurax.imagery.domain.player

/** Player preferences edited in Settings > Video player (gestures and resume are stored beside these). */
data class VideoPlayerSettings(
    val defaultSpeed: Float = 1f,
    val defaultScreenMode: ScreenMode = ScreenMode.Fit,
    val rememberScreenMode: Boolean = true,
    val seekIntervalSec: Int = 10,
    /** Seconds before the controls fade while playing; 0 keeps them on screen. */
    val autoHideSec: Int = 4,
    val autoPlayNext: Boolean = true,
    val subtitlesOnByDefault: Boolean = false,
    val orientation: OrientationLock = OrientationLock.Auto,
    val haptics: Boolean = true,
) {
    /** Snaps stored values to the choices the UI offers, so a bad value can never reach the player. */
    fun sanitized(): VideoPlayerSettings = copy(
        defaultSpeed = PLAYBACK_SPEEDS.minByOrNull { kotlin.math.abs(it - defaultSpeed) } ?: 1f,
        seekIntervalSec = SEEK_INTERVALS_SEC.minByOrNull { kotlin.math.abs(it - seekIntervalSec) } ?: 10,
        autoHideSec = if (autoHideSec in AUTO_HIDE_OPTIONS_SEC) autoHideSec else 4,
    )
}

val SEEK_INTERVALS_SEC = listOf(5, 10, 30)
val AUTO_HIDE_OPTIONS_SEC = listOf(2, 4, 8, 0)

fun autoHideLabel(seconds: Int): String = if (seconds == 0) "Never" else "$seconds s"
