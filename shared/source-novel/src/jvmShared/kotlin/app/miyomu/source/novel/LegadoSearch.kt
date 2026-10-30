package app.miyomu.source.novel

/**
 * One search page for [source]: fill the URL template, fetch through the platform
 * [fetcher], then evaluate the source's search rules.
 *
 * Shared by the per-source search screen and the cross-medium global search so both go
 * through the same rule path (and neither has to know about the QuickJS engine).
 *
 * @throws IllegalStateException when the source declares no search URL.
 */
suspend fun searchLegadoSource(
    source: LegadoBookSource,
    fetcher: suspend (String) -> String,
    query: String,
): List<LegadoSearchBook> {
    val reader = LegadoSourceReader(source, LegadoQuickJsEngine())
    val url = reader.searchUrl(query)
        ?: throw IllegalStateException("source has no search URL: ${source.bookSourceName}")
    return reader.search(fetcher(url), url)
}

/** One discovery category page for [source], evaluated through the same list rules as search. */
suspend fun exploreLegadoSource(
    source: LegadoBookSource,
    fetcher: suspend (String) -> String,
    category: LegadoExploreCategory,
    page: Int = 1,
): List<LegadoSearchBook> {
    val reader = LegadoSourceReader(source, LegadoQuickJsEngine())
    val url = reader.exploreUrl(category, page)
    return reader.explore(fetcher(url), url)
}

/** [LegadoSearchBook] → shelf/detail model; the TOC URL is only known after the detail load. */
fun LegadoSearchBook.toOnlineBook(sourceUrl: String?): LegadoOnlineBook = LegadoOnlineBook(
    name = name,
    author = author,
    bookUrl = bookUrl.orEmpty(),
    tocUrl = bookUrl.orEmpty(),
    coverUrl = coverUrl,
    sourceUrl = sourceUrl,
)
