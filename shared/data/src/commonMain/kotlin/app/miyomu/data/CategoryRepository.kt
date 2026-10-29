package app.miyomu.data

import app.miyomu.data.db.CategoryEntity
import app.miyomu.data.db.MangaCategoryEntity
import app.miyomu.data.db.MiyomuDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import tachiyomi.domain.category.model.Category

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

internal class RoomCategoryRepository(
    private val dao: MiyomuDao,
) : CategoryRepository {

    override fun observeCategories(): Flow<List<Category>> =
        dao.observeCategories().map { rows -> rows.map(CategoryEntity::toModel) }

    override suspend fun categories(): List<Category> =
        dao.categories().map(CategoryEntity::toModel)

    override suspend fun upsert(category: Category): Long =
        if (category.id == Category.UNCATEGORIZED_ID) {
            dao.insertCategory(
                CategoryEntity(id = 0, name = category.name, order = category.order, flags = category.flags, hidden = category.hidden),
            )
        } else {
            dao.updateCategory(
                CategoryEntity(id = category.id, name = category.name, order = category.order, flags = category.flags, hidden = category.hidden),
            )
            category.id
        }

    override suspend fun delete(id: Long) = dao.deleteCategory(id)

    override suspend fun categoryIdsForItem(itemId: String): List<Long> =
        dao.categoryIdsForItem(itemId)

    override fun observeItemIdsForCategory(categoryId: Long): Flow<List<String>> =
        dao.observeItemsForCategory(categoryId)

    override suspend fun setItemCategories(itemId: String, categoryIds: List<Long>) {
        dao.clearItemCategories(itemId)
        dao.upsertMangaCategories(categoryIds.map { MangaCategoryEntity(itemId = itemId, categoryId = it) })
    }

    override suspend fun allMemberships(): List<Pair<String, Long>> =
        dao.allMangaCategoryRows().map { it.itemId to it.categoryId }
}

private fun CategoryEntity.toModel(): Category = Category(
    id = id,
    name = name,
    order = order,
    flags = flags,
    hidden = hidden,
)

private fun Category.toEntity(): CategoryEntity = CategoryEntity(
    id = id,
    name = name,
    order = order,
    flags = flags,
    hidden = hidden,
)
