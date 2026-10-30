package app.miyomu.novel

/**
 * Disk cache for online novel chapters (P1-2). The online content sources read through it:
 * a cached chapter is served without network, and every successful fetch is written back, so
 * "缓存全本" plus offline reading fall out of one port.
 *
 * Keys follow the download-store conventions: [itemId] buckets a book, [chapterKey] is the
 * chapter's stable identity (the legado chapter url).
 */
interface NovelChapterCache {
    fun isCached(itemId: String, chapterKey: String): Boolean

    fun read(itemId: String, chapterKey: String): String?

    fun write(itemId: String, chapterKey: String, text: String)
}
