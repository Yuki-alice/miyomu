package app.miyomu.download

import app.miyomu.local.LocalBook
import app.miyomu.local.LocalBookFormat
import app.miyomu.local.LocalChapter
import app.miyomu.local.LocalPage
import app.miyomu.local.ReaderPageSource
import app.miyomu.novel.NovelChapterCache
import java.io.File

/**
 * Disk layout for downloaded chapters:
 *
 * ```
 * <root>/<itemId-hash>/<chapterKey-hash>/0000.jpg… + .complete (page count)
 * ```
 *
 * The same layout also serves the novel chapter cache (P1-2): a cached novel chapter is
 * `chapter.txt` plus the `.complete` marker in the item/chapter hash directories.
 *
 * Keys are hashed, so arbitrary source paths/urls cannot escape the downloads root.
 */
class FileDownloadStore(
    private val root: File,
) : DownloadStore, NovelChapterCache {

    override fun deleteChapter(itemId: String, chapterKey: String) {
        chapterDir(itemId, chapterKey).deleteRecursively()
    }

    override fun clearAll() {
        root.listFiles()?.forEach { it.deleteRecursively() }
    }

    fun chapterDir(itemId: String, chapterKey: String): File =
        File(root, "${itemId.hash32()}/${chapterKey.hash32()}")

    override fun isDownloaded(itemId: String, chapterKey: String): Boolean =
        chapterDir(itemId, chapterKey).resolve(COMPLETE_MARKER).isFile

    fun markComplete(itemId: String, chapterKey: String, pageCount: Int) {
        chapterDir(itemId, chapterKey).resolve(COMPLETE_MARKER).writeText(pageCount.toString())
    }

    fun pageCount(itemId: String, chapterKey: String): Int =
        chapterDir(itemId, chapterKey).resolve(COMPLETE_MARKER)
            .takeIf { it.isFile }
            ?.readText()?.trim()?.toIntOrNull() ?: 0

    fun writePage(itemId: String, chapterKey: String, index: Int, bytes: ByteArray): File {
        val dir = chapterDir(itemId, chapterKey)
        dir.mkdirs()
        val target = dir.resolve("%04d".format(index) + ".jpg")
        val tmp = File(dir, target.name + ".tmp")
        tmp.writeBytes(bytes)
        if (!tmp.renameTo(target)) {
            target.writeBytes(bytes)
            tmp.delete()
        }
        return target
    }

    fun readPage(itemId: String, chapterKey: String, index: Int): ByteArray =
        chapterDir(itemId, chapterKey).resolve("%04d".format(index) + ".jpg").readBytes()

    fun pageFile(itemId: String, chapterKey: String, index: Int): File =
        chapterDir(itemId, chapterKey).resolve("%04d".format(index) + ".jpg")

    /** Free bytes on the volume holding the downloads root (P0-7 space guard). */
    fun usableBytes(): Long = root.usableSpace

    /** Target file for one downloaded video episode (P1-2 视频按集); ext: "mp4"/"ts". */
    fun episodeFile(itemId: String, episodeUrl: String, ext: String = "mp4"): File =
        chapterDir(itemId, "video:$episodeUrl").resolve("video.$ext")

    fun isEpisodeDownloaded(itemId: String, episodeUrl: String): Boolean =
        chapterDir(itemId, "video:$episodeUrl").resolve(COMPLETE_MARKER).isFile

    fun markEpisodeComplete(itemId: String, episodeUrl: String) {
        chapterDir(itemId, "video:$episodeUrl").resolve(COMPLETE_MARKER).writeText("1")
    }

    /** The downloaded episode file for offline playback, whichever container was used. */
    fun downloadedEpisodeFile(itemId: String, episodeUrl: String): File? =
        listOf("mp4", "ts").map { episodeFile(itemId, episodeUrl, it) }.firstOrNull { it.isFile }

    fun deleteEpisode(itemId: String, episodeUrl: String) {
        chapterDir(itemId, "video:$episodeUrl").deleteRecursively()
    }

    override fun isCached(itemId: String, chapterKey: String): Boolean =
        isDownloaded(itemId, chapterKey)

    override fun read(itemId: String, chapterKey: String): String? {
        val file = chapterDir(itemId, chapterKey).resolve(NOVEL_CHAPTER_FILE)
        return file.takeIf { it.isFile }?.readText()
    }

    override fun write(itemId: String, chapterKey: String, text: String) {
        val dir = chapterDir(itemId, chapterKey)
        dir.mkdirs()
        dir.resolve(NOVEL_CHAPTER_FILE).writeText(text)
        markComplete(itemId, chapterKey, 1)
    }

    /** Drops every cached novel chapter for one book (used by shelf removal). */
    fun deleteNovelBook(itemId: String) {
        File(root, itemId.hash32()).deleteRecursively()
    }

    /** Remembers a display title for the cached novel book (unified download center). */
    fun registerNovelBook(itemId: String, title: String) {
        val dir = chapterDir(itemId, "-").parentFile ?: return
        dir.mkdirs()
        File(dir, NOVEL_BOOK_NAME_FILE).writeText(title)
    }

    /** Inventory of cached novel books for the download center, title-first, then by itemId. */
    fun novelCachedBooks(): List<NovelCachedBook> =
        root.listFiles().orEmpty()
            .filter { it.isDirectory }
            .mapNotNull { itemDir ->
                val chapterCount = itemDir.listFiles().orEmpty()
                    .count { chapterDir -> File(chapterDir, NOVEL_CHAPTER_FILE).isFile }
                if (chapterCount == 0) return@mapNotNull null
                NovelCachedBook(
                    itemId = itemDir.name,
                    title = File(itemDir, NOVEL_BOOK_NAME_FILE)
                        .takeIf { it.isFile }
                        ?.readText()
                        ?.trim()
                        ?.ifBlank { null },
                    chapterCount = chapterCount,
                )
            }
            .sortedWith(compareBy({ it.title ?: "~" }, { it.itemId }))

    override fun pageSource(itemId: String, chapterKey: String, chapterTitle: String): ReaderPageSource? {
        if (!isDownloaded(itemId, chapterKey)) return null
        return DownloadedChapterPageSource(this, itemId, chapterKey, chapterTitle)
    }

    companion object {
        const val COMPLETE_MARKER = ".complete"
        const val NOVEL_CHAPTER_FILE = "chapter.txt"
        const val NOVEL_BOOK_NAME_FILE = ".name"
        const val MAX_PAGES = 2000
    }
}

