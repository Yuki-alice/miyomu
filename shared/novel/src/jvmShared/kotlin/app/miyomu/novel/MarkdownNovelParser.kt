package app.miyomu.novel

/**
 * Markdown novel files (.md) — common for fan translations and serialized drafts. ATX
 * headings (`#`, `##`) start chapters; everything else is kept verbatim as text blocks
 * (Markdown is deliberately readable as plain text, so no further markup processing).
 */
class MarkdownNovelParser {

    fun parse(fileName: String, bytes: ByteArray): ParsedNovel {
        val text = TextDecode.decode(bytes)

        data class Draft(var title: String?, val lines: MutableList<String>)

        val drafts = mutableListOf<Draft>()
        var current = Draft(null, mutableListOf())

        for (line in text.lineSequence()) {
            val heading = HEADING_LINE.matchEntire(line.trimStart())
            if (heading != null) {
                if (current.lines.isNotEmpty() || current.title != null) drafts += current
                current = Draft(heading.groupValues[2].trim(), mutableListOf())
                continue
            }
            current.lines += line
        }
        if (current.lines.isNotEmpty() || current.title != null) drafts += current

        var offset = 0
        val chapters = drafts.mapNotNull { draft ->
            val body = draft.lines.joinToString("\n").trim { it == '\n' || it == '\r' }
            if (body.isBlank()) return@mapNotNull null
            val blocks = listOf(NovelBlock.Text(offset, body))
            offset += body.length + 1
            BlockChapter(title = draft.title, content = blocks)
        }

        val bookTitle = drafts.firstOrNull()?.title?.takeIf { it.isNotBlank() }
            ?: fileName.substringBeforeLast('.')
        return ParsedNovel(title = bookTitle, chapters = chapters)
    }

    companion object {
        const val BOOK_FALLBACK = "正文"

        private val HEADING_LINE = Regex("^(#{1,6})\\s+(.+?)\\s*#*\\s*$")
    }
}
