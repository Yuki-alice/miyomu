package app.miyomu.data.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface MiyomuDao {
    @Query("SELECT * FROM library_item ORDER BY COALESCE(lastViewedAt, 0) DESC, updatedAt DESC")
    fun observeLibrary(): Flow<List<LibraryItemEntity>>

    @Query("SELECT * FROM library_item WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): LibraryItemEntity?

    @Upsert
    suspend fun upsertLibrary(item: LibraryItemEntity)

    @Query("DELETE FROM library_item WHERE id = :id")
    suspend fun deleteLibrary(id: String)

    @Query("UPDATE library_item SET lastViewedAt = :viewedAt, updatedAt = :viewedAt WHERE id = :id")
    suspend fun markViewed(id: String, viewedAt: Long)

    @Query("SELECT * FROM reading_progress WHERE itemId = :itemId ORDER BY updatedAt DESC LIMIT 1")
    fun observeLatestProgress(itemId: String): Flow<List<ReadingProgressEntity>>

    @Query("SELECT * FROM reading_progress WHERE itemId = :itemId ORDER BY updatedAt DESC LIMIT 1")
    suspend fun latestProgress(itemId: String): ReadingProgressEntity?

    @Query("SELECT * FROM reading_progress WHERE itemId = :itemId ORDER BY updatedAt DESC")
    suspend fun allProgress(itemId: String): List<ReadingProgressEntity>

    @Query("SELECT * FROM reading_progress WHERE itemId = :itemId AND entryId = :entryId ORDER BY updatedAt DESC LIMIT 1")
    suspend fun progressForEntry(itemId: String, entryId: String): ReadingProgressEntity?

    @Upsert
    suspend fun upsertProgress(progress: ReadingProgressEntity)

    @Query("DELETE FROM reading_progress WHERE itemId = :itemId")
    suspend fun clearProgress(itemId: String)
}
