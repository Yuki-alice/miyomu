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

    /** Plain reading text: every [NovelBlock.Text] joined with newlines, images excluded. */
    fun text(): String

    /**
     * Ordered content blocks of the chapter. Char offsets in [NovelBlock.startChar] refer to
     * positions inside [text] (images carry the offset at which they appear). The default
     * wraps the whole chapter as one block, which is what formatless parsers produce.
     */
    fun blocks(): List<NovelBlock> = listOf(NovelBlock.Text(0, text()))
}

/**
 * A chapter content piece: either a paragraph of text or an inline illustration
 * (EPUB `<img>`, FB2 `<binary>`). [Image.width]/[height] may be 0 when the format
 * header could not be sniffed; the reader falls back to a default aspect ratio.
 */
sealed class NovelBlock {
    abstract val startChar: Int

    data class Text(override val startChar: Int, val text: String) : NovelBlock()

    class Image(
        override val startChar: Int,
        val bytes: ByteArray,
        val format: String,
        val width: Int,
        val height: Int,
    ) : NovelBlock() {
        override fun equals(other: Any?): Boolean = other is Image &&
            other.startChar == startChar &&
            other.format == format &&
            other.bytes.contentEquals(bytes)

        override fun hashCode(): Int = startChar * 31 + bytes.contentHashCode()

        override fun toString(): String =
            "NovelBlock.Image(startChar=$startChar, format=$format, size=${bytes.size}B, ${width}x$height)"
    }
}

/**
 * A novel file parsed into chapters. Created by the format parsers; chapter identity is
 * positional (index in [chapters]) and stable for an unchanged file.
 */
class ParsedNovel(
    val title: String,
    val author: String? = null,
    val coverBytes: ByteArray? = null,
    val chapters: List<NovelChapter> = emptyList(),
)

/** Generic chapter over pre-assembled blocks, used by the block-based format parsers. */
class BlockChapter(
    override val title: String?,
    private val content: List<NovelBlock>,
) : NovelChapter {
    override fun text(): String =
        content.filterIsInstance<NovelBlock.Text>().joinToString("\n") { it.text }

    override fun blocks(): List<NovelBlock> = content
}

class NovelParseException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
