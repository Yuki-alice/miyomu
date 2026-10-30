package app.miyomu.source.novel

import com.fleeksoft.ksoup.Ksoup
import java.net.URL

/**
 * The legado read path (search → book info → TOC → content) evaluated over already-fetched
 * response bodies, so the network layer stays thin and every rule evaluation is unit
 * testable. Responses may be HTML or JSON — community sources use both.
 */
class LegadoSourceReader(
    private val source: LegadoBookSource,
    private val js: LegadoJs,
) {

    private val evaluator = LegadoRuleEvaluator(js)

    // ---- URL building ----

    fun searchUrl(keyword: String, page: Int = 1): String? {
        val template = source.searchUrlTemplate() ?: return null
        val filled = template
            .replace("{{key}}", urlEncode(keyword))
            .replace("{{page}}", page.toString())
            .replace("{{pageSize}}", "20")
        return absolutize(filled, source.bookSourceUrl ?: return null)
    }

    fun exploreUrl(category: LegadoExploreCategory, page: Int = 1): String =
        absolutize(
            category.url.replace("{{page}}", page.toString()),
            source.bookSourceUrl.orEmpty(),
        )

    // ---- Search / explore ----

    suspend fun search(body: String, baseUrl: String): List<LegadoSearchBook> =
        books(source.ruleSearch, body, baseUrl)

    suspend fun explore(body: String, baseUrl: String): List<LegadoSearchBook> =
        books(source.ruleExplore, body, baseUrl)

    private suspend fun books(
        rules: LegadoRuleSet?,
        body: String,
        baseUrl: String,
    ): List<LegadoSearchBook> {
        rules ?: return emptyList()
        val result = mutableListOf<LegadoSearchBook>()
        for (item in itemList(rules.bookList, body)) {
            val name = evaluator.itemString(rules.name, item.element, item.jsonText, baseUrl)
                ?: continue
            val bookUrlRaw = evaluator.itemString(rule = rules.bookUrl, element = item.element, jsonText = item.jsonText, baseUrl = baseUrl)
            result += LegadoSearchBook(
                name = name,
                author = evaluator.itemString(rules.author, item.element, item.jsonText, baseUrl),
                bookUrl = bookUrlRaw?.let { absolutize(it, baseUrl) },
                coverUrl = evaluator.itemString(rules.coverUrl, item.element, item.jsonText, baseUrl)
                    ?.let { absolutize(it, baseUrl) },
                intro = evaluator.itemString(rules.intro, item.element, item.jsonText, baseUrl),
                latestChapter = evaluator.itemString(rules.lastChapter, item.element, item.jsonText, baseUrl),
            )
        }
        return result
    }

    // ---- Book info ----

    suspend fun bookInfo(body: String, baseUrl: String): LegadoBookInfo? {
        val rules = source.ruleBookInfo ?: return null
        val document = Ksoup.parse(body, baseUrl)
        suspend fun info(rule: String?) = evaluator.string(rule, document, baseUrl)
        val name = info(rules.name) ?: return null
        return LegadoBookInfo(
            name = name,
            author = info(rules.author),
            intro = info(rules.intro),
            coverUrl = info(rules.coverUrl)?.let { absolutize(it, baseUrl) },
            tocUrl = info(rules.tocUrl)?.let { absolutize(it, baseUrl) } ?: baseUrl,
            latestChapter = info(rules.lastChapter),
        )
    }

    // ---- TOC ----

    suspend fun toc(body: String, baseUrl: String): List<LegadoTocChapter> {
        val rules = source.ruleToc ?: return emptyList()
        return itemList(rules.chapterList, body).mapNotNull { item ->
            val (element, jsonText) = item
            val title = evaluator.itemString(rules.chapterName, element, jsonText, baseUrl)
                ?: return@mapNotNull null
            val url = evaluator.itemString(rules.chapterUrl, element, jsonText, baseUrl)
                ?.let { absolutize(it, baseUrl) }
                ?: return@mapNotNull null
            LegadoTocChapter(title = title, url = url)
        }
    }

    // ---- Content ----

    suspend fun content(body: String, baseUrl: String): String? {
        val rules = source.ruleContent ?: return null
        val (element, jsonText) = singleContext(body)
        val text = evaluator.itemString(rules.content, element, jsonText, baseUrl) ?: return null
        return text
            .split('\n')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .joinToString("\n")
    }

    /** Next page of a split chapter, resolved against the page that declared it. */
    suspend fun nextContentUrl(body: String, baseUrl: String): String? {
        val rule = source.ruleContent?.nextContentUrl ?: return null
        val (element, jsonText) = singleContext(body)
        val raw = evaluator.itemString(rule, element, jsonText, baseUrl) ?: return null
        val next = absolutize(raw, baseUrl)
        return next.takeIf {
            it != baseUrl && (it.startsWith("http://") || it.startsWith("https://"))
        }
    }

    // ---- Item context helpers ----

    private data class ItemContext(val element: com.fleeksoft.ksoup.nodes.Element?, val jsonText: String?)

    private suspend fun itemList(rule: String?, body: String): List<ItemContext> {
        val trimmed = body.trimStart()
        return if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
            evaluator.jsonList(rule, body).map { ItemContext(element = null, jsonText = it) }
        } else {
            val document = Ksoup.parse(body, "")
            evaluator.list(rule, document).map { ItemContext(element = it, jsonText = null) }
        }
    }

    private fun singleContext(body: String): ItemContext =
        if (body.trimStart().startsWith("{")) {
            ItemContext(element = null, jsonText = body)
        } else {
            ItemContext(element = Ksoup.parse(body, ""), jsonText = null)
        }

    private fun absolutize(url: String?, baseUrl: String): String {
        val raw = url?.trim().orEmpty()
        if (raw.isEmpty()) return baseUrl
        return try {
            URL(baseUrl).toURI().resolve(raw).toString()
        } catch (_: Exception) {
            raw
        }
    }

    private fun urlEncode(value: String): String =
        java.net.URLEncoder.encode(value, Charsets.UTF_8)
}

data class LegadoSearchBook(
    val name: String,
    val author: String?,
    val bookUrl: String?,
    val coverUrl: String?,
    val intro: String?,
    val latestChapter: String?,
)

data class LegadoBookInfo(
    val name: String,
    val author: String?,
    val intro: String?,
    val coverUrl: String?,
    val tocUrl: String,
    val latestChapter: String?,
)

data class LegadoTocChapter(
    val title: String,
    val url: String,
)
