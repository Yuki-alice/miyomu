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
