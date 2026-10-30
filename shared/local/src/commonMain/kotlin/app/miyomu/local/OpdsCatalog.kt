package app.miyomu.local

import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * P1-6 OPDS 导出: generates an OPDS 1.2 acquisition feed over the local shelves and copies
 * the books into an export directory, so any OPDS-compatible reader can browse and download
 * the library. The export directory sits under the library root with a dot prefix, which the
 * scanner ignores — exports never re-enter the shelves.
 */
object OpdsCatalog {

    const val FEED_TYPE: String = "application/atom+xml;profile=opds-catalog:kind=acquisition"
    const val EXPORT_DIR_NAME: String = ".opds-export"

    /** Builds the acquisition feed; [books] is the merged comic+novel shelf. */
    fun buildAcquisitionFeed(
        title: String,
        books: List<LocalBook>,
        generatedAtMillis: Long,
    ): String {
        val updated = isoTime(generatedAtMillis)
        val entries = books.joinToString("\n") { book -> entry(book, generatedAtMillis) }
        return """
            |<?xml version="1.0" encoding="UTF-8"?>
            |<feed xmlns="http://www.w3.org/2005/Atom" xmlns:opds="http://opds-spec.org/2010/catalog">
            |  <id>urn:miyomu:library</id>
            |  <title>${escape(title)}</title>
            |  <updated>$updated</updated>
            |  <link rel="self" href="/catalog.xml" type="${escape(FEED_TYPE)}"/>
            |  $entries
            |</feed>
        """.trimMargin().replace("\n\n", "\n").let { if (entries.isEmpty()) it.trimEnd() else it }
    }

    private fun entry(book: LocalBook, generatedAtMillis: Long): String {
        val fileName = exportFileName(book)
        val mime = bookMimeTypeOf(book)
        return """
            |  <entry>
            |    <id>urn:miyomu:book:${escape(book.id)}</id>
            |    <title>${escape(book.title)}</title>
            |    <updated>${isoTime(if (book.lastModified > 0) book.lastModified else generatedAtMillis)}</updated>
            |    <link rel="http://opds-spec.org/acquisition" href="books/$fileName" type="${escape(mime)}"/>
            |  </entry>
        """.trimMargin()
    }

    /** Copies [books] into [root]/books and writes [root]/catalog.xml. Returns the root. */
    fun export(root: File, title: String, books: List<LocalBook>, nowMillis: Long): File {
        val booksDir = File(root, "books").apply { mkdirs() }
        books.forEach { book ->
            val source = File(book.location)
            if (!source.exists()) return@forEach
            val target = File(booksDir, exportFileName(book))
            if (source.isDirectory) {
                source.copyRecursively(target, overwrite = true)
            } else {
                source.copyTo(target, overwrite = true)
            }
        }
        File(root, "catalog.xml").writeText(buildAcquisitionFeed(title, books, nowMillis))
        return root
    }

    /**
     * Imports books from a previously exported directory back into the library root (P1-6:
     * 导出 → 转移该目录到另一台设备 → 放回 `.opds-export` → 导入). Parses only the feed this
     * module generates; foreign OPDS feeds are out of scope for this slice. Returns the
     * number of books copied.
     */
    fun import(exportRoot: File, libraryRoot: File): Int {
        val catalog = File(exportRoot, "catalog.xml")
        if (!catalog.isFile) return 0
        val booksDir = File(exportRoot, "books")
        if (!booksDir.isDirectory) return 0

        val hrefs = FILE_HREF.findAll(catalog.readText())
            .map { it.groupValues[1] }
            .toList()
        var imported = 0
        for (href in hrefs) {
            val source = File(booksDir, href.removePrefix("books/"))
            if (!source.isFile) continue
            val target = uniqueTarget(File(libraryRoot, source.name))
            if (source.copyTo(target, overwrite = false).length() > 0) imported++
        }
        return imported
    }

    /** Avoids overwriting existing shelf files by appending " (n)" before the extension. */
    private fun uniqueTarget(target: File): File {
        if (!target.exists()) return target
        var copy = 1
        while (true) {
            val base = target.nameWithoutExtension
            val extension = target.extension
            val candidate = File(target.parentFile, if (extension.isEmpty()) "$base ($copy)" else "$base ($copy).$extension")
            if (!candidate.exists()) return candidate
            copy++
        }
    }

    internal fun exportFileName(book: LocalBook): String {
        val safeTitle = book.title.replace(Regex("[\\\\/:*?\"<>|]"), "_")
        val extension = File(book.location).extension
        val suffix = if (extension.isEmpty()) "" else ".$extension"
        return "$safeTitle$suffix"
    }

    private fun bookMimeTypeOf(book: LocalBook): String = when (book.format) {
        LocalBookFormat.ZIP -> "application/zip"
        LocalBookFormat.SEVEN_ZIP -> "application/x-7z-compressed"
        LocalBookFormat.PDF -> "application/pdf"
        LocalBookFormat.EPUB -> "application/epub+zip"
        LocalBookFormat.TXT, LocalBookFormat.MARKDOWN -> "text/plain"
        LocalBookFormat.HTML -> "text/html"
        LocalBookFormat.MOBI -> "application/x-mobipocket-ebook"
        else -> "application/octet-stream"
    }

    internal fun escape(raw: String): String = raw
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;")

    /** href extraction for this module's own catalog shape: `href="books/<file>"`. */
    private val FILE_HREF = Regex("""href="books/([^"]+)"""")

    private fun isoTime(millis: Long): String =
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.format(Date(millis))
}
