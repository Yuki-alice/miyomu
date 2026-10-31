package app.miyomu.core.model

data class FilterTag(val id: String, val title: String)

data class ContentFilter(
    val tags: Set<FilterTag> = emptySet(),
    val states: Set<String> = emptySet(),
    val year: Int? = null,
    val order: String? = null,
)

data class ContentFilterOptions(
    val tags: List<FilterTag> = emptyList(),
    val states: Set<String> = emptySet(),
    val sortOrders: Set<String> = emptySet(),
)
