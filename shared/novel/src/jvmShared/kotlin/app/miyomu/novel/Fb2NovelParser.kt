package app.miyomu.novel

import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.nodes.Document
import com.fleeksoft.ksoup.nodes.Element
import java.util.Base64

/**
 * FictionBook 2 (.fb2): XML with `<section>` chapters and base64 `<binary>` images.
 * Images are referenced by fragment id (`<image l:href="#id">`); the cover page comes
 * from description/title-info/coverpage. Parsed through the shared HTML body walker.
 * Common in the Russian e-book scene and used by several light-novel communities.
 */
class Fb2NovelParser {

    fun parse(bytes: ByteArray): ParsedNovel {
        val doc = Ksoup.parse(decodeXml(bytes), "")

        val binaries = doc.getElementsByTag("binary").mapNotNull { el ->
            val id = el.attr("id")
            val contentType = el.attr("content-type").substringAfter('/').ifBlank { "img" }
            val decoded = runCatching { Base64.getMimeDecoder().decode(el.text().trim()) }
                .getOrNull()
                ?.takeIf { it.isNotEmpty() }
            if (id.isBlank() || decoded == null) null else id to (decoded to contentType)
        }.toMap()

        val imageResolver: (Element) -> RawBlock? = { el ->
            val href = el.attr("l:href").ifBlank { el.attr("xlink:href") }.ifBlank { el.attr("href") }
            val key = href.removePrefix("#")
            binaries[key]?.let { (imageBytes, contentType) ->
                val info = ImageHeaderSniffer.sniff(imageBytes)
                RawBlock.Image(
                    bytes = imageBytes,
                    format = info?.format ?: contentType,
                    width = info?.width ?: 0,
                    height = info?.height ?: 0,
                )
            }
        }

        val title = doc.getElementsByTag("book-title").firstOrNull()?.text()?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: DEFAULT_TITLE
        val author = authorOf(doc)

        // The HTML tree builder rewrites <image> to <img> (legacy quirk), so look for both.
        val coverBytes = doc.getElementsByTag("coverpage").firstOrNull()
            ?.let { it.getElementsByTag("img").firstOrNull() ?: it.getElementsByTag("image").firstOrNull() }
            ?.let(imageResolver)
            ?.let { it as? RawBlock.Image }?.bytes

        // The HTML-flavored parser wraps foreign markup in an implicit <body>, so chapters
        // are the top-level <section> elements anywhere in the document (no ancestor section).
        val sections = doc.getElementsByTag("section").filter { section ->
            section.parents().none { it.normalName() == "section" }
        }
        val chapters = if (sections.isEmpty()) {
            val sink = mutableListOf<RawBlock>()
            doc.getElementsByTag("body").firstOrNull()?.let { body ->
                body.children()
                    .filter { it.normalName() != "description" }
                    .forEach { HtmlBodyWalker.walk(it, imageResolver, sink) }
            }
            val content = assembleBlocks(sink)
            if (content.isEmpty()) {
                emptyList()
            } else {
                listOf(BlockChapter(title = null, content = content))
            }
        } else {
            sections.mapIndexed { index, section ->
                BlockChapter(
                    title = sectionTitle(section) ?: defaultTitle(index),
                    content = sectionBlocks(section, imageResolver),
                )
            }
        }

        return ParsedNovel(title = title, author = author, coverBytes = coverBytes, chapters = chapters)
    }

    private fun sectionBlocks(section: Element, imageResolver: (Element) -> RawBlock?): List<NovelBlock> {
        val sink = mutableListOf<RawBlock>()
        HtmlBodyWalker.walk(section, imageResolver, sink)
        return assembleBlocks(sink)
    }

    private fun sectionTitle(section: Element): String? {
        // The HTML tree builder treats <title> as RCDATA, so FB2's <title><p>…</p></title>
        // arrives as literal escaped markup — strip the tags back out.
        val raw = section.selectFirst("title")?.text()?.trim() ?: return null
        return raw.replace(TAG_REGEX, " ")
            .replace(WHITESPACE_REGEX, " ")
            .trim()
            .takeIf { it.isNotBlank() }
    }

    private fun authorOf(doc: Document): String? {
        val info = doc.getElementsByTag("author").firstOrNull() ?: return null
        val first = info.getElementsByTag("first-name").firstOrNull()?.text()?.trim().orEmpty()
        val last = info.getElementsByTag("last-name").firstOrNull()?.text()?.trim().orEmpty()
        val nickname = info.getElementsByTag("nickname").firstOrNull()?.text()?.trim().orEmpty()
        return listOf("$first $last".trim(), nickname).firstOrNull { it.isNotBlank() }
    }

    private fun decodeXml(bytes: ByteArray): String {
        val prolog = bytes.decodeToString(0, minOf(bytes.size, 200))
        val declared = Regex("encoding\\s*=\\s*[\"']([A-Za-z0-9_-]+)[\"']").find(prolog)?.groupValues?.get(1)
        return when {
            bytes.size >= 2 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xFE.toByte() ->
                String(bytes, 2, bytes.size - 2, Charsets.UTF_16LE)
            bytes.size >= 2 && bytes[0] == 0xFE.toByte() && bytes[1] == 0xFF.toByte() ->
                String(bytes, 2, bytes.size - 2, Charsets.UTF_16BE)
            declared != null -> runCatching { String(bytes, charset(declared)) }
                .getOrDefault(TextDecode.decode(bytes))
            else -> TextDecode.decode(bytes)
        }
    }

    private fun defaultTitle(index: Int): String = "第 ${index + 1} 节"

    private companion object {
        const val DEFAULT_TITLE = "FB2"
        val TAG_REGEX = Regex("<[^>]+>")
        val WHITESPACE_REGEX = Regex("\\s+")
    }
}
