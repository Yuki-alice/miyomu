package app.miyomu.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "library_item",
    indices = [Index(value = ["sourceId", "sourceKey"], unique = true)],
)
data class LibraryItemEntity(
    @PrimaryKey val id: String,
    val mediaType: String,
    val sourceId: String,
    val sourceKey: String,
    val title: String,
    val coverUrl: String?,
    val author: String?,
    val description: String?,
    val status: String,
    val addedAt: Long,
    val updatedAt: Long,
    val lastViewedAt: Long?,
)

@Entity(
    tableName = "reading_progress",
    primaryKeys = ["itemId", "entryId", "deviceId"],
)
data class ReadingProgressEntity(
    val itemId: String,
    val entryId: String,
    val kind: String,
    val position: Double,
    val duration: Double?,
    val updatedAt: Long,
    val deviceId: String,
)

/**
 * Persisted episode row for any anime item (local scan or online source). `key` is the stable
 * identity a scan/source agrees on, `url` is the play target; marks survive re-scans because
 * replace reconciles on `key`.
 */
@Entity(
    tableName = "entry_episode",
    primaryKeys = ["itemId", "key"],
    indices = [Index(value = ["itemId", "sourceOrder"])],
)
data class EpisodeEntity(
    val itemId: String,
    val key: String,
    val name: String,
    val url: String,
    val episodeNumber: Double?,
    val sourceOrder: Long,
    val seen: Boolean,
    val bookmark: Boolean,
    val fetchedAt: Long,
)

/** Persisted chapter row for any comic item; same reconciliation contract as [EpisodeEntity]. */
@Entity(
    tableName = "entry_chapter",
    primaryKeys = ["itemId", "key"],
    indices = [Index(value = ["itemId", "sourceOrder"])],
)
data class ChapterEntity(
    val itemId: String,
    val key: String,
    val name: String,
    val url: String,
    val chapterNumber: Double?,
    val sourceOrder: Long,
    val read: Boolean,
    val bookmark: Boolean,
    val fetchedAt: Long,
)

/** Cached resolve result (stream URLs + anti-leech headers) for one episode url. */
@Entity(tableName = "video_resolve_cache")
data class VideoResolveEntity(
    @PrimaryKey val key: String,
    val streamsJson: String,
    val headersJson: String,
    val fetchedAt: Long,
)

/** User-defined library category (Aniyomi semantics; id 0 is the system "Default"). */
@Entity(tableName = "category")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val order: Long,
    val flags: Long = 0L,
    val hidden: Boolean = false,
)

/** Library item membership in a category, keyed like Aniyomi's manga_category table. */
@Entity(
    tableName = "manga_category",
    primaryKeys = ["itemId", "categoryId"],
    indices = [Index(value = ["categoryId"])],
)
data class MangaCategoryEntity(
    val itemId: String,
    val categoryId: Long,
)
