package app.miyomu.reader.comic

import app.miyomu.local.LocalBook
import app.miyomu.local.LocalChapter
import app.miyomu.local.LocalPage

data class ComicReaderUiState(
    val book: LocalBook? = null,
    val chapters: List<LocalChapter> = emptyList(),
    val chapterIndex: Int = 0,
    val pages: List<LocalPage> = emptyList(),
    val pageIndex: Int = 0,
    val mode: ComicReadingMode = ComicReadingMode.LEFT_TO_RIGHT,
    val loading: Boolean = false,
    val error: String? = null,
) {
    val currentChapter: LocalChapter?
        get() = chapters.getOrNull(chapterIndex)

    val currentPage: LocalPage?
        get() = pages.getOrNull(pageIndex)

    val hasPreviousChapter: Boolean
        get() = chapterIndex > 0

    val hasNextChapter: Boolean
        get() = chapterIndex < chapters.lastIndex
}

data class PagePair(
    val first: LocalPage,
    val second: LocalPage?,
)

/**
 * Pairs pages for dual-page mode. The first page is shown alone by default, matching
 * the common manga convention; callers can disable that for spreads that start on a pair.
 */
fun pairPages(pages: List<LocalPage>, firstPageAlone: Boolean = true): List<PagePair> {
    if (pages.isEmpty()) return emptyList()

    val result = mutableListOf<PagePair>()
    var index = 0
    if (firstPageAlone) {
        result += PagePair(pages.first(), null)
        index = 1
    }
    while (index < pages.size) {
        result += PagePair(pages[index], pages.getOrNull(index + 1))
        index += 2
    }
    return result
}
