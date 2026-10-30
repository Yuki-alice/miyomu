package app.miyomu.ui.navigation

import app.miyomu.source.novel.LegadoBookSource
import app.miyomu.source.novel.LegadoOnlineBook

/**
 * Legado's online models live in a JVM-only source set, so they cannot be `NavKey` fields
 * directly. These converters flatten them into the primitive fields the keys carry, and rebuild
 * them in the entry block. Keeping the mapping here means no screen has to know how a key is
 * shaped.
 */
fun LegadoOnlineBook.toDetailKey(): NovelOnlineDetailKey = NovelOnlineDetailKey(
    name = name,
    author = author,
    bookUrl = bookUrl,
    tocUrl = tocUrl,
    coverUrl = coverUrl,
    sourceUrl = sourceUrl,
)

fun LegadoOnlineBook.toReaderKey(chapterIndex: Int, charOffset: Int): NovelOnlineReaderKey =
    NovelOnlineReaderKey(
        name = name,
        author = author,
        bookUrl = bookUrl,
        tocUrl = tocUrl,
        coverUrl = coverUrl,
        sourceUrl = sourceUrl,
        chapterIndex = chapterIndex,
        charOffset = charOffset,
    )

fun NovelOnlineDetailKey.toOnlineBook(): LegadoOnlineBook = LegadoOnlineBook(
    name = name,
    author = author,
    bookUrl = bookUrl,
    tocUrl = tocUrl,
    coverUrl = coverUrl,
    sourceUrl = sourceUrl,
)

fun NovelOnlineReaderKey.toOnlineBook(): LegadoOnlineBook = LegadoOnlineBook(
    name = name,
    author = author,
    bookUrl = bookUrl,
    tocUrl = tocUrl,
    coverUrl = coverUrl,
    sourceUrl = sourceUrl,
)

/** Resolves a legado source by its url; falls back to a bare source so a page never crashes. */
fun legadoSourceFor(url: String?): LegadoBookSource = LegadoBookSource(bookSourceUrl = url)
