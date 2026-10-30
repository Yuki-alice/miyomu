package app.miyomu.local

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.compress.archivers.sevenz.SevenZFile
import java.io.File
import java.util.zip.ZipFile

/**
 * Reads directories, ZIP/CBZ and 7z/CB7 archives plus PDF documents through the JVM/JDK and
 * commons-compress APIs available on both Android and desktop. PDF pages are rendered to JPEG
 * by the platform source ([openPdfPageSource]); EPUB/TXT books are served by the novel module.
 */
class FileLocalComicRepository : LocalComicRepository {

    override suspend fun chapters(book: LocalBook): List<LocalChapter> = withContext(Dispatchers.IO) {
        when (book.format) {
            LocalBookFormat.DIRECTORY -> directoryChapters(book)
            LocalBookFormat.VIDEO -> videoChapters(book)
            LocalBookFormat.ZIP,
            LocalBookFormat.SEVEN_ZIP,
            LocalBookFormat.PDF,
            -> singleChapter(book)
            else -> emptyList()
        }
    }

    override suspend fun seasons(book: LocalBook): List<LocalBook> = withContext(Dispatchers.IO) {
        if (book.format != LocalBookFormat.VIDEO) return@withContext emptyList()
        videoSeasons(book)
    }

    override suspend fun pages(chapter: LocalChapter): List<LocalPage> = withContext(Dispatchers.IO) {
        when (chapter.format) {
            LocalBookFormat.DIRECTORY -> directoryPages(chapter)
            LocalBookFormat.ZIP -> zipPages(chapter)
            LocalBookFormat.SEVEN_ZIP -> sevenZipPages(chapter)
            LocalBookFormat.PDF -> pdfPages(chapter)
            else -> emptyList()
        }
    }

    override suspend fun readPage(page: LocalPage): ByteArray = withContext(Dispatchers.IO) {
        val separator = page.location.indexOf(ARCHIVE_SEPARATOR)
        if (separator < 0) return@withContext File(page.location).readBytes()

        val archive = File(page.location.substring(0, separator))
        val entry = page.location.substring(separator + ARCHIVE_SEPARATOR.length)
        when (extensionOf(archive.name)) {
            "7z", "cb7" -> readSevenZipEntry(archive, entry)
            "pdf" -> readPdfPage(archive, entry)
                ?: error("failed to render pdf page: ${archive.name}#$entry")
            else -> readZipEntry(archive, entry)
        }
    }

    override suspend fun delete(book: LocalBook) {
        withContext(Dispatchers.IO) {
            // Books are directories or single archive files under the library root.
            File(book.location).deleteRecursively()
        }
    }

    override suspend fun cover(book: LocalBook): ByteArray? = withContext(Dispatchers.IO) {
        runCatching {
            if (book.format == LocalBookFormat.VIDEO) return@runCatching videoCover(book)
            val chapter = chapters(book).firstOrNull() ?: return@runCatching null
            val page = pages(chapter).firstOrNull() ?: return@runCatching null
            readPage(page)
        }.getOrNull()
    }

    private fun singleChapter(book: LocalBook): List<LocalChapter> = listOf(
        LocalChapter(
            id = book.location,
            title = book.title,
            bookLocation = book.location,
            location = book.location,
            format = book.format,
            lastModified = book.lastModified,
        ),
    )

    /**
     * A video book has no pages; its episodes are the video files. A single file stands in as
     * its own episode so the detail screen has one code path.
     */
    private fun videoChapters(book: LocalBook): List<LocalChapter> {
        val file = File(book.location)
        if (file.isFile) {
            return listOf(
                LocalChapter(
                    id = book.location,
                    title = book.title,
                    bookLocation = book.location,
                    location = book.location,
                    format = LocalBookFormat.VIDEO,
                    lastModified = book.lastModified,
                ),
            )
        }
        if (!file.isDirectory) return emptyList()

        return file.listFiles().orEmpty()
            .filter { it.isFile && !it.name.startsWith('.') && isVideoName(it.name) }
            .sortedWith { left, right -> naturalCompare(left.name, right.name) }
            .map { episode ->
                LocalChapter(
                    id = episode.absolutePath,
                    title = episode.name.substringBeforeLast('.'),
                    bookLocation = book.location,
                    location = episode.absolutePath,
                    format = LocalBookFormat.VIDEO,
                    lastModified = episode.lastModified(),
                )
            }
    }

    private fun videoSeasons(book: LocalBook): List<LocalBook> {
        val directory = File(book.location)
        if (!directory.isDirectory) return emptyList()

        return directory.listFiles().orEmpty()
            .filter { it.isDirectory && !it.name.startsWith('.') }
            .filter { season ->
                season.listFiles().orEmpty().any { it.isFile && isVideoName(it.name) }
            }
            .sortedWith { left, right -> naturalCompare(left.name, right.name) }
            .map { season ->
                LocalBook(
                    id = season.absolutePath,
                    title = season.name,
                    format = LocalBookFormat.VIDEO,
                    mediaType = app.miyomu.model.MediaType.VIDEO,
                    location = season.absolutePath,
                    lastModified = season.lastModified(),
                )
            }
    }

    /** Uses `cover`/`folder`/`poster` images, or the same-named image next to a video file. */
    private fun videoCover(book: LocalBook): ByteArray? {
        val location = File(book.location)
        val directory = if (location.isDirectory) location else location.parentFile ?: return null
        val baseName = if (location.isDirectory) null else location.nameWithoutExtension

        val images = directory.listFiles().orEmpty()
            .filter { it.isFile && isImageName(it.name) }
        val preferred = images.firstOrNull {
            it.nameWithoutExtension.lowercase() in setOf("cover", "folder", "poster", "thumbnail")
        } ?: baseName?.let { base -> images.firstOrNull { it.nameWithoutExtension == base } }

        return preferred?.readBytes()
    }

