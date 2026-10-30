package app.miyomu.ui.novel

import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.sp
import app.miyomu.novel.NovelBlock
import kotlinx.coroutines.yield

/**
 * A chapter laid out for the paged reader. Paragraphs are measured individually (one
 * [TextLayoutResult] each) and stacked into pages; a paragraph taller than the page is
 * split by its own lines. Illustrations are pieces with a height derived from the image
 * aspect ratio. Global char offsets are preserved so progress survives configuration
 * changes (images do not advance the text offset).
 */
data class NovelChapterPages(
    val chapterIndex: Int,
    val pieces: List<Piece>,
    val pages: List<Page>,
) {
    /** One measured paragraph (or a chunk of an unbroken mega-paragraph), or an image. */
    data class Piece(
        val startChar: Int,
        val layout: TextLayoutResult?,
        val image: NovelBlock.Image?,
    )

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
 * Measures a chapter's [NovelBlock]s into [NovelChapterPages]. Yields between text
 * paragraphs so a long chapter cannot starve the frame. [paragraphSpacingPx] is the gap
 * inserted between text pieces and around images.
 */
internal suspend fun measureChapter(
    chapterIndex: Int,
    blocks: List<NovelBlock>,
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
    for (block in blocks) {
        when (block) {
            is NovelBlock.Text -> {
                var chunkStart = 0
                val paragraph = block.text
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
                    pieces += NovelChapterPages.Piece(
                        startChar = block.startChar + chunkStart,
                        layout = layout,
                        image = null,
                    )
                    chunkStart += SECTION_MAX_CHARS
                }
            }
            is NovelBlock.Image -> pieces += NovelChapterPages.Piece(
                startChar = block.startChar,
                layout = null,
                image = block,
            )
        }
        yield()
    }

    return NovelChapterPages(
        chapterIndex = chapterIndex,
        pieces = pieces,
        pages = paginatePieces(pieces, usableWidthPx, usableHeightPx, paragraphSpacingPx),
    )
}

/** Height an image piece occupies at full [widthPx]; unknown aspect falls back to 3:4. */
internal fun imagePieceHeight(image: NovelBlock.Image, widthPx: Int, pageHeightPx: Int): Int {
    val natural = if (image.width > 0 && image.height > 0) {
        (widthPx.toLong() * image.height / image.width).toInt()
    } else {
        widthPx * 4 / 3
    }
    return natural.coerceIn(1, pageHeightPx)
}

private fun paginatePieces(
    pieces: List<NovelChapterPages.Piece>,
    usableWidthPx: Int,
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
        val pieceHeight = when {
            piece.layout != null -> piece.layout.size.height.toFloat()
            piece.image != null -> imagePieceHeight(piece.image, usableWidthPx, pageHeightPx).toFloat()
            else -> 0f
        }

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
            // A piece taller than the page is sliced by its own lines; each slice
            // opens a fresh page.
            val layout = piece.layout ?: continue
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
