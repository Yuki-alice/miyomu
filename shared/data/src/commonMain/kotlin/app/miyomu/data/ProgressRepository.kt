package app.miyomu.data

import app.miyomu.model.ReadingProgress
import kotlinx.coroutines.flow.Flow

interface ProgressRepository {
    fun observeLatest(itemId: String): Flow<ReadingProgress?>
    suspend fun latest(itemId: String): ReadingProgress?
    suspend fun latestFor(itemId: String, entryId: String): ReadingProgress?

    /** Every saved position for an item, newest first (one row per entry). */
    suspend fun all(itemId: String): List<ReadingProgress>
    suspend fun save(progress: ReadingProgress)
    suspend fun clear(itemId: String)
}
