package tachiyomi.domain.category.model

data class Category(
    val id: Long,
    val name: String,
    val order: Long,
    val flags: Long = 0L,
    val hidden: Boolean = false,
) {
    val isSystemCategory: Boolean get() = id == UNCATEGORIZED_ID

    companion object {
        const val UNCATEGORIZED_ID = 0L
        const val DEFAULT_NAME = "Default"
    }
}
