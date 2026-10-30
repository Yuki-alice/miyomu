package app.miyomu.contract

import app.miyomu.model.ReadingProgress
import kotlinx.coroutines.flow.Flow

interface ProgressRepository {
    fun observeLatest(itemId: String): Flow<ReadingProgress?>
    suspend fun latest(itemId: String): ReadingProgress?
    suspend fun latestFor(itemId: String, entryId: String): ReadingProgress?

    /** Every saved position for an item, newest first (one row per entry). */
    suspend fun all(itemId: String): List<ReadingProgress>

    /** Every progress row across items, newest first — the reading-history source. */
    fun observeAll(): Flow<List<ReadingProgress>>

    suspend fun save(progress: ReadingProgress)
    suspend fun clear(itemId: String)

    /** Removes the whole reading history (data settings). */
    suspend fun clearAll()

    /** Removes one entry's history row (Aniyomi "remove from history"). */
    suspend fun clearEntry(itemId: String, entryId: String)
}
