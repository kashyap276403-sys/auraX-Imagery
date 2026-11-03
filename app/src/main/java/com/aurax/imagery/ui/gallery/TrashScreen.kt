package com.aurax.imagery.ui.gallery

import android.app.Activity
import android.content.ContentUris
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.RestoreFromTrash
import androidx.compose.material.icons.rounded.SelectAll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.aurax.imagery.AuraXApp
import com.aurax.imagery.ui.components.DropIconButton
import com.aurax.imagery.ui.components.EmptyState
import com.aurax.imagery.ui.components.GlassAlertDialog
import com.aurax.imagery.ui.components.LoadingState
import com.aurax.imagery.ui.components.ScreenScaffold
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** One file in the system trash. */
private data class TrashedItem(val id: Long, val uri: Uri, val name: String, val isVideo: Boolean, val expiresAtSec: Long)

/**
 * The system trash for this app's media. Deleted photos and videos stay here for about 30 days (the
 * system decides the exact time) and can be restored. "Delete forever" asks the system for
 * confirmation too; nothing is removed without it.
 */
@Composable
fun TrashScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val container = remember { (context.applicationContext as AuraXApp).container }
    val scope = rememberCoroutineScope()

    var items by remember { mutableStateOf<List<TrashedItem>?>(null) }
    var selected by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var reload by remember { mutableIntStateOf(0) }
    var confirmForever by remember { mutableStateOf(false) }
    var pendingMessage by remember { mutableStateOf("") }

    LaunchedEffect(reload) {
        items = withContext(Dispatchers.IO) { queryTrash(context) }
        selected = selected.intersect((items ?: emptyList()).map { it.id }.toSet())
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            Toast.makeText(context, pendingMessage, Toast.LENGTH_SHORT).show()
            selected = emptySet()
            reload++
            container.indexer.enqueue() // restored files must come back into the gallery
        }
    }

    fun launch(request: android.app.PendingIntent, message: String) {
        pendingMessage = message
        launcher.launch(IntentSenderRequest.Builder(request.intentSender).build())
    }

    val shown = items
    val targets = shown.orEmpty().filter { it.id in selected }

    ScreenScaffold(
        title = "Trash",
        subtitle = shown?.let { if (selected.isEmpty()) "${it.size} items" else "${selected.size} selected" },
        onBack = onBack,
        actions = {
            if (!shown.isNullOrEmpty()) {
                DropIconButton(onClick = { selected = shown.map { it.id }.toSet() }) {
                    Icon(Icons.Rounded.SelectAll, contentDescription = "Select all")
                }
            }
        },
    ) {
        when {
            shown == null -> LoadingState()
            shown.isEmpty() -> EmptyState(
                icon = Icons.Rounded.DeleteSweep,
                title = "Trash is empty",
                message = "Deleted photos and videos wait here for about 30 days before the system removes them.",
            )
            else -> Column(Modifier.fillMaxSize()) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    items(items = shown, key = { it.id }) { item ->
                        val on = item.id in selected
                        Box(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clip(androidx.compose.foundation.shape.RoundedCornerShape(6.dp))
                                .background(Color(0xFF111114))
                                .combinedClickable(onClick = {
                                    selected = if (on) selected - item.id else selected + item.id
                                }),
                        ) {
                            AsyncImage(
                                model = item.uri,
                                contentDescription = item.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                            )
                            if (on) {
                                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f)))
                                Icon(
                                    Icons.Rounded.CheckCircle,
                                    contentDescription = "Selected",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.align(Alignment.TopEnd).padding(6.dp).size(22.dp),
                                )
                            }
                            daysLeft(item.expiresAtSec)?.let { days ->
                                Text(
                                    text = if (days <= 0) "<1d" else "${days}d",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .padding(4.dp)
                                        .clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.55f))
                                        .padding(horizontal = 6.dp, vertical = 1.dp),
                                )
                            }
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                ) {
                    TextButton(
                        enabled = targets.isNotEmpty(),
                        onClick = {
                            runCatching {
                                launch(
                                    MediaStore.createTrashRequest(context.contentResolver, targets.map { it.uri }, false),
                                    if (targets.size == 1) "Restored" else "Restored ${targets.size} items",
                                )
                            }.onFailure { Toast.makeText(context, "Couldn't restore", Toast.LENGTH_SHORT).show() }
                        },
                    ) {
                        Icon(Icons.Rounded.RestoreFromTrash, contentDescription = null)
                        Text("  Restore")
                    }
                    TextButton(enabled = targets.isNotEmpty(), onClick = { confirmForever = true }) {
                        Icon(Icons.Rounded.Delete, contentDescription = null)
                        Text("  Delete forever")
                    }
                }
            }
        }
    }

    if (confirmForever) {
        GlassAlertDialog(
            onDismissRequest = { confirmForever = false },
            title = { Text("Delete forever?") },
            text = { Text("${targets.size} selected item(s) will be removed from this device. This can't be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmForever = false
                    runCatching {
                        launch(
                            MediaStore.createDeleteRequest(context.contentResolver, targets.map { it.uri }),
                            "Deleted",
                        )
                    }.onFailure { Toast.makeText(context, "Couldn't delete", Toast.LENGTH_SHORT).show() }
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { confirmForever = false }) { Text("Cancel") } },
        )
    }
}

private fun daysLeft(expiresAtSec: Long): Long? {
    if (expiresAtSec <= 0L) return null
    return (expiresAtSec - System.currentTimeMillis() / 1000L) / 86_400L
}

private fun queryTrash(context: android.content.Context): List<TrashedItem> {
    val resolver = context.contentResolver
    val files = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
    val args = Bundle().apply {
        putInt(MediaStore.QUERY_ARG_MATCH_TRASHED, MediaStore.MATCH_ONLY)
        putString(
            android.content.ContentResolver.QUERY_ARG_SQL_SELECTION,
            "${MediaStore.Files.FileColumns.MEDIA_TYPE} IN (?, ?)",
        )
        putStringArray(
            android.content.ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS,
            arrayOf(
                MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString(),
                MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString(),
            ),
        )
        putString(android.content.ContentResolver.QUERY_ARG_SQL_SORT_ORDER, "${MediaStore.MediaColumns.DATE_EXPIRES} ASC")
    }
    val projection = arrayOf(
        MediaStore.Files.FileColumns._ID,
        MediaStore.Files.FileColumns.DISPLAY_NAME,
        MediaStore.Files.FileColumns.MEDIA_TYPE,
        MediaStore.MediaColumns.DATE_EXPIRES,
    )
    val out = ArrayList<TrashedItem>()
    runCatching {
        resolver.query(files, projection, args, null)?.use { c ->
            while (c.moveToNext()) {
                val id = c.getLong(0)
                val video = c.getInt(2) == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO
                val base = if (video) MediaStore.Video.Media.EXTERNAL_CONTENT_URI else MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                out += TrashedItem(id, ContentUris.withAppendedId(base, id), c.getString(1).orEmpty(), video, c.getLong(3))
            }
        }
    }
    return out
}
