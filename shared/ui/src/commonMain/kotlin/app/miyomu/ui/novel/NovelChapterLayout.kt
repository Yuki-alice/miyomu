package app.miyomu.ui.novel

import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.yield

/**
 * A chapter laid out for the paged reader. Paragraphs are measured individually (one
 * [TextLayoutResult] each) and stacked into pages; a paragraph taller than the page is
 * split by its own lines. Global char offsets are preserved so progress survives
 * configuration changes.
 */
data class NovelChapterPages(
    val chapterIndex: Int,
    val pieces: List<Piece>,
    val pages: List<Page>,
) {
    /** One measured paragraph (or a chunk of an unbroken mega-paragraph). */
    data class Piece(val startChar: Int, val layout: TextLayoutResult)

    /** Where a piece (or a slice of it) sits on a page. */
    data class Placement(
        val pieceIndex: Int,
        /** Absolute y of the visible slice inside the piece's layout. */
        val topInPiece: Float,
        val height: Float,
        /** y of the slice within its page. */
        val yInPage: Float,
        val startChar: Int,
    )

    data class Page(val placements: List<Placement>) {
        val startChar: Int get() = placements.first().startChar
    }

    val pageCount: Int get() = pages.size

    /** Global char offset (within the chapter text) of the first char on [pageIndex]. */
    fun startOffsetOf(pageIndex: Int): Int =
        pages[pageIndex.coerceIn(0, pages.lastIndex)].startChar

    /** Page containing [offset]; clamps to the last page. */
    fun pageIndexForOffset(offset: Int): Int {
        if (pages.isEmpty()) return 0
        var low = 0
        var high = pages.lastIndex
        var result = 0
        while (low <= high) {
            val mid = (low + high) / 2
            if (pages[mid].startChar <= offset) {
                result = mid
                low = mid + 1
            } else {
                high = mid - 1
            }
        }
        return result
    }
}

/** Chapter text split into paragraphs; blank lines produce no piece. */
internal fun splitParagraphs(text: String): List<Pair<Int, String>> {
    val paragraphs = mutableListOf<Pair<Int, String>>()
    var start = 0
    while (start <= text.lastIndex) {
        val newline = text.indexOf('\n', start)
        val end = if (newline == -1) text.length else newline
        if (end > start) paragraphs += start to text.substring(start, end)
        if (newline == -1) break
        start = newline + 1
    }
    return paragraphs
}

/**
 * Measures [text] into [NovelChapterPages]. Yields between paragraphs so a long chapter
 * cannot starve the frame. [paragraphSpacingPx] is the gap inserted between paragraphs.
 */
internal suspend fun measureChapter(
    chapterIndex: Int,
    text: String,
    measurer: TextMeasurer,
    density: Density,
    usableWidthPx: Int,
    usableHeightPx: Int,
    fontSizeSp: Int,
    lineHeightMultiplier: Float,
    paragraphSpacingPx: Int,
): NovelChapterPages {
    val style = TextStyle(
        fontSize = fontSizeSp.sp,
        lineHeight = (fontSizeSp * lineHeightMultiplier).sp,
    )

    val pieces = mutableListOf<NovelChapterPages.Piece>()
    for ((start, paragraph) in splitParagraphs(text)) {
        var chunkStart = 0
        while (chunkStart < paragraph.length) {
            val chunk = paragraph.substring(
                chunkStart,
                (chunkStart + SECTION_MAX_CHARS).coerceAtMost(paragraph.length),
            )
            val layout = measurer.measure(
                text = chunk,
                style = style,
                constraints = Constraints(maxWidth = usableWidthPx.coerceAtLeast(1)),
                maxLines = Int.MAX_VALUE,
            )
            pieces += NovelChapterPages.Piece(start + chunkStart, layout)
            chunkStart += SECTION_MAX_CHARS
        }
        yield()
    }

    return NovelChapterPages(
        chapterIndex = chapterIndex,
        pieces = pieces,
        pages = paginatePieces(pieces, usableHeightPx, paragraphSpacingPx),
    )
}

private fun paginatePieces(
    pieces: List<NovelChapterPages.Piece>,
    pageHeightPx: Int,
    paragraphSpacingPx: Int,
): List<NovelChapterPages.Page> {
    val pageHeight = pageHeightPx.toFloat()
    val spacing = paragraphSpacingPx.toFloat()

    val pages = mutableListOf<NovelChapterPages.Page>()
    var current = mutableListOf<NovelChapterPages.Placement>()
    var yInPage = 0f

    fun closePage() {
        if (current.isNotEmpty()) {
            pages += NovelChapterPages.Page(current.toList())
            current = mutableListOf()
            yInPage = 0f
        }
    }

    for ((pieceIndex, piece) in pieces.withIndex()) {
        val pieceHeight = piece.layout.size.height.toFloat()

        if (pieceHeight <= pageHeight) {
            if (current.isNotEmpty() && yInPage + pieceHeight > pageHeight) closePage()
            current += NovelChapterPages.Placement(
                pieceIndex = pieceIndex,
                topInPiece = 0f,
                height = pieceHeight,
                yInPage = yInPage,
                startChar = piece.startChar,
            )
            yInPage += pieceHeight + spacing
        } else {
            // A paragraph taller than the page is sliced by its own lines; each slice
            // opens a fresh page.
            val layout = piece.layout
            var line = 0
            while (line < layout.lineCount) {
                closePage()
                val sliceTop = layout.getLineTop(line)
                var end = line
                while (end < layout.lineCount &&
                    layout.getLineBottom(end) - sliceTop <= pageHeight
                ) {
                    end++
                }
                val lastLine = (end - 1).coerceAtLeast(line)
                current += NovelChapterPages.Placement(
                    pieceIndex = pieceIndex,
                    topInPiece = sliceTop,
                    height = layout.getLineBottom(lastLine) - sliceTop,
                    yInPage = 0f,
                    startChar = layout.getLineStart(line),
                )
                line = end
            }
        }
    }
    closePage()
    return pages
}

private const val SECTION_MAX_CHARS = 20_000
