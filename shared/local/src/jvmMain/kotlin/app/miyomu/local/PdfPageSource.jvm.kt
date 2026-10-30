package app.miyomu.local

import java.io.ByteArrayOutputStream
import java.io.File
import javax.imageio.ImageIO
import org.apache.pdfbox.Loader
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.rendering.ImageType
import org.apache.pdfbox.rendering.PDFRenderer

/**
 * Desktop PDF rendering through Apache PDFBox. One document per source instance; rendering
 * synchronizes on the document because PDFBox is not thread-safe. Caching of open documents is
 * the repository's policy ([FileLocalComicRepository] keeps one open source at a time).
 */
internal actual fun openPdfPageSource(path: String): PdfPageSource = JvmPdfPageSource(path)

private class JvmPdfPageSource(path: String) : PdfPageSource {

    private val document: PDDocument = Loader.loadPDF(File(path))

    override val pageCount: Int
        get() = document.numberOfPages

    override fun renderPage(index: Int, maxDimension: Int): ByteArray {
        val image = synchronized(document) {
            val renderer = PDFRenderer(document)
            // Fit the longer side to maxDimension, computed from the 72dpi base page size.
            val page = document.getPage(index)
            val box = page.mediaBox
            val scale = minOf(
                maxDimension.toFloat() / box.width,
                maxDimension.toFloat() / box.height,
            )
            renderer.renderImage(index, scale, ImageType.RGB)
        }
        val bytes = ByteArrayOutputStream()
        ImageIO.write(image, "jpg", bytes)
        return bytes.toByteArray()
    }

    override fun close() {
        document.close()
    }
}
