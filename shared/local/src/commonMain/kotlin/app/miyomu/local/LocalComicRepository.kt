package app.miyomu.local

interface LocalComicRepository {
    suspend fun chapters(book: LocalBook): List<LocalChapter>

    /** Child folders of a video series, each one a season with its own episodes. */
    suspend fun seasons(book: LocalBook): List<LocalBook> = emptyList()

    suspend fun pages(chapter: LocalChapter): List<LocalPage>

    suspend fun readPage(page: LocalPage): ByteArray

    suspend fun cover(book: LocalBook): ByteArray?
}
