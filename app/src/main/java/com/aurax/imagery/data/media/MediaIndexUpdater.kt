package com.aurax.imagery.data.media

import android.content.Context
import com.aurax.imagery.data.local.database.MediaDao
import com.aurax.imagery.utils.MediaAccess
import com.aurax.imagery.utils.MediaPermissions

/**
 * One sync pass between MediaStore and the Room index.
 *
 * Privacy rule: without any media permission the index is emptied instead of kept, so the app
 * never retains file names or paths of media it is no longer authorised to see.
 */
class MediaIndexUpdater(
    private val context: Context,
    private val dao: MediaDao,
    private val scanner: MediaStoreScanner,
) {
    suspend fun sync() {
        when (MediaPermissions.currentAccess(context)) {
            MediaAccess.None -> dao.clearMedia()
            MediaAccess.Full, MediaAccess.Partial -> dao.replaceIndex(scanner.scan())
        }
    }
}
