package com.aurax.imagery.ui.viewer

import android.app.Activity
import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.util.Rational
import com.aurax.imagery.domain.player.pipAspect

const val PIP_ACTION_TOGGLE = "com.aurax.imagery.PIP_TOGGLE_PLAYBACK"

/**
 * Picture-in-Picture parameters: the video's shape, one play/pause button, and (while a video is
 * playing) automatic entry when the user leaves the app.
 */
fun pipParams(context: Context, videoWidth: Float, videoHeight: Float, playing: Boolean, autoEnter: Boolean): PictureInPictureParams {
    val aspect = pipAspect(videoWidth, videoHeight)
    val intent = PendingIntent.getBroadcast(
        context,
        0,
        Intent(PIP_ACTION_TOGGLE).setPackage(context.packageName),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )
    val icon = Icon.createWithResource(
        context,
        if (playing) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
    )
    val title = if (playing) "Pause" else "Play"
    return PictureInPictureParams.Builder()
        .setAspectRatio(Rational((aspect * 1000).toInt(), 1000))
        .setActions(listOf(RemoteAction(icon, title, title, intent)))
        .setAutoEnterEnabled(autoEnter)
        .build()
}

/** Shrinks the player into the floating window. Returns false if the system refused. */
fun enterPip(activity: Activity, params: PictureInPictureParams): Boolean =
    runCatching { activity.enterPictureInPictureMode(params) }.getOrDefault(false)

/** Safe wrapper: some devices or modes refuse PiP params. */
fun setPipParams(activity: Activity, params: PictureInPictureParams) {
    runCatching { activity.setPictureInPictureParams(params) }
}
