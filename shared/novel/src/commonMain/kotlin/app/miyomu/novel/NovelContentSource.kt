package app.miyomu.novel

/**
 * Abstraction over "where chapter content comes from", letting one reader serve both
 * local files and online sources. [NovelChapterData.itemId] buckets progress, [entryId]
 * keys the chapter inside that bucket.
 */
interface NovelContentSource {
    suspend fun load(bookKey: String, chapterIndex: Int, offset: Int): NovelChapterData

    /**
     * Every chapter of the book, for whole-book features such as in-book search.
     *
     * Sources that cannot materialise the whole book up front (an online source would have
     * to download each chapter) keep the default and the caller falls back to a
     * titles-only search.
     */
    suspend fun chapters(bookKey: String): List<NovelChapter> = emptyList()
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
