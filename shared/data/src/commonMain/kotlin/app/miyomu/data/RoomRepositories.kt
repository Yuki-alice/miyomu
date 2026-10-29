package app.miyomu.data

import app.miyomu.data.db.LibraryItemEntity
import app.miyomu.data.db.MiyomuDao
import app.miyomu.data.db.ReadingProgressEntity
import app.miyomu.model.LibraryItem
import app.miyomu.model.LibraryStatus
import app.miyomu.model.MediaType
import app.miyomu.model.ProgressKind
import app.miyomu.model.ReadingProgress
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class RoomLibraryRepository(
    private val dao: MiyomuDao,
) : LibraryRepository {

    override fun observeLibrary(): Flow<List<LibraryItem>> =
        dao.observeLibrary().map { rows -> rows.map(LibraryItemEntity::toModel) }

    override suspend fun get(id: String): LibraryItem? =
        dao.getById(id)?.toModel()

    override suspend fun upsert(item: LibraryItem) {
        dao.upsertLibrary(item.toEntity())
    }

    override suspend fun delete(id: String) {
        dao.deleteLibrary(id)
    }

    override suspend fun markViewed(id: String, viewedAt: Long) {
        dao.markViewed(id, viewedAt)
    }
}

internal class RoomProgressRepository(
    private val dao: MiyomuDao,
) : ProgressRepository {

    override fun observeLatest(itemId: String): Flow<ReadingProgress?> =
        dao.observeLatestProgress(itemId).map { rows -> rows.firstOrNull()?.toModel() }

    override suspend fun latest(itemId: String): ReadingProgress? =
        dao.latestProgress(itemId)?.toModel()

    override suspend fun latestFor(itemId: String, entryId: String): ReadingProgress? =
        dao.progressForEntry(itemId, entryId)?.toModel()

    override suspend fun all(itemId: String): List<ReadingProgress> =
        dao.allProgress(itemId).map { it.toModel() }

    override suspend fun save(progress: ReadingProgress) {
        dao.upsertProgress(progress.toEntity())
    }

    override suspend fun clear(itemId: String) {
        dao.clearProgress(itemId)
    }
}

private fun LibraryItem.toEntity(): LibraryItemEntity = LibraryItemEntity(
    id = id,
    mediaType = mediaType.name,
    sourceId = sourceId,
    sourceKey = sourceKey,
    title = title,
    coverUrl = coverUrl,
    author = author,
    description = description,
    status = status.name,
    addedAt = addedAt,
    updatedAt = updatedAt,
    lastViewedAt = lastViewedAt,
)

private fun LibraryItemEntity.toModel(): LibraryItem = LibraryItem(
    id = id,
    mediaType = MediaType.valueOf(mediaType),
    sourceId = sourceId,
    sourceKey = sourceKey,
    title = title,
    coverUrl = coverUrl,
    author = author,
    description = description,
    status = LibraryStatus.valueOf(status),
    addedAt = addedAt,
    updatedAt = updatedAt,
    lastViewedAt = lastViewedAt,
)

private fun ReadingProgress.toEntity(): ReadingProgressEntity = ReadingProgressEntity(
    itemId = itemId,
    entryId = entryId,
    kind = kind.name,
    position = position,
    duration = duration,
    updatedAt = updatedAt,
    deviceId = deviceId,
)

private fun ReadingProgressEntity.toModel(): ReadingProgress = ReadingProgress(
    itemId = itemId,
    entryId = entryId,
    kind = ProgressKind.valueOf(kind),
    position = position,
    duration = duration,
    updatedAt = updatedAt,
    deviceId = deviceId,
)
