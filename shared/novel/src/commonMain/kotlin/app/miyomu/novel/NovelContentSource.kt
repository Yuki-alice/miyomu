package app.miyomu.novel

/**
 * Abstraction over "where chapter content comes from", letting one reader serve both
 * local files and online sources. [NovelChapterData.itemId] buckets progress, [entryId]
 * keys the chapter inside that bucket.
 */
interface NovelContentSource {
    suspend fun load(bookKey: String, chapterIndex: Int, offset: Int): NovelChapterData
}

data class NovelChapterData(
    val itemId: String,
    val entryId: String,
    val chapterCount: Int,
    val chapterIndex: Int,
    val chapterTitle: String?,
    val chapterTitles: List<String?>,
    val blocks: List<NovelBlock>,
    val text: String,
    val pendingOffset: Int,
    val error: String? = null,
)
