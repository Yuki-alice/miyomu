package app.miyomu.local

interface LocalComicRepository {
    suspend fun chapters(book: LocalBook): List<LocalChapter>

    suspend fun pages(chapter: LocalChapter): List<LocalPage>

    suspend fun readPage(page: LocalPage): ByteArray

    suspend fun cover(book: LocalBook): ByteArray?
}
