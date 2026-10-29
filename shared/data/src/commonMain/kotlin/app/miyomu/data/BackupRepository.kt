package app.miyomu.data

import app.miyomu.data.db.CategoryEntity
import app.miyomu.data.db.ChapterEntity
import app.miyomu.data.db.MangaCategoryEntity
import app.miyomu.data.db.EpisodeEntity
import app.miyomu.data.db.LibraryItemEntity
import app.miyomu.data.db.MiyomuDao
import app.miyomu.data.db.ReadingProgressEntity

/** Serializable snapshot of the whole data layer, written by the backup manager. */
@kotlinx.serialization.Serializable
data class BackupData(
    val version: Int = 1,
    val exportedAt: Long,
    val library: List<BackupLibraryItem> = emptyList(),
    val progress: List<BackupProgress> = emptyList(),
    val chapters: List<BackupChapter> = emptyList(),
    val episodes: List<BackupEpisode> = emptyList(),
    val categories: List<BackupCategory> = emptyList(),
    val categoryMemberships: List<BackupCategoryMembership> = emptyList(),
)

@kotlinx.serialization.Serializable
data class BackupCategory(
    val id: Long,
    val name: String,
    val order: Long,
    val flags: Long = 0L,
    val hidden: Boolean = false,
)

@kotlinx.serialization.Serializable
data class BackupCategoryMembership(
    val itemId: String,
    val categoryId: Long,
)

@kotlinx.serialization.Serializable
data class BackupLibraryItem(
    val id: String,
    val mediaType: String,
    val sourceId: String,
    val sourceKey: String,
    val title: String,
    val coverUrl: String? = null,
    val author: String? = null,
    val description: String? = null,
    val status: String,
    val addedAt: Long,
    val updatedAt: Long,
    val lastViewedAt: Long? = null,
)

@kotlinx.serialization.Serializable
data class BackupProgress(
    val itemId: String,
    val entryId: String,
    val kind: String,
    val position: Double,
    val duration: Double? = null,
    val updatedAt: Long,
    val deviceId: String,
)

@kotlinx.serialization.Serializable
data class BackupChapter(
    val itemId: String,
    val key: String,
    val name: String,
    val url: String,
    val chapterNumber: Double? = null,
    val sourceOrder: Long,
    val read: Boolean = false,
    val bookmark: Boolean = false,
    val fetchedAt: Long = 0,
)

@kotlinx.serialization.Serializable
data class BackupEpisode(
    val itemId: String,
    val key: String,
    val name: String,
    val url: String,
    val episodeNumber: Double? = null,
    val sourceOrder: Long,
    val seen: Boolean = false,
    val bookmark: Boolean = false,
    val fetchedAt: Long = 0,
)

interface BackupRepository {
    suspend fun export(): BackupData

    /** Restores rows on top of the current database; with [replace] the tables are cleared first. */
    suspend fun restore(data: BackupData, replace: Boolean)
}

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
