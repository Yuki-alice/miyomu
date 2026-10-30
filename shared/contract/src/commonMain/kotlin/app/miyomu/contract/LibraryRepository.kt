package app.miyomu.contract

import app.miyomu.model.LibraryItem
import kotlinx.coroutines.flow.Flow

interface LibraryRepository {
    fun observeLibrary(): Flow<List<LibraryItem>>
    suspend fun get(id: String): LibraryItem?
    suspend fun upsert(item: LibraryItem)
    suspend fun delete(id: String)
    suspend fun markViewed(id: String, viewedAt: Long)
}
