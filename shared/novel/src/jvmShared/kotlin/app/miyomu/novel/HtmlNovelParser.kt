package app.miyomu.novel

import com.fleeksoft.ksoup.Ksoup

/**
 * Standalone .html/.htm novel files — the common form of fan-translated web novels.
 * The whole document becomes one chapter; scripts/styles are dropped and the body is
 * walked with the shared block walker. Referenced images are not resolved in v1
 * (a single HTML file has no reliable sibling context; see docs/12).
 */
class HtmlNovelParser {

    fun parse(fileName: String, bytes: ByteArray): ParsedNovel {
        val doc = Ksoup.parse(TextDecode.decode(bytes), "")
        doc.select("script, style, noscript").remove()

        val heading = doc.body()?.selectFirst("h1")?.text()?.trim()?.takeIf { it.isNotBlank() }
        val title = doc.selectFirst("title")?.text()?.trim()?.takeIf { it.isNotBlank() }
            ?: heading
            ?: fileName.substringBeforeLast('.')

        val sink = mutableListOf<RawBlock>()
        doc.body()?.let { body ->
            HtmlBodyWalker.walk(body, imageResolver = null, sink = sink)
        }
        if (sink.none { it is RawBlock.Text }) {
            val fallback = doc.body()?.text()?.trim().orEmpty()
            if (fallback.isNotBlank()) sink += RawBlock.Text(fallback)
        }
        val content = assembleBlocks(sink)

        val chapters = if (content.isEmpty()) {
            emptyList()
        } else {
            listOf(BlockChapter(title = heading, content = content))
        }
        return ParsedNovel(title = title, chapters = chapters)
    }
}
