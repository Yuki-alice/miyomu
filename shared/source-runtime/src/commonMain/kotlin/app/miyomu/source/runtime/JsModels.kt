package app.miyomu.source.runtime

import app.miyomu.model.Chapter
import app.miyomu.model.ComicDetails
import app.miyomu.model.ComicSummary
import app.miyomu.model.PageRef
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

@Serializable
internal data class SourceMetaDto(
    val key: String = "",
    val name: String = "",
    val version: String = "",
    val minAppVersion: String? = null,
    val url: String? = null,
)

@Serializable
internal data class JsComicListDto(
    val comics: List<JsComicDto> = emptyList(),
    val maxPage: Int? = null,
    val next: String? = null,
)

@Serializable
internal data class JsComicDto(
    val id: String = "",
    val title: String = "",
    val cover: String? = null,
    val subtitle: String? = null,
    val tags: List<String> = emptyList(),
)

@Serializable
internal data class JsComicDetailsDto(
    val title: String = "",
    val cover: String? = null,
    val description: String? = null,
    val tags: JsonElement? = null,
    val chapters: JsonElement? = null,
    val maxPage: Int? = null,
    val thumbnails: List<String> = emptyList(),
    val updateTime: String? = null,
    val url: String? = null,
)

internal const val SingleChapterId = "__miyomu_single__"

@Serializable
internal data class JsPagesDto(
    val images: List<JsonElement> = emptyList(),
)

@Serializable
internal data class HostHttpResponse(
    val status: Int,
    val headers: Map<String, String>,
    val body: String,
)

@Serializable
internal data class HostHttpBytesMeta(
    val status: Int,
    val headers: Map<String, String>,
    val token: String,
)

@Serializable
internal data class SourceCookieDto(
    val name: String = "",
    val value: String = "",
    val domain: String? = null,
)

@Serializable
internal data class SearchPayload(
    val query: String,
    val page: Int,
)

@Serializable
internal data class DetailsPayload(
    val id: String,
)

@Serializable
internal data class PagesPayload(
    val comicId: String,
    val chapterId: String?,
)

internal fun JsComicDto.toSummary(): ComicSummary = ComicSummary(
    id = id,
    title = title,
    coverUrl = cover?.takeIf { it.isNotBlank() },
    author = subtitle?.takeIf { it.isNotBlank() },
    tags = tags,
)

internal fun JsComicDetailsDto.toDetails(comicId: String): ComicDetails {
    val parsedChapters = parseChapters(chapters)
    val resolvedChapters = if (parsedChapters.isEmpty() && (maxPage != null || thumbnails.isNotEmpty())) {
        // Single-gallery sources (e.g. hitomi) return maxPage/thumbnails but no chapters.
        listOf(Chapter(id = SingleChapterId, title = "1"))
    } else {
        parsedChapters
    }
    return ComicDetails(
        id = comicId,
        title = title,
        coverUrl = cover?.takeIf { it.isNotBlank() },
        description = description?.takeIf { it.isNotBlank() },
        tags = parseTags(tags),
        chapters = resolvedChapters,
        updateTime = updateTime?.takeIf { it.isNotBlank() },
        url = url?.takeIf { it.isNotBlank() },
    )
}

internal fun JsPagesDto.toPages(): List<PageRef> = images.mapNotNull { image ->
    when (image) {
        is JsonPrimitive -> image.contentOrNull
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?.let { PageRef(url = it) }
        is JsonObject -> {
            val url = image["url"]?.jsonPrimitiveContent()?.trim().orEmpty()
            if (url.isEmpty()) null else PageRef(url = url, headers = parsePageHeaders(image["headers"]))
        }
        else -> null
    }
}

private fun parsePageHeaders(element: JsonElement?): Map<String, String> {
    val headers = element as? JsonObject ?: return emptyMap()
    return headers.mapNotNull { (name, value) ->
        (value as? JsonPrimitive)?.contentOrNull?.let { name to it }
    }.toMap()
}

internal fun parseTags(element: JsonElement?): Map<String, List<String>> {
    val objectValue = element as? JsonObject ?: return emptyMap()
    return objectValue.mapValues { (_, value) ->
        when (value) {
            is JsonArray -> value.mapNotNull { (it as? JsonPrimitive)?.contentOrNull }
            is JsonPrimitive -> listOfNotNull(value.contentOrNull)
            else -> emptyList()
        }
    }
}

private fun parseChapters(element: JsonElement?): List<Chapter> = when (element) {
    null, JsonNull -> emptyList()
    is JsonObject -> element.flatMap { (groupOrId, value) ->
        when (value) {
            is JsonPrimitive -> listOfNotNull(
                value.contentOrNull?.let { Chapter(id = groupOrId, title = it) },
            )
            is JsonObject -> value.mapNotNull { (id, title) ->
                chapterTitle(title)?.let { Chapter(id = id, title = it, group = groupOrId) }
            }
            else -> emptyList()
        }
    }
    is JsonArray -> element.mapNotNull { item ->
        val chapter = item as? JsonObject ?: return@mapNotNull null
        val id = chapter["id"]?.jsonPrimitiveContent() ?: return@mapNotNull null
        Chapter(id = id, title = chapter["title"]?.jsonPrimitiveContent() ?: id)
    }
    else -> emptyList()
}

private fun chapterTitle(value: JsonElement): String? = when (value) {
    is JsonPrimitive -> value.contentOrNull
    is JsonObject -> value["title"]?.jsonPrimitiveContent() ?: value["name"]?.jsonPrimitiveContent()
    else -> null
}

private fun JsonElement.jsonPrimitiveContent(): String? = (this as? JsonPrimitive)?.contentOrNull
