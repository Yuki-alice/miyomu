package app.miyomu.novel

import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.nodes.Document
import com.fleeksoft.ksoup.nodes.Element
import java.io.ByteArrayInputStream
import java.net.URLDecoder
import java.util.zip.ZipInputStream

/**
 * Parses an EPUB file into chapters: `META-INF/container.xml` → OPF package → spine
 * documents → content blocks. Body markup is walked in document order so text paragraphs
 * and `<img>`/`<image>` illustrations interleave the way they appear in the book (the
 * light-novel case); image dimensions come from the file header so the reader can reserve
 * space before decoding. Cover extraction follows EPUB 2 (`meta name="cover"`) and EPUB 3
 * (`properties` containing `cover-image`). CSS, footnotes and CSS-based layout are out of
 * scope for v1 (docs/10). Extraction behavior was cross-checked against Myne's EPUB reader
 * (Apache-2.0).
 */
class EpubNovelParser {

    fun parse(bytes: ByteArray): ParsedNovel {
        val opfPath = zipEntryText(bytes, CONTAINER_PATH)?.let(::rootfileOf)
            ?: throw NovelParseException("EPUB: $CONTAINER_PATH missing or has no rootfile")

        val opfText = zipEntryText(bytes, opfPath)
            ?: throw NovelParseException("EPUB: OPF document not found at $opfPath")
        val opf = Ksoup.parse(opfText, "")

        val title = opf.getElementsByTag("dc:title").firstOrNull()?.text()?.takeIf { it.isNotBlank() }
        val author = opf.getElementsByTag("dc:creator").firstOrNull()?.text()?.takeIf { it.isNotBlank() }

        val manifest = opf.selectFirst("manifest")?.select("item").orEmpty()
            .mapNotNull { item ->
                val id = item.attr("id")
                val href = item.attr("href")
                if (id.isEmpty() || href.isEmpty()) null else {
                    id to ManifestItem(href, item.attr("media-type"), item.attr("properties"))
                }
            }.toMap()

        val spineHrefs = opf.selectFirst("spine")?.select("itemref").orEmpty()
            .mapNotNull { it.attr("idref") }
            .mapNotNull { idref -> manifest[idref]?.href }

        val baseDir = opfPath.substringBeforeLast('/', "")
        val chapters = spineHrefs.mapIndexedNotNull { index, href ->
            val resolved = resolveHref(baseDir, href)
            val docText = zipEntryText(bytes, resolved) ?: return@mapIndexedNotNull null
            val doc = Ksoup.parse(docText, "")
            val content = chapterBlocks(doc, resolved.substringBeforeLast('/', ""), bytes)
            if (content.isEmpty()) return@mapIndexedNotNull null
            EpubChapter(
                title = docTitleOf(doc) ?: defaultTitle(index),
                content = content,
            )
        }

        val coverHref = coverHrefOf(opf, manifest)
        val coverBytes = coverHref?.let { href ->
            runCatching { zipEntryBytes(bytes, resolveHref(baseDir, href)) }.getOrNull()
        }

        return ParsedNovel(
            title = title ?: DEFAULT_BOOK_TITLE,
            author = author,
            coverBytes = coverBytes,
            chapters = chapters,
        )
    }

    private fun rootfileOf(containerXml: String): String? =
        Ksoup.parse(containerXml, "")
            .selectFirst("rootfile")
            ?.attr("full-path")
            ?.takeIf { it.isNotBlank() }

