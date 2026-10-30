package app.miyomu.novel

import app.miyomu.local.LocalBook
import app.miyomu.local.LocalBookFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Parses and caches local novel books. Parsing happens on demand (opening a book or
 * its detail page); a small LRU keeps recently opened books parsed so paging between
 * chapters does not re-read the file.
 *
 * Reads go through [File] like the comic repository does today; when Android SAF
 * arrives both repositories move to [app.miyomu.local.LocalFileRef] handles together.
 */
class LocalNovelRepository(
    private val txtParser: TxtNovelParser = TxtNovelParser(),
    private val epubParser: EpubNovelParser = EpubNovelParser(),
    private val fb2Parser: Fb2NovelParser = Fb2NovelParser(),
    private val htmlParser: HtmlNovelParser = HtmlNovelParser(),
    private val markdownParser: MarkdownNovelParser = MarkdownNovelParser(),
    private val cacheSize: Int = DEFAULT_CACHE_SIZE,
) {

    private val cacheMutex = Mutex()
    private val cache = object : LinkedHashMap<String, ParsedNovel>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, ParsedNovel>): Boolean =
            size > cacheSize
    }

    suspend fun parse(book: LocalBook): ParsedNovel = withContext(Dispatchers.IO) {
        val key = "${book.location}:${book.lastModified}"
        cacheMutex.withLock { cache[key] } ?: run {
            val parsed = parseUncached(book)
            cacheMutex.withLock { cache[key] = parsed }
            parsed
        }
    }

    suspend fun cover(book: LocalBook): ByteArray? =
        runCatching { parse(book).coverBytes }.getOrNull()

    suspend fun chapterText(book: LocalBook, chapterIndex: Int): String =
        parse(book).chapters[chapterIndex].text()

    private fun parseUncached(book: LocalBook): ParsedNovel = when (book.format) {
        LocalBookFormat.TXT -> txtParser.parse(book.title, File(book.location).readBytes())
        LocalBookFormat.EPUB -> epubParser.parse(File(book.location).readBytes())
        LocalBookFormat.FB2 -> fb2Parser.parse(File(book.location).readBytes())
        LocalBookFormat.HTML -> htmlParser.parse(book.title, File(book.location).readBytes())
        LocalBookFormat.MARKDOWN -> markdownParser.parse(book.title, File(book.location).readBytes())
        else -> throw NovelParseException("Unsupported novel format: ${book.format}")
    }

    companion object {
        const val DEFAULT_CACHE_SIZE = 3

        /**
         * Stable progress key for a chapter: book location plus chapter position,
         * hashed the same way the comic side keys chapters (A2-4). Positional identity
         * is stable for an unchanged file; see docs/00-current-status.md for the known
         * limitation around edited files.
         */
        fun chapterId(book: LocalBook, chapterIndex: Int): String =
            "${book.location}#${chapterIndex}".hashCode().toString()
    }
}
