package com.aurax.imagery.ui.navigation

import android.net.Uri
import com.aurax.imagery.domain.model.Album
import com.aurax.imagery.domain.model.AlbumRef

/** Route patterns and builders for screens that are not bottom-bar tabs. */
object AuraRoutes {
    const val ALBUM_PREFIX = "album/"
    const val ALBUM_PATTERN = "album/{kind}/{id}?name={name}"

    const val ARG_KIND = "kind"
    const val ARG_ID = "id"
    const val ARG_NAME = "name"

    private const val KIND_BUCKET = "bucket"
    private const val KIND_FAVORITES = "favorites"
    private const val KIND_VIDEOS = "videos"

    /** The album name travels as a query argument so the detail screen can title itself. */
    fun album(album: Album): String {
        val name = Uri.encode(album.name)
        return when (val ref = album.ref) {
            is AlbumRef.Bucket -> "album/$KIND_BUCKET/${ref.id}?name=$name"
            AlbumRef.Favorites -> "album/$KIND_FAVORITES/0?name=$name"
            AlbumRef.Videos -> "album/$KIND_VIDEOS/0?name=$name"
        }
    }

    fun albumRef(kind: String, id: Long): AlbumRef = when (kind) {
        KIND_FAVORITES -> AlbumRef.Favorites
        KIND_VIDEOS -> AlbumRef.Videos
        else -> AlbumRef.Bucket(id)
    }

    fun isAlbumDetail(route: String?): Boolean = route?.startsWith(ALBUM_PREFIX) == true
}
