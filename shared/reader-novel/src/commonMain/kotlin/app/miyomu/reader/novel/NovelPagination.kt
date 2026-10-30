package app.miyomu.reader.novel

/**
 * Pure pagination math shared by the compose reader. The UI measures a chapter into
 * lines (via TextMeasurer) and feeds the line metrics here; keeping the grouping
 * logic separate makes it unit-testable without a composition.
 */
object NovelPagination {

    /** One measured text line with its vertical extent and start char offset. */
    data class LineMetric(
        val topY: Float,
        val bottomY: Float,
        val startOffset: Int,
    )

    data class Page(
        /** First char of the page, inclusive. */
        val startOffset: Int,
        /** First char of the next page, exclusive; [Int.MAX_VALUE] for the last page. */
        val endOffset: Int,
    ) {
        fun contains(offset: Int): Boolean = offset >= startOffset && offset < endOffset
    }

    /**
     * Groups consecutive [lines] into pages of at most [pageHeight] pixels. The first line
     * whose bottom edge overflows the open page starts a new page at its own top, so tall
     * lines are never clipped. Returns one [Page] per page; the union of all ranges covers
     * the whole chapter.
     */
    fun paginate(lines: List<LineMetric>, pageHeight: Float): List<Page> {
        if (lines.isEmpty()) return emptyList()
        require(pageHeight > 0f) { "pageHeight must be positive" }

        val pages = mutableListOf<Page>()
        var pageStartLine = 0
        var pageTop = lines[0].topY

        for (index in lines.indices) {
            val line = lines[index]
            if (line.bottomY - pageTop > pageHeight && index > pageStartLine) {
                pages += Page(
                    startOffset = lines[pageStartLine].startOffset,
                    endOffset = line.startOffset,
                )
                pageStartLine = index
                pageTop = line.topY
            }
        }
        pages += Page(
            startOffset = lines[pageStartLine].startOffset,
            endOffset = Int.MAX_VALUE,
        )
        return pages
    }

    /**
     * Char offset -> page index, for restoring progress. Binary search over the page
     * start offsets; offsets before the first page land on it, offsets past the last
     * clamp to it.
     */
    fun pageIndexForOffset(pages: List<Page>, offset: Int): Int {
        if (pages.isEmpty()) return 0
        var low = 0
        var high = pages.lastIndex
        var result = 0
        while (low <= high) {
            val mid = (low + high) / 2
            if (pages[mid].startOffset <= offset) {
                result = mid
                low = mid + 1
            } else {
                high = mid - 1
            }
        }
        return result
    }
}
