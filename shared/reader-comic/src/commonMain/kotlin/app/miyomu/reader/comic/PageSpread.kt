package app.miyomu.reader.comic

import app.miyomu.local.LocalPage

/**
 * One visual step in the reader: a single page, or two side-by-side pages in dual-page mode.
 * [startIndex] is the index of [pages].first() in the chapter's raw page list, which keeps
 * progress persistence independent of the layout mode.
 */
data class PageSpread(
    val startIndex: Int,
    val pages: List<LocalPage>,
)

/**
 * Builds the spreads shown by paged reading modes. In dual-page mode the first page is shown
 * alone by default (manga cover convention); disable that for chapters that start on a spread.
 */
fun pageSpreads(
    pages: List<LocalPage>,
    dualPage: Boolean,
    firstPageAlone: Boolean = true,
): List<PageSpread> {
    if (pages.isEmpty()) return emptyList()
    if (!dualPage) return pages.mapIndexed { index, page -> PageSpread(index, listOf(page)) }

    val spreads = mutableListOf<PageSpread>()
    var index = 0
    if (firstPageAlone) {
        spreads += PageSpread(0, listOf(pages.first()))
        index = 1
    }
    while (index < pages.size) {
        spreads += PageSpread(index, pages.subList(index, minOf(index + 2, pages.size)))
        index += 2
    }
    return spreads
}

/**
 * Raw page index -> spread index, so the UI can restore the spread that contains a stored
 * progress value.
 */
fun spreadIndexForPage(spreads: List<PageSpread>, pageIndex: Int): Int {
    if (spreads.isEmpty()) return 0
    var result = 0
    for (index in spreads.indices) {
        if (spreads[index].startIndex <= pageIndex) result = index else break
    }
    return result
}
