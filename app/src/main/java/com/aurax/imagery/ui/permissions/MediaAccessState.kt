package com.aurax.imagery.ui.permissions

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.aurax.imagery.utils.MediaAccess
import com.aurax.imagery.utils.MediaPermissions

/**
 * What the UI needs to know about media permission, and how to change it.
 *
 * @property askedThisSession true once the system dialog has been shown and answered. If
 * access is still [MediaAccess.None] after that, Android may no longer show the dialog, so the
 * UI offers "Open settings" instead of only asking again.
 */
@Stable
class MediaAccessState(
    val access: MediaAccess,
    val askedThisSession: Boolean,
    val request: () -> Unit,
    val openSettings: () -> Unit,
)

/**
 * Tracks the current media access, re-checking whenever the app returns to the foreground
 * (the user may have changed it in system settings). [onAccessChanged] fires on every change,
 * including after the first grant, so the caller can re-index.
 */
@Composable
fun rememberMediaAccessState(onAccessChanged: (MediaAccess) -> Unit = {}): MediaAccessState {
    val context = LocalContext.current
    val currentOnChanged by rememberUpdatedState(onAccessChanged)
    var access by remember { mutableStateOf(MediaPermissions.currentAccess(context)) }
    var askedThisSession by remember { mutableStateOf(false) }

    fun refresh() {
        val latest = MediaPermissions.currentAccess(context)
        if (latest != access) {
            access = latest
            currentOnChanged(latest)
        }
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        askedThisSession = true
        refresh()
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    return remember(access, askedThisSession) {
        MediaAccessState(
            access = access,
            askedThisSession = askedThisSession,
            request = { launcher.launch(MediaPermissions.REQUIRED) },
            openSettings = { openAppSettings(context) },
        )
    }
}

private fun openAppSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(intent)
}
