package app.miyomu.novel

/**
 * One chapter of a parsed novel. [title] is `null` when the source document has no
 * usable heading; the UI layer falls back to a positional name.
 *
 * Implementations are lazy where possible: the TXT parser keeps char offsets into the
 * decoded file so chapter text is only materialized when [text] is called.
 */
interface NovelChapter {
    val title: String?

    fun text(): String
}

/**
 * A novel file parsed into chapters. Created by [TxtNovelParser] or [EpubNovelParser];
 * chapter identity is positional (index in [chapters]) and stable for an unchanged file.
 */
class ParsedNovel(
    val title: String,
    val author: String? = null,
    val coverBytes: ByteArray? = null,
    val chapters: List<NovelChapter> = emptyList(),
)

class NovelParseException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
