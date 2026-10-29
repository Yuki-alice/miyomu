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
            LocalBookFormat.VIDEO -> videoChapters(book)
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

    override suspend fun seasons(book: LocalBook): List<LocalBook> = withContext(Dispatchers.IO) {
        if (book.format != LocalBookFormat.VIDEO) return@withContext emptyList()
        videoSeasons(book)
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
            if (book.format == LocalBookFormat.VIDEO) return@runCatching videoCover(book)
            val chapter = chapters(book).firstOrNull() ?: return@runCatching null
            val page = pages(chapter).firstOrNull() ?: return@runCatching null
            readPage(page)
        }.getOrNull()
    }

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
