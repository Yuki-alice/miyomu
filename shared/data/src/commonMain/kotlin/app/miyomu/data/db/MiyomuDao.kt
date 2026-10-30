package app.miyomu.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Update
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

    @Query("SELECT * FROM entry_episode WHERE itemId = :itemId ORDER BY sourceOrder")
    fun observeEpisodes(itemId: String): Flow<List<EpisodeEntity>>

    @Query("SELECT * FROM entry_episode WHERE itemId = :itemId ORDER BY sourceOrder")
    suspend fun episodes(itemId: String): List<EpisodeEntity>

    @Upsert
    suspend fun upsertEpisodes(episodes: List<EpisodeEntity>)

    @Query("UPDATE entry_episode SET seen = :seen WHERE itemId = :itemId AND key IN (:keys)")
    suspend fun setEpisodesSeen(itemId: String, keys: List<String>, seen: Boolean)

    @Query("UPDATE entry_episode SET bookmark = :bookmark WHERE itemId = :itemId AND key IN (:keys)")
    suspend fun setEpisodesBookmarked(itemId: String, keys: List<String>, bookmark: Boolean)

    @Query("DELETE FROM entry_episode WHERE itemId = :itemId AND key NOT IN (:keys)")
    suspend fun deleteEpisodesNotIn(itemId: String, keys: List<String>)

    @Query("DELETE FROM entry_episode WHERE itemId = :itemId")
    suspend fun clearEpisodes(itemId: String)

    @Query("SELECT * FROM entry_chapter WHERE itemId = :itemId ORDER BY sourceOrder")
    fun observeChapters(itemId: String): Flow<List<ChapterEntity>>

    @Query("SELECT * FROM entry_chapter WHERE itemId = :itemId ORDER BY sourceOrder")
    suspend fun chapters(itemId: String): List<ChapterEntity>

    @Upsert
    suspend fun upsertChapters(chapters: List<ChapterEntity>)

    @Query("UPDATE entry_chapter SET read = :read WHERE itemId = :itemId AND key IN (:keys)")
    suspend fun setChaptersRead(itemId: String, keys: List<String>, read: Boolean)

    @Query("UPDATE entry_chapter SET bookmark = :bookmark WHERE itemId = :itemId AND key IN (:keys)")
    suspend fun setChaptersBookmarked(itemId: String, keys: List<String>, bookmark: Boolean)

    @Query("DELETE FROM entry_chapter WHERE itemId = :itemId AND key NOT IN (:keys)")
    suspend fun deleteChaptersNotIn(itemId: String, keys: List<String>)

    @Query("DELETE FROM entry_chapter WHERE itemId = :itemId")
    suspend fun clearChapters(itemId: String)

    @Query("SELECT * FROM reading_progress")
    suspend fun allProgressRows(): List<ReadingProgressEntity>

    @Query("SELECT * FROM reading_progress ORDER BY updatedAt DESC")
    fun observeAllProgressRows(): Flow<List<ReadingProgressEntity>>

    @Query("DELETE FROM reading_progress WHERE itemId = :itemId AND entryId = :entryId")
    suspend fun clearProgressEntry(itemId: String, entryId: String)

    @Query("SELECT * FROM entry_chapter")
    suspend fun allChapterRows(): List<ChapterEntity>

    @Query("SELECT * FROM entry_episode")
    suspend fun allEpisodeRows(): List<EpisodeEntity>

    @Query("SELECT * FROM library_item")
    suspend fun allLibraryRows(): List<LibraryItemEntity>

    @Query("DELETE FROM reading_progress")
    suspend fun clearAllProgress()

    @Query("DELETE FROM entry_chapter")
    suspend fun clearAllChapters()

    @Query("DELETE FROM entry_episode")
    suspend fun clearAllEpisodes()

    @Query("DELETE FROM library_item")
    suspend fun clearAllLibrary()

    @Query("SELECT * FROM video_resolve_cache WHERE key = :key LIMIT 1")
    suspend fun resolveEntry(key: String): VideoResolveEntity?

    @Upsert
    suspend fun upsertResolveEntry(entry: VideoResolveEntity)

    @Query("DELETE FROM video_resolve_cache WHERE fetchedAt < :before")
    suspend fun deleteResolveOlderThan(before: Long)

    // --- Categories ---

    @Query("SELECT * FROM category ORDER BY `order`")
    fun observeCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM category ORDER BY `order`")
    suspend fun categories(): List<CategoryEntity>

    @Insert
    suspend fun insertCategory(category: CategoryEntity): Long

    @Update
    suspend fun updateCategory(category: CategoryEntity)

    @Query("DELETE FROM category WHERE id = :id AND id != 0")
    suspend fun deleteCategory(id: Long)

    @Query("SELECT categoryId FROM manga_category WHERE itemId = :itemId")
    suspend fun categoryIdsForItem(itemId: String): List<Long>

    @Query("SELECT itemId FROM manga_category WHERE categoryId = :categoryId")
    fun observeItemsForCategory(categoryId: Long): Flow<List<String>>

    @Query("SELECT * FROM manga_category")
    suspend fun allMangaCategoryRows(): List<MangaCategoryEntity>

    @Upsert
    suspend fun upsertMangaCategories(rows: List<MangaCategoryEntity>)

    @Query("DELETE FROM manga_category WHERE itemId = :itemId")
    suspend fun clearItemCategories(itemId: String)

    // --- Library updates ---

    @Query("SELECT * FROM library_updates ORDER BY fetchedAt DESC")
    fun observeLibraryUpdates(): Flow<List<LibraryUpdateEntity>>

    @Upsert
    suspend fun upsertLibraryUpdates(rows: List<LibraryUpdateEntity>)

    @Query("DELETE FROM library_updates WHERE itemId = :itemId AND key = :key")
    suspend fun deleteLibraryUpdate(itemId: String, key: String)

    @Query("DELETE FROM library_updates")
    suspend fun clearLibraryUpdates()
}
