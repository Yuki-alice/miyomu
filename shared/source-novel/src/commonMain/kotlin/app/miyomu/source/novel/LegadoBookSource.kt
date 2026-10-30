package app.miyomu.source.novel

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

/**
 * The legado(阅读) book-source JSON format — the de-facto extension standard of the
 * Chinese novel-source community (hundreds of community-maintained sources). This model
 * covers the read-path subset: search, book info, TOC and content rules. Legado itself
 * is GPL-3.0; this is an independent implementation driven by the documented JSON shape,
 * with no legado code (docs/13 §2).
 */
@Serializable
data class LegadoBookSource(
    val bookSourceUrl: String? = null,
    val bookSourceName: String? = null,
    val bookSourceGroup: String? = null,
    /** String form, or the newer object form with a `url` member. */
    val searchUrl: JsonElement? = null,
    val ruleSearch: LegadoRuleSet? = null,
    /** Discovery categories: `分类::url` lines, or a JSON array/object of them. */
    val exploreUrl: JsonElement? = null,
    val ruleExplore: LegadoRuleSet? = null,
    val ruleBookInfo: LegadoRuleSet? = null,
    val ruleToc: LegadoRuleSet? = null,
    val ruleContent: LegadoRuleSet? = null,
) {
    fun searchUrlTemplate(): String? = when (val value = searchUrl) {
        is JsonPrimitive -> value.takeIf { it.isString || it.content.isNotBlank() }?.content
        is JsonObject -> value["url"]?.jsonPrimitive?.content
        else -> null
    }

    fun exploreCategories(): List<LegadoExploreCategory> =
        parseExploreValue(exploreUrl ?: return emptyList())

    companion object {
        private val json = Json { ignoreUnknownKeys = true; isLenient = true }

        fun fromJson(raw: String): LegadoBookSource = json.decodeFromString(serializer(), raw)

        /** Community source files hold either one object or an array of sources. */
        fun listFromJson(raw: String): List<LegadoBookSource> = when {
            raw.trimStart().startsWith("[") ->
                json.decodeFromString(kotlinx.serialization.builtins.ListSerializer(serializer()), raw)
            else -> listOf(fromJson(raw))
        }
    }
}

data class LegadoExploreCategory(val title: String, val url: String)

private fun parseExploreValue(value: JsonElement): List<LegadoExploreCategory> {
    return when (value) {
        is JsonPrimitive -> {
            val text = value.contentOrNull.orEmpty()
            val trimmed = text.trim()
            if (trimmed.startsWith("[") || trimmed.startsWith("{")) {
                runCatching { Json.parseToJsonElement(trimmed) }
                    .getOrNull()
                    ?.let(::parseExploreValue)
                    ?: parseExploreLines(text)
            } else {
                parseExploreLines(text)
            }
        }
        is JsonArray -> value.flatMap(::parseExploreValue)
        is JsonObject -> {
            val directUrl = value["url"]?.jsonPrimitive?.contentOrNull
            if (directUrl != null) {
                val title = value["title"]?.jsonPrimitive?.contentOrNull
                    ?: value["name"]?.jsonPrimitive?.contentOrNull
                    ?: return emptyList()
                listOfNotNull(exploreCategory(title, directUrl))
            } else {
                value.mapNotNull { (title, rawUrl) ->
                    val url = (rawUrl as? JsonPrimitive)?.contentOrNull ?: return@mapNotNull null
                    exploreCategory(title, url)
                }
            }
        }
        else -> emptyList()
    }
}

private fun parseExploreLines(text: String): List<LegadoExploreCategory> =
    text.lineSequence().mapNotNull(::parseExploreLine).toList()

private fun parseExploreLine(line: String): LegadoExploreCategory? {
    val trimmed = line.trim()
    if (trimmed.isEmpty()) return null
    val separator = when {
        trimmed.contains("::") -> "::"
        trimmed.contains("##") -> "##"
        else -> return null
    }
    val index = trimmed.indexOf(separator)
    return exploreCategory(
        trimmed.substring(0, index),
        trimmed.substring(index + separator.length),
    )
}

private fun exploreCategory(title: String, url: String): LegadoExploreCategory? {
    val name = title.trim()
    val link = url.trim()
    return if (name.isEmpty() || link.isEmpty()) null else LegadoExploreCategory(name, link)
}

/** Rule strings for one read stage; absent rules skip the stage. */
@Serializable
data class LegadoRuleSet(
    val bookList: String? = null,
    val name: String? = null,
    val author: String? = null,
    val bookUrl: String? = null,
    val coverUrl: String? = null,
    val intro: String? = null,
    val kind: String? = null,
    val lastChapter: String? = null,
    val tocUrl: String? = null,
    val chapterList: String? = null,
    val chapterName: String? = null,
    val chapterUrl: String? = null,
    val content: String? = null,
    val nextTocUrl: String? = null,
    val nextContentUrl: String? = null,
)
