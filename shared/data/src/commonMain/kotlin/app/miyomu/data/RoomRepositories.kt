package app.miyomu.data

import app.miyomu.data.db.CategoryEntity
import app.miyomu.data.db.MangaCategoryEntity
import app.miyomu.data.db.LibraryUpdateEntity
import tachiyomi.domain.category.model.Category
import app.miyomu.contract.*
import app.miyomu.data.db.ChapterEntity
import app.miyomu.data.db.EpisodeEntity
import app.miyomu.data.db.LibraryItemEntity
import app.miyomu.data.db.MiyomuDao
import app.miyomu.data.db.ReadingProgressEntity
import app.miyomu.data.db.VideoResolveEntity
import app.miyomu.model.LibraryItem
import app.miyomu.model.LibraryStatus
import app.miyomu.model.MediaType
import app.miyomu.model.ProgressKind
import app.miyomu.model.ResolvedStream
import app.miyomu.model.ResolvedVideo
import app.miyomu.model.ReadingProgress
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

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

    override fun observeAll(): Flow<List<ReadingProgress>> =
        dao.observeAllProgressRows().map { rows -> rows.map { it.toModel() } }

    override suspend fun save(progress: ReadingProgress) {
        dao.upsertProgress(progress.toEntity())
    }

    override suspend fun clear(itemId: String) {
        dao.clearProgress(itemId)
    }

    override suspend fun clearEntry(itemId: String, entryId: String) {
        dao.clearProgressEntry(itemId, entryId)
    }

    override suspend fun clearAll() {
        dao.clearAllProgress()
    }
}

internal class RoomAnimeRepository(
    private val dao: MiyomuDao,
) : AnimeRepository {

    override fun observeEpisodes(itemId: String): Flow<List<EpisodeRecord>> =
        dao.observeEpisodes(itemId).map { rows -> rows.map(EpisodeEntity::toRecord) }

    override suspend fun episodes(itemId: String): List<EpisodeRecord> =
        dao.episodes(itemId).map(EpisodeEntity::toRecord)

    override suspend fun clearAll() {
        dao.clearAllEpisodes()
    }

    override suspend fun replaceEpisodes(itemId: String, episodes: List<EpisodeRecord>, fetchedAt: Long) {
        val existing = dao.episodes(itemId).associateBy { it.key }
        val incoming = episodes.map { record ->
            val marks = existing[record.key]
            EpisodeEntity(
                itemId = itemId,
                key = record.key,
                name = record.name,
                url = record.url,
                episodeNumber = record.episodeNumber,
                sourceOrder = record.sourceOrder,
                seen = marks?.seen ?: record.seen,
                bookmark = marks?.bookmark ?: record.bookmark,
                fetchedAt = fetchedAt,
            )
        }
        dao.upsertEpisodes(incoming)
        val keys = episodes.map { it.key }
        dao.deleteEpisodesNotIn(itemId, keys)
    }

    override suspend fun setSeen(itemId: String, keys: List<String>, seen: Boolean) {
        if (keys.isNotEmpty()) dao.setEpisodesSeen(itemId, keys, seen)
    }

    override suspend fun setBookmarked(itemId: String, keys: List<String>, bookmarked: Boolean) {
        if (keys.isNotEmpty()) dao.setEpisodesBookmarked(itemId, keys, bookmarked)
    }

    override suspend fun clear(itemId: String) {
        dao.clearEpisodes(itemId)
    }
}

internal class RoomChapterRepository(
    private val dao: MiyomuDao,
) : ChapterRepository {

    override fun observeChapters(itemId: String): Flow<List<ChapterRecord>> =
        dao.observeChapters(itemId).map { rows -> rows.map(ChapterEntity::toRecord) }

    override suspend fun chapters(itemId: String): List<ChapterRecord> =
        dao.chapters(itemId).map(ChapterEntity::toRecord)

    override suspend fun replaceChapters(itemId: String, chapters: List<ChapterRecord>, fetchedAt: Long) {
        val existing = dao.chapters(itemId).associateBy { it.key }
        val incoming = chapters.map { record ->
            val marks = existing[record.key]
            ChapterEntity(
                itemId = itemId,
                key = record.key,
                name = record.name,
                url = record.url,
                chapterNumber = record.chapterNumber,
                sourceOrder = record.sourceOrder,
                read = marks?.read ?: record.read,
                bookmark = marks?.bookmark ?: record.bookmark,
                fetchedAt = fetchedAt,
            )
        }
        dao.upsertChapters(incoming)
        val keys = chapters.map { it.key }
        dao.deleteChaptersNotIn(itemId, keys)
    }

    override suspend fun setRead(itemId: String, keys: List<String>, read: Boolean) {
        if (keys.isNotEmpty()) dao.setChaptersRead(itemId, keys, read)
    }

    override suspend fun setBookmarked(itemId: String, keys: List<String>, bookmarked: Boolean) {
        if (keys.isNotEmpty()) dao.setChaptersBookmarked(itemId, keys, bookmarked)
    }

    override suspend fun clear(itemId: String) {
        dao.clearChapters(itemId)
    }

    override suspend fun clearAll() {
        dao.clearAllChapters()
    }
}

