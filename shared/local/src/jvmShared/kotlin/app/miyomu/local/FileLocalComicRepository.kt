package app.miyomu.local

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.zip.ZipFile

/**
 * Reads directories and ZIP/CBZ files through the JVM/JDK APIs available on both
 * Android and desktop. 7z/PDF/EPUB loaders are added in later slices.
 */
class FileLocalComicRepository : LocalComicRepository {

    override suspend fun chapters(book: LocalBook): List<LocalChapter> = withContext(Dispatchers.IO) {
        when (book.format) {
            LocalBookFormat.DIRECTORY -> directoryChapters(book)
            LocalBookFormat.ZIP -> listOf(
                LocalChapter(
                    id = book.location,
                    title = book.title,
                    bookLocation = book.location,
                    location = book.location,
                    format = LocalBookFormat.ZIP,
                    lastModified = book.lastModified,
                ),
            )
            else -> emptyList()
        }
    }

    override suspend fun pages(chapter: LocalChapter): List<LocalPage> = withContext(Dispatchers.IO) {
        when (chapter.format) {
            LocalBookFormat.DIRECTORY -> directoryPages(chapter)
            LocalBookFormat.ZIP -> zipPages(chapter)
            else -> emptyList()
        }
    }

    override suspend fun readPage(page: LocalPage): ByteArray = withContext(Dispatchers.IO) {
        val separator = page.location.indexOf(ARCHIVE_SEPARATOR)
        if (separator >= 0) {
            val archive = File(page.location.substring(0, separator))
            val entry = page.location.substring(separator + ARCHIVE_SEPARATOR.length)
            ZipFile(archive).use { zip ->
                zip.getInputStream(zip.getEntry(entry) ?: error("missing zip entry: $entry"))
                    .use { it.readBytes() }
            }
        } else {
            File(page.location).readBytes()
        }
    }

    override suspend fun cover(book: LocalBook): ByteArray? = withContext(Dispatchers.IO) {
        runCatching {
            val chapter = chapters(book).firstOrNull() ?: return@runCatching null
            val page = pages(chapter).firstOrNull() ?: return@runCatching null
            readPage(page)
        }.getOrNull()
    }

    private fun directoryChapters(book: LocalBook): List<LocalChapter> {
        val directory = File(book.location)
        if (!directory.isDirectory) return emptyList()

        val entries = directory.listFiles().orEmpty()
            .filterNot { it.name.startsWith('.') }

        val chapters = entries
            .filter { it.isDirectory || isComicArchive(it.name) }
            .map { file ->
                LocalChapter(
                    id = file.absolutePath,
                    title = if (file.isDirectory) file.name else file.name.substringBeforeLast('.'),
                    bookLocation = book.location,
                    location = file.absolutePath,
                    format = if (file.isDirectory) LocalBookFormat.DIRECTORY else LocalBookFormat.ZIP,
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

    private fun isComicArchive(name: String): Boolean = when (extensionOf(name)) {
        "zip", "cbz" -> true
        else -> false
    }

    private companion object {
        const val ARCHIVE_SEPARATOR = "!/"
    }
}
