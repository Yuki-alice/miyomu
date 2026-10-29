package app.miyomu.reader.novel

import app.miyomu.novel.NovelChapter

/**
 * In-book search across chapter titles and content. Chapter text comes from the cached
 * [app.miyomu.novel.ParsedNovel] (TXT keeps lazy offsets, EPUB is pre-extracted), so a
 * full-book scan is a bounded, user-triggered cost. Results carry the chapter index and
 * the char offset the reader jumps to.
 */
object NovelBookSearch {

    data class Match(
        val chapterIndex: Int,
        val offset: Int,
        /** Localized excerpt around the match with newlines collapsed. */
        val excerpt: String,
        /** True when the match hit the chapter title instead of the body. */
        val inTitle: Boolean,
    )

    fun search(
        chapters: List<NovelChapter>,
        rawQuery: String,
        maxResults: Int = DEFAULT_MAX_RESULTS,
        excerptRadius: Int = EXCERPT_RADIUS,
    ): List<Match> {
        val query = rawQuery.trim()
        if (query.isEmpty()) return emptyList()
        val lowerQuery = query.lowercase()

        val matches = mutableListOf<Match>()
        for ((index, chapter) in chapters.withIndex()) {
            val titleHit = chapter.title?.lowercase()?.contains(lowerQuery) == true
            if (titleHit) {
                matches += Match(index, 0, chapter.title.orEmpty(), inTitle = true)
            }

            val text = chapter.text()
            var from = 0
            while (matches.size < maxResults) {
                val at = text.lowercase().indexOf(lowerQuery, from)
                if (at < 0) break
                matches += Match(index, at, excerptOf(text, at, query.length, excerptRadius), inTitle = false)
                from = at + query.length
            }
            if (matches.size >= maxResults) break
        }
        return matches
    }

    private fun excerptOf(text: String, at: Int, matchLength: Int, radius: Int): String {
        val start = (at - radius).coerceAtLeast(0)
        val end = (at + matchLength + radius).coerceAtMost(text.length)
        val prefix = if (start > 0) "…" else ""
        val suffix = if (end < text.length) "…" else ""
        return prefix + text.substring(start, end).replace('\n', ' ') + suffix
    }

    const val DEFAULT_MAX_RESULTS = 50
    const val EXCERPT_RADIUS = 16
}
