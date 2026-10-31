package app.miyomu.core.database

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "favourite")
data class FavouriteEntity(
    @PrimaryKey @ColumnInfo(name = "contentId") val contentId: Long,
    @ColumnInfo(name = "sourceName") val sourceName: String,
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "coverUrl") val coverUrl: String?,
    @ColumnInfo(name = "authors") val authors: String,
    @ColumnInfo(name = "contentType") val contentType: String,
    @ColumnInfo(name = "createdAt") val createdAt: Long,
    @ColumnInfo(name = "updatedAt") val updatedAt: Long,
)

@Entity(tableName = "reading_progress")
data class ReadingProgressEntity(
    @PrimaryKey @ColumnInfo(name = "contentId") val contentId: Long,
    @ColumnInfo(name = "chapterId") val chapterId: Long,
    @ColumnInfo(name = "chapterTitle") val chapterTitle: String?,
    @ColumnInfo(name = "page") val page: Int,
    @ColumnInfo(name = "scroll") val scroll: Float,
    @ColumnInfo(name = "percent") val percent: Float,
    @ColumnInfo(name = "updatedAt") val updatedAt: Long,
)

@Dao
interface FavouriteDao {
    @Query("SELECT * FROM favourite ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<FavouriteEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM favourite WHERE contentId = :contentId)")
    fun observeIsFavourite(contentId: Long): Flow<Boolean>

    @Upsert
    suspend fun upsert(entity: FavouriteEntity)

    @Query("DELETE FROM favourite WHERE contentId = :contentId")
    suspend fun delete(contentId: Long)
}

@Dao
interface ReadingProgressDao {
    @Query("SELECT * FROM reading_progress WHERE contentId = :contentId")
    fun observe(contentId: Long): Flow<ReadingProgressEntity?>

    @Query("SELECT * FROM reading_progress WHERE contentId = :contentId")
    suspend fun find(contentId: Long): ReadingProgressEntity?

    @Upsert
    suspend fun upsert(entity: ReadingProgressEntity)
}
