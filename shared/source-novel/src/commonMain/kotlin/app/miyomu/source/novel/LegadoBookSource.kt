package app.miyomu.source.novel

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive

/**
 * The legado(阅读) book-source JSON format — the de-facto extension standard of the
 * Chinese novel-source community (hundreds of community-maintained sources). This model
 * covers the read-path subset: search, book info, TOC and content rules. Legado itself
 * is GPL-3.0; this is an independent implementation driven by the documented JSON shape,
 * with no legado code (docs/11 §2).
 */
@Serializable
data class LegadoBookSource(
    val bookSourceUrl: String? = null,
    val bookSourceName: String? = null,
    val bookSourceGroup: String? = null,
    /** String form, or the newer object form with a `url` member. */
    val searchUrl: JsonElement? = null,
    val ruleSearch: LegadoRuleSet? = null,
    val ruleBookInfo: LegadoRuleSet? = null,
    val ruleToc: LegadoRuleSet? = null,
    val ruleContent: LegadoRuleSet? = null,
) {
    fun searchUrlTemplate(): String? = when (val value = searchUrl) {
        is JsonPrimitive -> value.takeIf { it.isString || it.content.isNotBlank() }?.content
        is JsonObject -> value["url"]?.jsonPrimitive?.content
        else -> null
    }

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
