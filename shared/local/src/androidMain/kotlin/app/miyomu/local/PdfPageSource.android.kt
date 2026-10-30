package app.miyomu.local

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import java.io.ByteArrayOutputStream
import java.io.File

/**
 * Platform PDF rendering through Android's [PdfRenderer] (API 21+). The document stays open for
 * the lifetime of the source; the comic pipeline renders pages sequentially through IO dispatch.
 */
internal actual fun openPdfPageSource(path: String): PdfPageSource = AndroidPdfPageSource(path)

private class AndroidPdfPageSource(path: String) : PdfPageSource {

    private val fileDescriptor: ParcelFileDescriptor =
        ParcelFileDescriptor.open(File(path), ParcelFileDescriptor.MODE_READ_ONLY)
    private val renderer = PdfRenderer(fileDescriptor)

    override val pageCount: Int
        get() = renderer.pageCount

    override fun renderPage(index: Int, maxDimension: Int): ByteArray {
        val page = renderer.openPage(index)
        try {
            // Fit the longer side to maxDimension; upscaling is allowed so small pages stay
            // readable on high-density screens.
            val scale = minOf(
                maxDimension.toFloat() / page.width,
                maxDimension.toFloat() / page.height,
            )
            val width = (page.width * scale).toInt().coerceAtLeast(1)
            val height = (page.height * scale).toInt().coerceAtLeast(1)

            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            bitmap.eraseColor(Color.WHITE)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

            val bytes = ByteArrayOutputStream().use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                out.toByteArray()
            }
            bitmap.recycle()
            return bytes
        } finally {
            page.close()
        }
    }

    override fun close() {
        renderer.close()
        fileDescriptor.close()
    }
}