internal class RoomVideoResolveRepository(
    private val dao: MiyomuDao,
) : VideoResolveRepository {

    override suspend fun get(key: String, maxAgeMillis: Long, now: Long): ResolvedVideo? {
        val entry = dao.resolveEntry(key) ?: return null
        if (now - entry.fetchedAt > maxAgeMillis) return null
        return runCatching { decodeResolvedVideo(entry) }.getOrNull()
    }

    override suspend fun put(key: String, video: ResolvedVideo, now: Long) {
        dao.upsertResolveEntry(
            VideoResolveEntity(
                key = key,
                streamsJson = Json.encodeToString(video.streams),
                headersJson = Json.encodeToString(video.headers),
                fetchedAt = now,
            ),
        )
    }

    override suspend fun clearAll() {
        dao.clearResolveCache()
    }

    override suspend fun clearExpired(now: Long, maxAgeMillis: Long) {
        dao.deleteResolveOlderThan(now - maxAgeMillis)
    }
}

private fun decodeResolvedVideo(entry: VideoResolveEntity): ResolvedVideo = ResolvedVideo(
    streams = Json.decodeFromString<List<ResolvedStream>>(entry.streamsJson),
    headers = Json.decodeFromString(entry.headersJson),
)

private fun ChapterEntity.toRecord(): ChapterRecord = ChapterRecord(
    key = key,
    name = name,
    url = url,
    chapterNumber = chapterNumber,
    sourceOrder = sourceOrder,
    read = read,
    bookmark = bookmark,
    fetchedAt = fetchedAt,
)

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

// --- moved from CategoryRepository.kt -------------------------------------------

internal class RoomCategoryRepository(
    private val dao: MiyomuDao,
) : CategoryRepository {

    override fun observeCategories(): Flow<List<Category>> =
        dao.observeCategories().map { rows -> rows.map(CategoryEntity::toModel) }

    override suspend fun categories(): List<Category> =
        dao.categories().map(CategoryEntity::toModel)

    override suspend fun upsert(category: Category): Long =
        if (category.id == Category.UNCATEGORIZED_ID) {
            dao.insertCategory(
                CategoryEntity(id = 0, name = category.name, order = category.order, flags = category.flags, hidden = category.hidden),
            )
        } else {
            dao.updateCategory(
                CategoryEntity(id = category.id, name = category.name, order = category.order, flags = category.flags, hidden = category.hidden),
            )
            category.id
        }

    override suspend fun delete(id: Long) = dao.deleteCategory(id)

    override suspend fun categoryIdsForItem(itemId: String): List<Long> =
        dao.categoryIdsForItem(itemId)

    override fun observeItemIdsForCategory(categoryId: Long): Flow<List<String>> =
        dao.observeItemsForCategory(categoryId)

    override suspend fun setItemCategories(itemId: String, categoryIds: List<Long>) {
        dao.clearItemCategories(itemId)
        dao.upsertMangaCategories(categoryIds.map { MangaCategoryEntity(itemId = itemId, categoryId = it) })
    }

    override suspend fun allMemberships(): List<Pair<String, Long>> =
        dao.allMangaCategoryRows().map { it.itemId to it.categoryId }
}

private fun CategoryEntity.toModel(): Category = Category(
    id = id,
    name = name,
    order = order,
    flags = flags,
    hidden = hidden,
)

private fun Category.toEntity(): CategoryEntity = CategoryEntity(
    id = id,
    name = name,
    order = order,
    flags = flags,
    hidden = hidden,
)

// --- moved from LibraryUpdateRepository.kt -------------------------------------------

internal class RoomLibraryUpdateRepository(
    private val dao: MiyomuDao,
) : LibraryUpdateRepository {

    override fun observeUpdates(): Flow<List<LibraryUpdate>> =
        dao.observeLibraryUpdates().map { rows -> rows.map(LibraryUpdateEntity::toModel) }

    override suspend fun recordUpdates(itemId: String, updates: List<LibraryUpdate>) {
        if (updates.isEmpty()) return
        dao.upsertLibraryUpdates(
            updates.map {
                LibraryUpdateEntity(
                    itemId = itemId,
                    key = it.key,
                    name = it.name,
                    chapterNumber = it.chapterNumber,
                    fetchedAt = it.fetchedAt,
                )
            },
        )
    }

    override suspend fun deleteUpdate(itemId: String, key: String) = dao.deleteLibraryUpdate(itemId, key)

    override suspend fun clear() = dao.clearLibraryUpdates()
}