private fun String.hash32(): String = "%08x".format(hashCode().toLong() and 0xFFFFFFFFL)

/** Serves a downloaded chapter from disk through the shared reader chrome. */
private class DownloadedChapterPageSource(
    private val store: FileDownloadStore,
    private val itemId: String,
    private val chapterKey: String,
    private val chapterTitle: String,
) : ReaderPageSource {

    private val book = LocalBook(
        id = itemId,
        title = chapterTitle,
        format = LocalBookFormat.DIRECTORY,
        mediaType = app.miyomu.model.MediaType.COMIC,
        location = itemId,
    )

    private val chapter = LocalChapter(
        id = chapterKey,
        title = chapterTitle,
        bookLocation = itemId,
        location = chapterKey,
        format = LocalBookFormat.DIRECTORY,
        lastModified = 0L,
    )

    override suspend fun chapters(book: LocalBook): List<LocalChapter> = listOf(chapter)

    override suspend fun pages(chapter: LocalChapter): List<LocalPage> {
        val count = store.pageCount(itemId, chapterKey)
        return (0 until count).map { index -> LocalPage(index = index, name = "第 ${index + 1} 页", location = index.toString()) }
    }

    override suspend fun readPage(page: LocalPage): ByteArray =
        store.readPage(itemId, chapterKey, page.index)
}
