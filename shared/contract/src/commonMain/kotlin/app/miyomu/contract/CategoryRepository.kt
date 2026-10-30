package app.miyomu.contract

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import app.miyomu.model.Category

interface CategoryRepository {
    fun observeCategories(): Flow<List<Category>>
    suspend fun categories(): List<Category>

    /** Creates or renames a category; returns its id. */
    suspend fun upsert(category: Category): Long

    suspend fun delete(id: Long)

    suspend fun categoryIdsForItem(itemId: String): List<Long>

    fun observeItemIdsForCategory(categoryId: Long): Flow<List<String>>

    /** Replaces the categories an item belongs to (Aniyomi "edit categories"). */
    suspend fun setItemCategories(itemId: String, categoryIds: List<Long>)

    /** All (itemId, categoryId) rows, for library filtering. */
    suspend fun allMemberships(): List<Pair<String, Long>>
}
