package com.aurax.imagery.data.repository

import com.aurax.imagery.data.local.database.AlbumRow
import com.aurax.imagery.data.local.database.MediaDao
import com.aurax.imagery.data.local.database.MediaRow
import com.aurax.imagery.domain.model.Album
import com.aurax.imagery.domain.model.AlbumRef
import com.aurax.imagery.domain.model.LibraryStats
import com.aurax.imagery.domain.model.MediaItem
import com.aurax.imagery.domain.model.MediaType
import com.aurax.imagery.domain.repository.MediaRepository
import com.aurax.imagery.domain.repository.PreferencesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

/** [MediaRepository] backed by the Room index. Mapping runs off the main thread. */
class MediaRepositoryImpl(
    private val dao: MediaDao,
    private val preferences: PreferencesRepository,
) : MediaRepository {

    override fun observeMedia(): Flow<List<MediaItem>> = dao.observeMedia().toDomain()

    override fun observeAlbumMedia(ref: AlbumRef): Flow<List<MediaItem>> = when (ref) {
        is AlbumRef.Bucket -> dao.observeBucketMedia(ref.id)
        AlbumRef.Favorites -> dao.observeFavorites()
        AlbumRef.Videos -> dao.observeVideos()
        AlbumRef.RecentlyAdded -> dao.observeRecentlyAdded(recentSinceMs())
        AlbumRef.RecentlyViewed -> recentlyViewedRows()
    }.toDomain()

    private fun recentSinceMs() = System.currentTimeMillis() - RECENT_DAYS * 24L * 60L * 60L * 1000L

    /** Rows for the ids the viewer recorded, in the order they were viewed. */
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private fun recentlyViewedRows(): Flow<List<MediaRow>> =
        preferences.recentViewedIds.flatMapLatest { ids ->
            if (ids.isEmpty()) {
                flowOf(emptyList())
            } else {
                dao.observeByIds(ids).map { rows ->
                    val byId = rows.associateBy { it.media.id }
                    ids.mapNotNull { byId[it] }
                }
            }
        }

    override fun observeAlbums(): Flow<List<Album>> =
        combine(
            dao.observeFavoritesSummary(),
            dao.observeVideosSummary(),
            dao.observeAlbums(),
            dao.observeRecentlyAdded(recentSinceMs()),
            recentlyViewedRows(),
        ) { favorites, videos, buckets, added, viewed ->
            buildList {
                if (favorites.itemCount > 0) {
                    add(Album(AlbumRef.Favorites, "Favorites", favorites.coverUri, favorites.itemCount))
                }
                if (videos.itemCount > 0) {
                    add(Album(AlbumRef.Videos, "Videos", videos.coverUri, videos.itemCount))
                }
                if (added.isNotEmpty()) {
                    add(Album(AlbumRef.RecentlyAdded, "Recently added", added.first().media.uri, added.size))
                }
                if (viewed.isNotEmpty()) {
                    add(Album(AlbumRef.RecentlyViewed, "Recently viewed", viewed.first().media.uri, viewed.size))
                }
                // Well-known folders first (stable sort keeps the rest alphabetical).
                addAll(buckets.map(AlbumRow::toDomain).sortedBy { folderPriority(it.name) })
            }
        }.flowOn(Dispatchers.Default)

    override fun observeStats(): Flow<LibraryStats> = dao.observeStats().map {
        LibraryStats(total = it.total, videos = it.videos, photos = it.total - it.videos, bytes = it.bytes)
    }

    override suspend fun clearFavorites() = dao.clearFavorites()

    override suspend fun setFavorites(mediaIds: List<Long>, favorite: Boolean) {
        if (mediaIds.isEmpty()) return
        dao.setFavorites(mediaIds, favorite, System.currentTimeMillis())
    }

    private fun Flow<List<MediaRow>>.toDomain(): Flow<List<MediaItem>> =
        map { rows -> rows.map(MediaRow::toDomain) }.flowOn(Dispatchers.Default)

    private companion object {
        const val RECENT_DAYS = 30L
    }

    private fun folderPriority(name: String): Int =
        when (name.lowercase()) {
            "camera" -> 0
            "screenshots" -> 1
            "download", "downloads" -> 2
            else -> 3
        }
}

internal fun MediaRow.toDomain() = MediaItem(
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
