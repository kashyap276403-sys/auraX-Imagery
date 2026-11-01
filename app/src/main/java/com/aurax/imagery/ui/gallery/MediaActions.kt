package com.aurax.imagery.ui.gallery

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.aurax.imagery.domain.model.MediaItem

/**
 * Hands a photo to the user's installed editor. Read access only: editors save a copy, so the
 * original is never overwritten. Replaced by the built-in editor in Phase 5.
 */
fun editExternally(context: Context, item: MediaItem) {
    val intent = Intent(Intent.ACTION_EDIT)
        .setDataAndType(Uri.parse(item.uri), item.mimeType)
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    try {
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, "No editor app available", Toast.LENGTH_SHORT).show()
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
