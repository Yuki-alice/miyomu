package app.miyomu.contract

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** One discovered chapter from a library update check (Aniyomi's updates feed). */
data class LibraryUpdate(
    val itemId: String,
    val key: String,
    val name: String,
    val chapterNumber: Double?,
    val fetchedAt: Long,
)

interface LibraryUpdateRepository {
    fun observeUpdates(): Flow<List<LibraryUpdate>>

    suspend fun recordUpdates(itemId: String, updates: List<LibraryUpdate>)

    suspend fun deleteUpdate(itemId: String, key: String)

    suspend fun clear()
}
