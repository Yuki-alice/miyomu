package app.miyomu.local

interface LocalComicRepository : ReaderPageSource {
    override suspend fun chapters(book: LocalBook): List<LocalChapter>

    /** Child folders of a video series, each one a season with its own episodes. */
    suspend fun seasons(book: LocalBook): List<LocalBook> = emptyList()

    /** Deletes the book's files from disk (batch delete in the library; UI confirms first). */
    suspend fun delete(book: LocalBook)

    override suspend fun pages(chapter: LocalChapter): List<LocalPage>

    override suspend fun readPage(page: LocalPage): ByteArray

    suspend fun cover(book: LocalBook): ByteArray?
}
