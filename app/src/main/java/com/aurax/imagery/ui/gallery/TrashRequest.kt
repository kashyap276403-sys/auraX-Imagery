package com.aurax.imagery.ui.gallery

import android.app.Activity
import android.net.Uri
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.aurax.imagery.domain.model.MediaItem

/**
 * "Delete" for gallery items, done the scoped-storage way: the system shows its own
 * confirmation ("Allow AuraX to move N items to the trash?"), and only an approved request
 * moves anything. Items go to the system trash, where they stay recoverable for about 30 days.
 *
 * [onTrashed] runs with the items after the user approves. Returns a function to call with the
 * items to trash.
 */
@Composable
fun rememberTrashAction(onTrashed: (List<MediaItem>) -> Unit): (List<MediaItem>) -> Unit {
    val context = LocalContext.current
    val currentOnTrashed by rememberUpdatedState(onTrashed)
    var pending by remember { mutableStateOf<List<MediaItem>>(emptyList()) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        val items = pending
        pending = emptyList()
        if (result.resultCode == Activity.RESULT_OK && items.isNotEmpty()) {
            Toast.makeText(
                context,
                if (items.size == 1) "Moved to trash" else "Moved ${items.size} items to trash",
                Toast.LENGTH_SHORT,
            ).show()
            currentOnTrashed(items)
        }
    }

    return remember(launcher) {
        { items: List<MediaItem> ->
            if (items.isNotEmpty()) {
                pending = items
                try {
                    val request = MediaStore.createTrashRequest(
                        context.contentResolver,
                        items.map { Uri.parse(it.uri) },
                        true,
                    )
                    launcher.launch(IntentSenderRequest.Builder(request.intentSender).build())
                } catch (e: Exception) {
                    // e.g. an item is no longer in MediaStore, or access to it was revoked.
                    pending = emptyList()
                    Toast.makeText(context, "Couldn't move to trash", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
