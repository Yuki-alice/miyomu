package app.miyomu.core.model

data class PageSummary(
    val id: Long,
    val index: Int,
    val url: String,
    val headers: Map<String, String> = emptyMap(),
)