    /** Pre-order walk of the spine document body, interleaving paragraphs and illustrations. */
    private fun chapterBlocks(doc: Document, baseDir: String, epubBytes: ByteArray): List<NovelBlock> {
        val body = doc.body() ?: return emptyList()
        val sink = mutableListOf<Raw>()
        walk(body, baseDir, epubBytes, sink)
        if (sink.none { it is Raw.Text }) {
            // Degenerate documents without recognizable block markup still keep their text.
            val fallback = body.text().trim()
            if (fallback.isNotBlank()) sink.add(Raw.Text(fallback))
        }

        var offset = 0
        val blocks = mutableListOf<NovelBlock>()
        for (raw in sink) {
            when (raw) {
                is Raw.Text -> {
                    blocks += NovelBlock.Text(offset, raw.text)
                    offset += raw.text.length + 1 // +1 for the joining newline in text()
                }
                is Raw.Image -> blocks += NovelBlock.Image(
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

    private fun walk(el: Element, baseDir: String, epubBytes: ByteArray, sink: MutableList<Raw>) {
        val name = el.normalName()
        if (name == "img" || name == "image") {
            imageOf(el, baseDir, epubBytes)?.let(sink::add)
            return
        }
        val hasBlockChild = el.children().any { it.normalName() in BLOCK_TAGS }
        if (name in TEXT_TAGS && !hasBlockChild) {
            val text = el.text().trim()
            if (text.isNotBlank()) {
                sink += Raw.Text(text)
                return
            }
            // Empty paragraph that may still carry an illustration — fall through.
        }
        el.children().forEach { walk(it, baseDir, epubBytes, sink) }
    }

    private fun imageOf(el: Element, baseDir: String, epubBytes: ByteArray): Raw.Image? {
        val src = el.attr("src").ifBlank { el.attr("xlink:href") }.ifBlank { el.attr("href") }
        if (src.isBlank()) return null
        val resolved = resolveHref(baseDir, src)
        val imageBytes: ByteArray = runCatching { zipEntryBytes(epubBytes, resolved) }
            .getOrNull()
            ?.takeIf { it.isNotEmpty() }
            ?: return null
        val info = ImageHeaderSniffer.sniff(imageBytes)
        return Raw.Image(
            bytes = imageBytes,
            format = info?.format ?: "img",
            width = info?.width ?: 0,
            height = info?.height ?: 0,
        )
    }

    private fun coverHrefOf(opf: Document, manifest: Map<String, ManifestItem>): String? {
        // EPUB 2: <meta name="cover" content="<manifest id>" />.
        val epub2Id = opf.select("meta")
            .firstOrNull { it.attr("name") == "cover" }
            ?.attr("content")
            ?.takeIf { it.isNotBlank() }
        val item2 = epub2Id?.let(manifest::get)
        val item3 = manifest.values.firstOrNull { "cover-image" in it.properties.split(' ') }
        return (item2 ?: item3)?.href
    }

    private fun docTitleOf(doc: Document): String? =
        doc.body()?.selectFirst("h1, h2, h3, h4")?.text()?.trim()?.takeIf { it.isNotEmpty() }
            ?: doc.selectFirst("title")?.text()?.trim()?.takeIf { it.isNotEmpty() }

    private fun resolveHref(baseDir: String, href: String): String {
        val decoded = runCatching { URLDecoder.decode(href, "UTF-8") }.getOrDefault(href)
        val path = decoded.substringBefore('#')
        return if (baseDir.isEmpty()) path else "$baseDir/$path"
    }

    private inline fun <T> zipWalk(bytes: ByteArray, onEntry: (String, ZipInputStream) -> T?): T? {
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                onEntry(entry.name, zip)?.let { return it }
                entry = zip.nextEntry
            }
        }
        return null
    }

    private fun zipEntryText(bytes: ByteArray, name: String): String? =
        zipWalk(bytes) { entryName, stream -> if (entryName == name) stream.readBytes() else null }
            ?.toString(Charsets.UTF_8)

    private fun zipEntryBytes(bytes: ByteArray, name: String): ByteArray? =
        zipWalk(bytes) { entryName, stream -> if (entryName == name) stream.readBytes() else null }

    private class EpubChapter(
        override val title: String?,
        private val content: List<NovelBlock>,
    ) : NovelChapter {
        override fun text(): String =
            content.filterIsInstance<NovelBlock.Text>().joinToString("\n") { it.text }

        override fun blocks(): List<NovelBlock> = content
    }

    private sealed class Raw {
        data class Text(val text: String) : Raw()
        data class Image(val bytes: ByteArray, val format: String, val width: Int, val height: Int) : Raw()
    }

    private data class ManifestItem(val href: String, val mediaType: String, val properties: String)

    private fun defaultTitle(index: Int): String = "第 ${index + 1} 节"

    private companion object {
        const val CONTAINER_PATH = "META-INF/container.xml"
        const val DEFAULT_BOOK_TITLE = "EPUB"

        val TEXT_TAGS = setOf(
            "p", "h1", "h2", "h3", "h4", "h5", "h6", "blockquote", "li", "dd", "dt", "pre",
            "figcaption", "div", "section", "article", "aside", "header", "footer", "td",
        )
        val BLOCK_TAGS = setOf(
            "p", "div", "section", "article", "figure", "header", "footer", "aside",
            "blockquote", "li", "ul", "ol", "table", "tr", "td", "th", "dl", "main", "nav",
            "center", "h1", "h2", "h3", "h4", "h5", "h6", "body",
        )
    }
}
