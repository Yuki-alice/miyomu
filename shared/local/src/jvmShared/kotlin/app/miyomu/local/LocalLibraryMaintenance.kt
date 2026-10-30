package app.miyomu.local

import org.apache.commons.compress.archivers.sevenz.SevenZFile
import java.io.File
import java.util.zip.ZipFile

/**
 * Disk-level health of the local shelves (P0-11): how much space the books take, which titles
 * exist more than once, which books point at files that are gone, and which container files
 * fail their format probe (P1-4 损坏文件扫描). Pure reporting — cleanup stays a user decision
 * in the settings UI.
 */
data class LibraryHealth(
    val totalBytes: Long,
    val bookCount: Int,
    /** Title groups with more than one book (same normalized title, different locations). */
    val duplicateGroups: List<List<LocalBook>>,
    /** Books whose location no longer exists on disk (deleted/moved outside the app). */
    val missingBooks: List<LocalBook>,
    /** Books whose container file cannot be opened by its format probe (corrupt/truncated). */
    val corruptBooks: List<LocalBook>,
) {
    val isHealthy: Boolean
        get() = duplicateGroups.isEmpty() && missingBooks.isEmpty() && corruptBooks.isEmpty()
}

class LocalLibraryMaintenance {

    fun audit(books: List<LocalBook>): LibraryHealth {
        var totalBytes = 0L
        val missing = mutableListOf<LocalBook>()
        val corrupt = mutableListOf<LocalBook>()
        for (book in books) {
            val file = File(book.location)
            if (!file.exists()) {
                missing += book
                continue
            }
            totalBytes += if (file.isDirectory) {
                file.walkBottomUp().filter { it.isFile }.sumOf { it.length() }
            } else {
                file.length()
            }
            if (file.isFile && !probe(book.format, file)) {
                corrupt += book
            }
        }

        val duplicates = books
            .groupBy { it.title.trim().lowercase() }
            .values
            .filter { it.size > 1 }

        return LibraryHealth(
            totalBytes = totalBytes,
            bookCount = books.size,
            duplicateGroups = duplicates,
            missingBooks = missing,
            corruptBooks = corrupt,
        )
    }

    /**
     * Format probes for the container-based books (P1-4): archives and PDFs are opened and
     * closed; EPUB is a ZIP; plain-text formats are only checked for readability — their
     * parsers own deeper validation on open.
     */
    private fun probe(format: LocalBookFormat, file: File): Boolean = runCatching {
        when (format) {
            LocalBookFormat.ZIP -> { ZipFile(file).use { }; true }
            LocalBookFormat.SEVEN_ZIP -> { SevenZFile.Builder().setFile(file).get().use { }; true }
            LocalBookFormat.PDF -> { openPdfPageSource(file.absolutePath).use { }; true }
            LocalBookFormat.EPUB -> { ZipFile(file).use { }; true }
            else -> file.canRead() && file.length() > 0
        }
    }.getOrDefault(false)

    companion object {
        fun formatBytes(bytes: Long): String = when {
            bytes >= GIB -> "%.1f GiB".format(bytes / GIB.toDouble())
            bytes >= MIB -> "%.1f MiB".format(bytes / MIB.toDouble())
            bytes >= KIB -> "%.0f KiB".format(bytes / KIB.toDouble())
            else -> "$bytes B"
        }

        private const val KIB = 1024L
        private const val MIB = 1024L * 1024
        private const val GIB = 1024L * 1024 * 1024
    }
}
