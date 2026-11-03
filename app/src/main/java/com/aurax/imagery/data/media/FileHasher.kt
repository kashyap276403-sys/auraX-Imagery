package com.aurax.imagery.data.media

import android.content.Context
import android.net.Uri
import java.security.MessageDigest

/** SHA-256 of a media file's bytes, streamed so large videos never sit in memory. Null if unreadable. */
object FileHasher {
    fun sha256(context: Context, uri: String): String? = runCatching {
        val digest = MessageDigest.getInstance("SHA-256")
        context.contentResolver.openInputStream(Uri.parse(uri))?.use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val n = input.read(buffer)
                if (n < 0) break
                digest.update(buffer, 0, n)
            }
        } ?: return null
        digest.digest().joinToString("") { "%02x".format(it) }
    }.getOrNull()
}
