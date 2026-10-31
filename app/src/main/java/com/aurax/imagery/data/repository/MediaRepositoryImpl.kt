package com.aurax.imagery.data.repository

import com.aurax.imagery.data.local.database.AlbumRow
import com.aurax.imagery.data.local.database.FavoriteEntity
import com.aurax.imagery.data.local.database.MediaDao
import com.aurax.imagery.data.local.database.MediaRow
import com.aurax.imagery.domain.model.Album
import com.aurax.imagery.domain.model.MediaItem
import com.aurax.imagery.domain.model.MediaType
import com.aurax.imagery.domain.repository.MediaRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

/** [MediaRepository] backed by the Room index. Mapping runs off the main thread. */
class MediaRepositoryImpl(private val dao: MediaDao) : MediaRepository {

    override fun observeMedia(): Flow<List<MediaItem>> =
        dao.observeMedia().map { rows -> rows.map(MediaRow::toDomain) }.flowOn(Dispatchers.Default)

    override fun observeFavorites(): Flow<List<MediaItem>> =
        dao.observeFavorites().map { rows -> rows.map(MediaRow::toDomain) }.flowOn(Dispatchers.Default)

    override fun observeAlbums(): Flow<List<Album>> =
        dao.observeAlbums().map { rows -> rows.map(AlbumRow::toDomain) }.flowOn(Dispatchers.Default)

    override suspend fun setFavorite(mediaId: Long, favorite: Boolean) {
        if (favorite) {
            dao.addFavorite(FavoriteEntity(mediaId = mediaId, addedAt = System.currentTimeMillis()))
        } else {
            dao.removeFavorite(mediaId)
        }
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
    id = id,
    name = name,
    coverUri = coverUri,
    itemCount = itemCount,
)
