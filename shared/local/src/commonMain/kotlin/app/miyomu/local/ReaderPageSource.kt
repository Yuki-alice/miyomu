package app.miyomu.local

/**
 * Everything the reader needs from a content backend. Local books use [LocalComicRepository];
 * online sources adapt their chapters/pages to the same shapes so the ported reader chrome and
 * controller stay shared.
 */
interface ReaderPageSource {
    suspend fun chapters(book: LocalBook): List<LocalChapter>

    suspend fun pages(chapter: LocalChapter): List<LocalPage>

    suspend fun readPage(page: LocalPage): ByteArray
}
