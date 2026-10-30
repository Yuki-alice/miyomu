package app.miyomu.local

import java.io.Closeable

/**
 * Random page access over a local PDF file. The comic pipeline consumes image bytes, so each
 * platform renders the requested page into a JPEG (white background, [maxDimension] bound on
 * the longer side) instead of exposing a document model.
 *
 * Implementations are not thread-safe; [FileLocalComicRepository] serializes reads through the
 * caller's IO dispatcher and must open/close per use or synchronize.
 */
interface PdfPageSource : Closeable {
    val pageCount: Int

    /** Renders [index] (0-based) to JPEG bytes, scaled so the longer side is at most [maxDimension]. */
    fun renderPage(index: Int, maxDimension: Int): ByteArray
}

/** Longer-side bound for rendered PDF pages; large enough for crisp reader pages without OOM. */
internal const val PDF_RENDER_MAX_DIMENSION: Int = 2048

internal const val PDF_LOCATION_PREFIX: String = "page:"

internal expect fun openPdfPageSource(path: String): PdfPageSource
