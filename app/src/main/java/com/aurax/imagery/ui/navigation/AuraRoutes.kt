package com.aurax.imagery.ui.navigation

import android.net.Uri
import com.aurax.imagery.domain.model.Album
import com.aurax.imagery.domain.model.AlbumRef
import com.aurax.imagery.ui.gallery.MediaSource

/** Route patterns and builders for screens that are not bottom-bar tabs. */
object AuraRoutes {
    const val ALBUM_PREFIX = "album/"
    const val ALBUM_PATTERN = "album/{kind}/{id}?name={name}"

    const val VIEWER_PREFIX = "viewer/"
    const val VIEWER_PATTERN = "viewer/{kind}/{albumId}/{mediaId}"

    const val GLASS_STUDIO = "glass-studio"

    const val EDITOR_PREFIX = "editor/"
    const val EDITOR_PATTERN = "editor/{mediaId}"

    const val ARG_KIND = "kind"
    const val ARG_ID = "id"
    const val ARG_NAME = "name"
    const val ARG_ALBUM_ID = "albumId"
    const val ARG_MEDIA_ID = "mediaId"

    private const val KIND_ALL = "all"
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

    /** Opens the full-screen viewer on [mediaId], paging through the items of [source]. */
    fun viewer(source: MediaSource, mediaId: Long): String = when (source) {
        MediaSource.All -> "viewer/$KIND_ALL/0/$mediaId"
        is MediaSource.Album -> when (val ref = source.ref) {
            is AlbumRef.Bucket -> "viewer/$KIND_BUCKET/${ref.id}/$mediaId"
            AlbumRef.Favorites -> "viewer/$KIND_FAVORITES/0/$mediaId"
            AlbumRef.Videos -> "viewer/$KIND_VIDEOS/0/$mediaId"
        }
    }

    fun mediaSource(kind: String, albumId: Long): MediaSource =
        if (kind == KIND_ALL) MediaSource.All else MediaSource.Album(albumRef(kind, albumId))

    fun isAlbumDetail(route: String?): Boolean = route?.startsWith(ALBUM_PREFIX) == true

    fun editor(mediaId: Long): String = "editor/$mediaId"

    fun isViewer(route: String?): Boolean = route?.startsWith(VIEWER_PREFIX) == true

    fun isEditor(route: String?): Boolean = route?.startsWith(EDITOR_PREFIX) == true

    /** Full-screen destinations draw their own chrome, so the floating tab bar is hidden there. */
    fun hidesBottomBar(route: String?): Boolean = isViewer(route) || isEditor(route) || route == GLASS_STUDIO
}
