package app.miyomu.data

import app.miyomu.data.db.LibraryUpdateEntity
import app.miyomu.data.db.MiyomuDao
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

internal class RoomLibraryUpdateRepository(
    private val dao: MiyomuDao,
) : LibraryUpdateRepository {

    override fun observeUpdates(): Flow<List<LibraryUpdate>> =
        dao.observeLibraryUpdates().map { rows -> rows.map(LibraryUpdateEntity::toModel) }

    override suspend fun recordUpdates(itemId: String, updates: List<LibraryUpdate>) {
        if (updates.isEmpty()) return
        dao.upsertLibraryUpdates(
            updates.map {
                LibraryUpdateEntity(
                    itemId = itemId,
                    key = it.key,
                    name = it.name,
                    chapterNumber = it.chapterNumber,
                    fetchedAt = it.fetchedAt,
                )
            },
        )
    }

    override suspend fun deleteUpdate(itemId: String, key: String) = dao.deleteLibraryUpdate(itemId, key)

    override suspend fun clear() = dao.clearLibraryUpdates()
}

private fun LibraryUpdateEntity.toModel(): LibraryUpdate = LibraryUpdate(
    itemId = itemId,
    key = key,
    name = name,
    chapterNumber = chapterNumber,
    fetchedAt = fetchedAt,
)
