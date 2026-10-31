package app.miyomu.core.model

data class ChapterSummary(
    val id: ChapterId,
    val title: String,
    val number: Float,
    val uploadedAt: Long,
)

data class ContentDetails(
    val summary: ContentSummary,
    val description: String?,
    val chapters: List<ChapterSummary>,
)
