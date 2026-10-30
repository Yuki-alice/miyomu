package app.miyomu.contract


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
