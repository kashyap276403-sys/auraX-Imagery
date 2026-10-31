package com.aurax.imagery.ui.gallery

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.aurax.imagery.domain.model.MediaItem

/**
 * Interim "open": hands the file to the user's default viewer. Replaced by the in-app
 * immersive viewer in Phase 4.
 */
fun openExternally(context: Context, item: MediaItem) {
    val intent = Intent(Intent.ACTION_VIEW)
        .setDataAndType(Uri.parse(item.uri), item.mimeType)
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    try {
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, "No app available to open this file", Toast.LENGTH_SHORT).show()
    }
}

/** Shares the given items through the system share sheet. Always user-initiated. */
fun shareItems(context: Context, items: List<MediaItem>) {
    if (items.isEmpty()) return
    val uris = ArrayList(items.map { Uri.parse(it.uri) })
    val kinds = items.map { it.mimeType.substringBefore('/') }.distinct()
    val type = if (kinds.size == 1) "${kinds.first()}/*" else "*/*"

    val intent = if (uris.size == 1) {
        Intent(Intent.ACTION_SEND).putExtra(Intent.EXTRA_STREAM, uris.first())
    } else {
        Intent(Intent.ACTION_SEND_MULTIPLE).putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
    }
    intent.type = type
    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

    try {
        context.startActivity(Intent.createChooser(intent, null))
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, "Nothing available to share with", Toast.LENGTH_SHORT).show()
    }
}
