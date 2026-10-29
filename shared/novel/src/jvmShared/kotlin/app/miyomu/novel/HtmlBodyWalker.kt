package app.miyomu.novel

import com.fleeksoft.ksoup.nodes.Element

/** A raw content piece collected while walking HTML markup, before offset assembly. */
internal sealed class RawBlock {
    data class Text(val text: String) : RawBlock()
    data class Image(val bytes: ByteArray, val format: String, val width: Int, val height: Int) : RawBlock()
}

/**
 * Assembles raw blocks into [NovelBlock]s with char offsets aligned to the plain-text
 * stream: every text block advances the offset by its length plus one joining newline;
 * images do not advance it.
 */
internal fun assembleBlocks(raws: List<RawBlock>): List<NovelBlock> {
    var offset = 0
    val blocks = mutableListOf<NovelBlock>()
    for (raw in raws) {
        when (raw) {
            is RawBlock.Text -> {
                blocks += NovelBlock.Text(offset, raw.text)
                offset += raw.text.length + 1
            }
            is RawBlock.Image -> blocks += NovelBlock.Image(
                startChar = offset,
                bytes = raw.bytes,
                format = raw.format,
                width = raw.width,
                height = raw.height,
            )
        }
    }
    return blocks
}

/**
 * Pre-order walk of HTML/XHTML markup shared by the EPUB, HTML and FB2 parsers: text
 * elements become paragraphs, `img`/`image` elements become illustrations resolved
 * through the caller's [imageResolver]. An empty body yields nothing.
 */
internal object HtmlBodyWalker {

    val TEXT_TAGS = setOf(
        "p", "h1", "h2", "h3", "h4", "h5", "h6", "blockquote", "li", "dd", "dt", "pre",
        "figcaption", "div", "section", "article", "aside", "header", "footer", "td",
        "subtitle", "v", "text-author", // FB2
    )

    private val BLOCK_TAGS = setOf(
        "p", "div", "section", "article", "figure", "header", "footer", "aside",
        "blockquote", "li", "ul", "ol", "table", "tr", "td", "th", "dl", "main", "nav",
        "center", "h1", "h2", "h3", "h4", "h5", "h6", "body",
        "poem", "stanza", "cite", "epigraph", // FB2 containers
    )

    fun walk(
        root: Element,
        imageResolver: ((Element) -> RawBlock?)?,
        sink: MutableList<RawBlock>,
    ) {
        walkElement(root, imageResolver, sink)
    }

    private fun walkElement(
        el: Element,
        imageResolver: ((Element) -> RawBlock?)?,
        sink: MutableList<RawBlock>,
    ) {
        val name = el.normalName()
        if (name == "img" || name == "image") {
            if (imageResolver != null) imageResolver(el)?.let(sink::add)
            return
        }
        val hasBlockChild = el.children().any { it.normalName() in BLOCK_TAGS }
        if (name in TEXT_TAGS && !hasBlockChild) {
            val text = el.text().trim()
            if (text.isNotBlank()) {
                sink += RawBlock.Text(text)
                return
            }
            // Empty paragraph that may still carry an illustration — fall through.
        }
        el.children().forEach { walkElement(it, imageResolver, sink) }
    }
}
