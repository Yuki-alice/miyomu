package app.miyomu.source.novel

import app.miyomu.novel.NovelBlock
import app.miyomu.novel.NovelChapterData
import app.miyomu.novel.NovelContentSource

/** A book kept on the online shelf, addressed by its legado URLs. */
data class LegadoOnlineBook(
    val name: String,
    val author: String?,
    val bookUrl: String,
    val tocUrl: String,
    val coverUrl: String?,
    val sourceUrl: String?,
)

/**
 * [NovelContentSource] over a legado book source: the TOC is fetched once per session and
 * cached, chapter content is fetched on demand. The fetcher is injected so the whole path
 * is testable without network.
 */
class LegadoContentSource(
    private val source: LegadoBookSource,
    private val book: LegadoOnlineBook,
    private val fetch: suspend (String) -> String,
    js: LegadoJs,
) : NovelContentSource {

    private val reader = LegadoSourceReader(source, js)
    private var tocCache: List<LegadoTocChapter>? = null

    override suspend fun load(bookKey: String, chapterIndex: Int, offset: Int): NovelChapterData {
        val itemId = "legado:${source.bookSourceUrl.orEmpty()}:${book.bookUrl}"
        val fallback = NovelChapterData(
            itemId = itemId,
            entryId = "",
            chapterCount = 0,
            chapterIndex = chapterIndex,
            chapterTitle = null,
            chapterTitles = emptyList(),
            blocks = emptyList(),
            text = "",
            pendingOffset = 0,
        )
        return runCatching {
            val toc = tocCache ?: reader.toc(fetch(book.tocUrl), book.tocUrl).also { tocCache = it }
            if (toc.isEmpty()) return fallback.copy(error = "目录为空")

            val safeIndex = chapterIndex.coerceIn(0, toc.lastIndex)
            val chapter = toc[safeIndex]
            val text = chapterText(chapter.url)
                ?.takeIf { it.isNotBlank() }
                ?: return fallback.copy(
                    chapterCount = toc.size,
                    chapterTitles = toc.map { it.title },
                    error = "正文获取失败:${chapter.title}",
                )

            NovelChapterData(
                itemId = itemId,
                entryId = chapterEntryId(book.bookUrl, safeIndex),
                chapterCount = toc.size,
                chapterIndex = safeIndex,
                chapterTitle = chapter.title,
                chapterTitles = toc.map { it.title },
                blocks = listOf(NovelBlock.Text(0, text)),
                text = text,
                pendingOffset = if (safeIndex == chapterIndex) offset.coerceAtLeast(0) else 0,
            )
        }.getOrElse { failure ->
            fallback.copy(error = failure.message ?: failure.toString())
        }
    }

    private suspend fun chapterText(url: String): String? {
        val parts = mutableListOf<String>()
        val visited = mutableSetOf<String>()
        var current: String? = url
        while (current != null && parts.size < MaxContentPages && visited.add(current)) {
            val body = fetch(current)
            val text = reader.content(body, current)?.takeIf { it.isNotBlank() } ?: break
            parts += text
            current = reader.nextContentUrl(body, current)
        }
        return parts.joinToString("\n").takeIf { it.isNotBlank() }
    }

    companion object {
        private const val MaxContentPages = 20

        /** Progress key for an online chapter; positional like the local side. */
        fun chapterEntryId(bookUrl: String, chapterIndex: Int): String =
            "$bookUrl#$chapterIndex".hashCode().toString()
    }
}