    private fun directoryChapters(book: LocalBook): List<LocalChapter> {
        val directory = File(book.location)
        if (!directory.isDirectory) return emptyList()

        val entries = directory.listFiles().orEmpty()
            .filterNot { it.name.startsWith('.') }

        val chapters = entries
            .filter { it.isDirectory || archiveFormat(it.name) != null }
            .map { file ->
                LocalChapter(
                    id = file.absolutePath,
                    title = if (file.isDirectory) file.name else file.name.substringBeforeLast('.'),
                    bookLocation = book.location,
                    location = file.absolutePath,
                    format = if (file.isDirectory) {
                        LocalBookFormat.DIRECTORY
                    } else {
                        archiveFormat(file.name) ?: LocalBookFormat.ZIP
                    },
                    lastModified = file.lastModified(),
                )
            }
            .sortedWith { left, right -> naturalCompare(left.title, right.title) }

        if (chapters.isNotEmpty()) return chapters

        // A flat image directory is still a valid one-chapter book.
        val hasImages = entries.any { it.isFile && isImageName(it.name) }
        return if (hasImages) {
            listOf(
                LocalChapter(
                    id = book.location,
                    title = book.title,
                    bookLocation = book.location,
                    location = book.location,
                    format = LocalBookFormat.DIRECTORY,
                    lastModified = book.lastModified,
                ),
            )
        } else {
            emptyList()
        }
    }

    private fun directoryPages(chapter: LocalChapter): List<LocalPage> {
        val directory = File(chapter.location)
        if (!directory.isDirectory) return emptyList()
        return directory.listFiles().orEmpty()
            .filter { it.isFile && isImageName(it.name) }
            .sortedWith { left, right -> naturalCompare(left.name, right.name) }
            .mapIndexed { index, file ->
                LocalPage(index = index, name = file.name, location = file.absolutePath)
            }
    }

    private fun zipPages(chapter: LocalChapter): List<LocalPage> {
        val archive = File(chapter.location)
        if (!archive.isFile) return emptyList()
        return ZipFile(archive).use { zip ->
            zip.entries()
                .asSequence()
                .filter { !it.isDirectory && isImageName(it.name) }
                .sortedWith { left, right -> naturalCompare(left.name, right.name) }
                .mapIndexed { index, entry ->
                    LocalPage(
                        index = index,
                        name = entry.name,
                        location = "${archive.absolutePath}$ARCHIVE_SEPARATOR${entry.name}",
                    )
                }
                .toList()
        }
    }

    private fun sevenZipPages(chapter: LocalChapter): List<LocalPage> {
        val archive = File(chapter.location)
        if (!archive.isFile) return emptyList()
        return SevenZFile.Builder().setFile(archive).get().use { seven ->
            seven.entries
                .filter { !it.isDirectory && isImageName(it.name) }
                .sortedWith { left, right -> naturalCompare(left.name, right.name) }
                .mapIndexed { index, entry ->
                    LocalPage(
                        index = index,
                        name = entry.name,
                        location = "${archive.absolutePath}$ARCHIVE_SEPARATOR${entry.name}",
                    )
                }
        }
    }

    private fun readZipEntry(archive: File, entry: String): ByteArray =
        ZipFile(archive).use { zip ->
            zip.getInputStream(zip.getEntry(entry) ?: error("missing zip entry: $entry"))
                .use { it.readBytes() }
        }

    private fun readSevenZipEntry(archive: File, entry: String): ByteArray =
        SevenZFile.Builder().setFile(archive).get().use { seven ->
            val target = seven.entries.firstOrNull { it.name == entry }
                ?: error("missing 7z entry: $entry")
            seven.getInputStream(target).use { it.readBytes() }
        }

    /** Lists the PDF's pages as renderable entries, `null` when the document cannot be opened. */
    private fun pdfPages(chapter: LocalChapter): List<LocalPage> = withPdf(chapter.location) { pdf ->
        (0 until pdf.pageCount).map { index ->
            LocalPage(
                index = index,
                name = "page-${index + 1}",
                location = "${chapter.location}$ARCHIVE_SEPARATOR$PDF_LOCATION_PREFIX$index",
            )
        }
    } ?: emptyList()

    private fun readPdfPage(archive: File, entry: String): ByteArray? {
        val index = entry.removePrefix(PDF_LOCATION_PREFIX).toIntOrNull() ?: return null
        return withPdf(archive.absolutePath) { pdf ->
            require(index in 0 until pdf.pageCount) { "pdf page out of range: $index" }
            pdf.renderPage(index, PDF_RENDER_MAX_DIMENSION)
        }
    }

    /**
     * Serializes all PDF access behind one cached open source: the reader requests pages one at
     * a time, so a single slot covers the reading flow while bounding file descriptors.
     */
    private val pdfLock = Any()
    private var openPdfPath: String? = null
    private var openPdfSource: PdfPageSource? = null

    private fun <T> withPdf(path: String, block: (PdfPageSource) -> T): T? = synchronized(pdfLock) {
        try {
            val source = if (openPdfPath == path && openPdfSource != null) {
                openPdfSource!!
            } else {
                openPdfSource?.runCatching { close() }
                openPdfPageSource(path).also {
                    openPdfPath = path
                    openPdfSource = it
                }
            }
            block(source)
        } catch (t: Throwable) {
            null
        }
    }

    private fun archiveFormat(name: String): LocalBookFormat? = when (extensionOf(name)) {
        "zip", "cbz" -> LocalBookFormat.ZIP
        "7z", "cb7" -> LocalBookFormat.SEVEN_ZIP
        else -> null
    }

    private companion object {
        const val ARCHIVE_SEPARATOR = "!/"
    }
}
