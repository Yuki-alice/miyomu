package app.miyomu.novel

import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets

/**
 * Splits a plain-text novel file into chapters.
 *
 * Encoding: BOM first, then strict UTF-8, then GB18030 (covers GBK/GB2312).
 * Chapter detection: a short line that starts with `第…章/回/节/卷…` or one of the
 * well-known markers (序章/楔子/番外/Chapter N). Behavior reference: legado's chapter
 * heuristics (design/behavior only, no code copied). Files larger than [maxBytes]
 * are rejected instead of being decoded whole.
 */
class TxtNovelParser(
    val maxBytes: Long = DEFAULT_MAX_BYTES,
) {

    fun parse(fileName: String, bytes: ByteArray): ParsedNovel {
        if (bytes.size.toLong() > maxBytes) {
            throw NovelParseException(
                "TXT file is ${bytes.size} bytes, above the limit of $maxBytes; " +
                    "increase TxtNovelParser.maxBytes if this is expected",
            )
        }
        val text = decode(bytes)
        return ParsedNovel(
            title = fileName.substringBeforeLast('.'),
            chapters = splitChapters(text),
        )
    }

    private fun decode(bytes: ByteArray): String {
        when {
            bytes.size >= 3 &&
                bytes[0] == 0xEF.toByte() && bytes[1] == 0xBB.toByte() && bytes[2] == 0xBF.toByte() ->
                return String(bytes, 3, bytes.size - 3, StandardCharsets.UTF_8)

            bytes.size >= 2 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xFE.toByte() ->
                return String(bytes, 2, bytes.size - 2, StandardCharsets.UTF_16LE)

            bytes.size >= 2 && bytes[0] == 0xFE.toByte() && bytes[1] == 0xFF.toByte() ->
                return String(bytes, 2, bytes.size - 2, StandardCharsets.UTF_16BE)
        }

        try {
            return StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(bytes))
                .toString()
        } catch (_: CharacterCodingException) {
            // Not valid UTF-8; fall through to GB18030.
        }
        // GB18030 is a superset of GBK/GB2312 and the de-facto fallback for Chinese TXT.
        return String(bytes, charset("GB18030"))
    }

    private fun splitChapters(text: String): List<NovelChapter> {
        // A mark is a chapter-title line; body starts right after the line and runs to the
        // next mark. Offsets are tracked while scanning so chapters lazily substring the
        // decoded text instead of materializing it twice.
        data class Mark(val title: String, val lineStart: Int, val contentStart: Int)

        val raw = mutableListOf<Mark>()
        var lineStart = 0
        while (lineStart <= text.lastIndex) {
            val newline = text.indexOf('\n', lineStart)
            val lineEnd = if (newline == -1) text.length else newline
            val line = text.substring(lineStart, lineEnd).trimEnd('\r')
            val title = chapterTitleOf(line)
            if (title != null) raw += Mark(title, lineStart, lineStart + line.length)
            if (newline == -1) break
            lineStart = newline + 1
        }
        if (raw.isEmpty()) {
            return listOf(TxtChapter(text, BOOK_FALLBACK_TITLE, 0, text.length))
        }

        // A mark whose body (up to the next mark) is blank is a divider such as `第X卷`
        // or a stray heading: fold its title into the next mark instead of emitting an
        // empty chapter. A blank-bodied mark at the end of the file is dropped.
        data class Bound(val title: String, val lineStart: Int, val contentStart: Int)

        val bounds = mutableListOf<Bound>()
        val foldedTitle = StringBuilder()
        for ((index, mark) in raw.withIndex()) {
            val nextStart = raw.getOrNull(index + 1)?.lineStart ?: text.length
            foldedTitle.append(mark.title).append(' ')
            if (!text.substring(mark.contentStart, nextStart).isBlank()) {
                bounds += Bound(foldedTitle.toString().trim(), mark.lineStart, mark.contentStart)
                foldedTitle.setLength(0)
            }
        }
        if (bounds.isEmpty()) {
            return listOf(TxtChapter(text, BOOK_FALLBACK_TITLE, 0, text.length))
        }

        val chapters = mutableListOf<NovelChapter>()
        // Front matter runs to the first raw title line, even if that mark was folded
        // into a later chapter (e.g. a leading `第X卷` divider).
        if (text.substring(0, raw.first().lineStart).isNotBlank()) {
            chapters += TxtChapter(text, FRONT_MATTER_TITLE, 0, raw.first().lineStart)
        }
        for ((index, bound) in bounds.withIndex()) {
            val end = bounds.getOrNull(index + 1)?.lineStart ?: text.length
            chapters += TxtChapter(text, bound.title, bound.contentStart, end)
        }
        return chapters
    }

    private fun chapterTitleOf(line: String): String? {
        val trimmed = line.trim()
        if (trimmed.isEmpty() || trimmed.length > MAX_TITLE_LENGTH) return null
        return if (CHAPTER_LINE.matches(trimmed)) trimmed else null
    }

    private class TxtChapter(
        private val full: String,
        override val title: String?,
        private val start: Int,
        private val end: Int,
    ) : NovelChapter {
        override fun text(): String = full.substring(start, end).trim { it == '\n' || it == '\r' }
    }

    companion object {
        const val DEFAULT_MAX_BYTES: Long = 128L * 1024 * 1024
        const val FRONT_MATTER_TITLE = "开篇"
        const val BOOK_FALLBACK_TITLE = "正文"

        private const val MAX_TITLE_LENGTH = 60

        private val CHAPTER_LINE = Regex(
            "^(?:" +
                "第\\s*[0-9〇零一二三四五六七八九十百千万两]+\\s*[章回节卷集部].{0,36}|" +
                "序章.{0,36}|楔子.{0,36}|引子.{0,36}|尾声.{0,36}|终章.{0,36}|后记.{0,36}|番外.{0,36}|" +
                "Chapter\\s+\\d+.{0,36}" +
                ")$",
            RegexOption.IGNORE_CASE,
        )
    }
}