private fun LibraryUpdateEntity.toModel(): LibraryUpdate = LibraryUpdate(
    itemId = itemId,
    key = key,
    name = name,
    chapterNumber = chapterNumber,
    fetchedAt = fetchedAt,
)

// --- moved from BackupRepository.kt -------------------------------------------

internal class RoomBackupRepository(
    private val dao: MiyomuDao,
) : BackupRepository {

    override suspend fun export(): BackupData = BackupData(
        exportedAt = 0L, // stamped by the caller
        library = dao.allLibraryRows().map { it.toBackup() },
        progress = dao.allProgressRows().map { it.toBackup() },
        chapters = dao.allChapterRows().map { it.toBackup() },
        episodes = dao.allEpisodeRows().map { it.toBackup() },
        categories = dao.categories().map { it.toBackup() },
        categoryMemberships = dao.allMangaCategoryRows().map { BackupCategoryMembership(it.itemId, it.categoryId) },
    )

    override suspend fun restore(data: BackupData, replace: Boolean) {
        if (replace) {
            dao.clearAllProgress()
            dao.clearAllChapters()
            dao.clearAllEpisodes()
            dao.clearAllLibrary()
        }
        data.library.forEach { dao.upsertLibrary(it.toEntity()) }
        data.progress.forEach { dao.upsertProgress(it.toEntity()) }
        dao.upsertChapters(data.chapters.map { it.toEntity() })
        dao.upsertEpisodes(data.episodes.map { it.toEntity() })
        data.categories.forEach { category ->
            if (category.id != 0L) {
                dao.insertCategory(
                    CategoryEntity(
                        id = category.id, name = category.name, order = category.order,
                        flags = category.flags, hidden = category.hidden,
                    ),
                )
            }
        }
        dao.upsertMangaCategories(
            data.categoryMemberships.map { MangaCategoryEntity(it.itemId, it.categoryId) },
        )
    }
}

private fun LibraryItemEntity.toBackup() = BackupLibraryItem(
    id = id,
    mediaType = mediaType,
    sourceId = sourceId,
    sourceKey = sourceKey,
    title = title,
    coverUrl = coverUrl,
    author = author,
    description = description,
    status = status,
    addedAt = addedAt,
    updatedAt = updatedAt,
    lastViewedAt = lastViewedAt,
)

private fun BackupLibraryItem.toEntity() = LibraryItemEntity(
    id = id,
    mediaType = mediaType,
    sourceId = sourceId,
    sourceKey = sourceKey,
    title = title,
    coverUrl = coverUrl,
    author = author,
    description = description,
    status = status,
    addedAt = addedAt,
    updatedAt = updatedAt,
    lastViewedAt = lastViewedAt,
)

private fun ReadingProgressEntity.toBackup() = BackupProgress(
    itemId = itemId,
    entryId = entryId,
    kind = kind,
    position = position,
    duration = duration,
    updatedAt = updatedAt,
    deviceId = deviceId,
)

private fun BackupProgress.toEntity() = ReadingProgressEntity(
    itemId = itemId,
    entryId = entryId,
    kind = kind,
    position = position,
    duration = duration,
    updatedAt = updatedAt,
    deviceId = deviceId,
)

private fun CategoryEntity.toBackup() = BackupCategory(
    id = id,
    name = name,
    order = order,
    flags = flags,
    hidden = hidden,
)

private fun ChapterEntity.toBackup() = BackupChapter(
    itemId = itemId,
    key = key,
    name = name,
    url = url,
    chapterNumber = chapterNumber,
    sourceOrder = sourceOrder,
    read = read,
    bookmark = bookmark,
    fetchedAt = fetchedAt,
)

private fun BackupChapter.toEntity() = ChapterEntity(
    itemId = itemId,
    key = key,
    name = name,
    url = url,
    chapterNumber = chapterNumber,
    sourceOrder = sourceOrder,
    read = read,
    bookmark = bookmark,
    fetchedAt = fetchedAt,
)

private fun EpisodeEntity.toBackup() = BackupEpisode(
    itemId = itemId,
    key = key,
    name = name,
    url = url,
    episodeNumber = episodeNumber,
    sourceOrder = sourceOrder,
    seen = seen,
    bookmark = bookmark,
    fetchedAt = fetchedAt,
)

private fun BackupEpisode.toEntity() = EpisodeEntity(
    itemId = itemId,
    key = key,
    name = name,
    url = url,
    episodeNumber = episodeNumber,
    sourceOrder = sourceOrder,
    seen = seen,
    bookmark = bookmark,
    fetchedAt = fetchedAt,
)

// --- moved from AnimeRepository.kt -----------------------------------

internal fun EpisodeEntity.toRecord(): EpisodeRecord = EpisodeRecord(
    key = key,
    name = name,
    url = url,
    episodeNumber = episodeNumber,
    sourceOrder = sourceOrder,
    seen = seen,
    bookmark = bookmark,
    fetchedAt = fetchedAt,
)
