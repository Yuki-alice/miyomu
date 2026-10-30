package app.miyomu.novel

import app.miyomu.local.LocalBook
import app.miyomu.model.LibraryItem

/**
 * [NovelContentSource] over local files: parses the book through the cached
 * [LocalNovelRepository] and maps the result into reader data. Progress keys mirror the
 * comic conventions (A2-4): itemId from the library id, entryId from the chapter hash.
 */
class LocalNovelContentSource(
    private val novels: LocalNovelRepository,
    private val book: LocalBook,
) : NovelContentSource {

    override suspend fun load(bookKey: String, chapterIndex: Int, offset: Int): NovelChapterData {
        val fallback = NovelChapterData(
            itemId = LibraryItem.idOf("local", book.location),
            entryId = "",
            chapterCount = 0,
            chapterIndex = chapterIndex,
            chapterTitle = null,
            chapterTitles = emptyList(),
            blocks = emptyList(),
            text = "",
            pendingOffset = 0,
        )
        return runCatching {
            val parsed = novels.parse(book)
            val chapters = parsed.chapters
            val safeIndex = chapterIndex.coerceIn(0, chapters.lastIndex.coerceAtLeast(0))
            val chapter = chapters.getOrNull(safeIndex)
            NovelChapterData(
                itemId = fallback.itemId,
                entryId = LocalNovelRepository.chapterId(book, safeIndex),
                chapterCount = chapters.size,
                chapterIndex = safeIndex,
                chapterTitle = chapter?.title,
                chapterTitles = chapters.map { it.title },
                blocks = chapter?.blocks().orEmpty(),
                text = chapter?.text().orEmpty(),
                pendingOffset = if (safeIndex == chapterIndex) offset.coerceAtLeast(0) else 0,
            )
        }.getOrElse { failure ->
            fallback.copy(error = failure.message ?: failure.toString())
        }
    }

    /** The parse is cached in [LocalNovelRepository], so this is cheap after the first load. */
    override suspend fun chapters(bookKey: String): List<NovelChapter> =
        runCatching { novels.parse(book).chapters }.getOrDefault(emptyList())
}
