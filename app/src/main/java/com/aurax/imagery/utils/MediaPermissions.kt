package com.aurax.imagery.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager

/** How much of the library the user has allowed the app to see. */
enum class MediaAccess {
    /** Whole library (images and/or videos). */
    Full,

    /** Android 14+ "Select photos and videos": only items the user picked. */
    Partial,

    /** Nothing. The app must not read media and must not keep an index. */
    None,
}

object MediaPermissions {
    /**
     * Request all three together: the system dialog then offers "Allow all" or "Select
     * photos and videos" and returns whichever the user chose.
     */
    val REQUIRED: Array<String> = arrayOf(
        Manifest.permission.READ_MEDIA_IMAGES,
        Manifest.permission.READ_MEDIA_VIDEO,
        Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
    )

    fun currentAccess(context: Context): MediaAccess {
        fun granted(permission: String) =
            context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED

        return when {
            granted(Manifest.permission.READ_MEDIA_IMAGES) ||
                granted(Manifest.permission.READ_MEDIA_VIDEO) -> MediaAccess.Full
            granted(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED) -> MediaAccess.Partial
            else -> MediaAccess.None
        }
    }
}
