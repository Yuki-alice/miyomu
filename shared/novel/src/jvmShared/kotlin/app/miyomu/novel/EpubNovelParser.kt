package app.miyomu.novel

import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.nodes.Document
import com.fleeksoft.ksoup.nodes.Element
import java.io.ByteArrayInputStream
import java.net.URLDecoder
import java.util.zip.ZipInputStream

/**
 * Parses an EPUB file into chapters: `META-INF/container.xml` → OPF package → spine
 * documents → plain text. Cover extraction follows EPUB 2 (`meta name="cover"`) and
 * EPUB 3 (`properties` containing `cover-image`). Paragraphs are the text of block
 * elements joined with newlines; CSS, images and footnotes are out of scope for v1
 * (see docs/10-novel-plan.md). Extraction behavior was cross-checked against Myne's
 * EPUB reader (Apache-2.0).
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
            val body = extractBody(doc) ?: return@mapIndexedNotNull null
            EpubChapter(
                title = docTitleOf(doc) ?: defaultTitle(index),
                value = body,
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

    private fun coverHrefOf(
        opf: Document,
        manifest: Map<String, ManifestItem>,
    ): String? {
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

    /**
     * Text of the body as block paragraphs joined with `\n`. Inline markup collapses
     * into the paragraph text; documents without block elements fall back to the
     * whole body text.
     */
    private fun extractBody(doc: Document): String? {
        val body = doc.body() ?: return null
        val blocks = body.select(BLOCK_SELECTOR)
        if (blocks.isEmpty()) {
            return body.text().trim().takeIf { it.isNotEmpty() }
        }
        return blocks.mapNotNull { it.text().trim().takeIf(String::isNotEmpty) }
            .joinToString("\n")
            .takeIf { it.isNotEmpty() }
    }

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
        private val value: String,
    ) : NovelChapter {
        override fun text(): String = value
    }

    private data class ManifestItem(val href: String, val mediaType: String, val properties: String)

    private fun defaultTitle(index: Int): String = "第 ${index + 1} 节"

    private companion object {
        const val CONTAINER_PATH = "META-INF/container.xml"
        const val DEFAULT_BOOK_TITLE = "EPUB"

        // Headings are excluded: the first one becomes the chapter title and would
        // otherwise duplicate it at the top of the body text.
        const val BLOCK_SELECTOR = "p, blockquote, li"
    }
}
