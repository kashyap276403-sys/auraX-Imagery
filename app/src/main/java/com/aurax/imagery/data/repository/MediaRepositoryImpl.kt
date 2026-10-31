package com.aurax.imagery.data.repository

import com.aurax.imagery.data.local.database.AlbumRow
import com.aurax.imagery.data.local.database.MediaDao
import com.aurax.imagery.data.local.database.MediaRow
import com.aurax.imagery.domain.model.Album
import com.aurax.imagery.domain.model.AlbumRef
import com.aurax.imagery.domain.model.MediaItem
import com.aurax.imagery.domain.model.MediaType
import com.aurax.imagery.domain.repository.MediaRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

/** [MediaRepository] backed by the Room index. Mapping runs off the main thread. */
class MediaRepositoryImpl(private val dao: MediaDao) : MediaRepository {

    override fun observeMedia(): Flow<List<MediaItem>> = dao.observeMedia().toDomain()

    override fun observeAlbumMedia(ref: AlbumRef): Flow<List<MediaItem>> = when (ref) {
        is AlbumRef.Bucket -> dao.observeBucketMedia(ref.id)
        AlbumRef.Favorites -> dao.observeFavorites()
        AlbumRef.Videos -> dao.observeVideos()
    }.toDomain()

    override fun observeAlbums(): Flow<List<Album>> =
        combine(
            dao.observeFavoritesSummary(),
            dao.observeVideosSummary(),
            dao.observeAlbums(),
        ) { favorites, videos, buckets ->
            buildList {
                if (favorites.itemCount > 0) {
                    add(Album(AlbumRef.Favorites, "Favorites", favorites.coverUri, favorites.itemCount))
                }
                if (videos.itemCount > 0) {
                    add(Album(AlbumRef.Videos, "Videos", videos.coverUri, videos.itemCount))
                }
                // Well-known folders first (stable sort keeps the rest alphabetical).
                addAll(buckets.map(AlbumRow::toDomain).sortedBy { folderPriority(it.name) })
            }
        }.flowOn(Dispatchers.Default)

    override suspend fun setFavorites(mediaIds: List<Long>, favorite: Boolean) {
        if (mediaIds.isEmpty()) return
        dao.setFavorites(mediaIds, favorite, System.currentTimeMillis())
    }

    private fun Flow<List<MediaRow>>.toDomain(): Flow<List<MediaItem>> =
        map { rows -> rows.map(MediaRow::toDomain) }.flowOn(Dispatchers.Default)

    private fun folderPriority(name: String): Int =
        when (name.lowercase()) {
            "camera" -> 0
            "screenshots" -> 1
            "download", "downloads" -> 2
            else -> 3
        }
}

private fun MediaRow.toDomain() = MediaItem(
    id = media.id,
    uri = media.uri,
    displayName = media.displayName,
    mimeType = media.mimeType,
    type = if (media.isVideo) MediaType.Video else MediaType.Image,
    dateTakenMillis = media.dateTaken,
    sizeBytes = media.sizeBytes,
    width = media.width,
    height = media.height,
    durationMillis = if (media.isVideo) media.durationMs else null,
    albumId = media.bucketId,
    albumName = media.bucketName,
    isFavorite = isFavorite,
)

private fun AlbumRow.toDomain() = Album(
    ref = AlbumRef.Bucket(id),
    name = name,
    coverUri = coverUri,
    itemCount = itemCount,
)
