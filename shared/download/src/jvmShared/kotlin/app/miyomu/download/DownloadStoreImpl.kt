package app.miyomu.download

import app.miyomu.local.LocalBook
import app.miyomu.local.LocalBookFormat
import app.miyomu.local.LocalChapter
import app.miyomu.local.LocalPage
import app.miyomu.local.ReaderPageSource
import java.io.File

/**
 * Disk layout for downloaded chapters:
 *
 * ```
 * <root>/<itemId-hash>/<chapterKey-hash>/0000.jpg… + .complete (page count)
 * ```
 *
 * Keys are hashed, so arbitrary source paths/urls cannot escape the downloads root.
 */
class FileDownloadStore(
    private val root: File,
) : DownloadStore {

    override fun deleteChapter(itemId: String, chapterKey: String) {
        chapterDir(itemId, chapterKey).deleteRecursively()
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

    override fun pageSource(itemId: String, chapterKey: String, chapterTitle: String): ReaderPageSource? {
        if (!isDownloaded(itemId, chapterKey)) return null
        return DownloadedChapterPageSource(this, itemId, chapterKey, chapterTitle)
    }

    companion object {
        const val COMPLETE_MARKER = ".complete"
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
