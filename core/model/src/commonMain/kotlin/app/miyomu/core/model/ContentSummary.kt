package app.miyomu.core.model

data class ContentSummary(
    val id: ContentId,
    val source: SourceId,
    val title: String,
    val coverUrl: String? = null,
    val authors: List<String> = emptyList(),
    val contentType: ContentType = ContentType.MANGA,
    val rating: Float? = null,
    val state: ContentState = ContentState.UNKNOWN,
)
