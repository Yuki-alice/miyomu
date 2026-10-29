package app.miyomu.source.runtime

import app.miyomu.model.AnimeEpisode
import app.miyomu.model.ComicDetails
import app.miyomu.model.ComicSummary
import app.miyomu.model.ResolvedStream
import app.miyomu.model.ResolvedVideo
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

@Serializable
internal data class JsAnimeListDto(
    val list: List<JsAnimeItemDto> = emptyList(),
    val maxPage: Int? = null,
    val next: String? = null,
)

@Serializable
internal data class JsAnimeItemDto(
    val id: String = "",
    val title: String = "",
    val cover: String? = null,
    val subtitle: String? = null,
    val tags: List<String> = emptyList(),
)

@Serializable
internal data class JsAnimeDetailDto(
    val title: String = "",
    val cover: String? = null,
    val description: String? = null,
    val tags: JsonElement? = null,
    val updateTime: String? = null,
    val url: String? = null,
    val episodes: JsonElement? = null,
)

@Serializable
internal data class JsAnimeEpisodeDto(
    val key: String = "",
    val name: String = "",
    val url: String = "",
    val line: Int = 0,
    val number: Double? = null,
)

@Serializable
internal data class JsAnimeResolveDto(
    val urls: List<JsStreamDto> = emptyList(),
    val headers: Map<String, String> = emptyMap(),
)

@Serializable
internal data class JsStreamDto(
    val url: String = "",
    val quality: String? = null,
    val type: String = "hls",
)

@Serializable
internal data class AnimeResolvePayload(
    val episodeUrl: String,
)

internal fun JsAnimeItemDto.toSummary(): ComicSummary = ComicSummary(
    id = id,
    title = title,
    coverUrl = cover?.takeIf { it.isNotBlank() },
    author = subtitle?.takeIf { it.isNotBlank() },
    tags = tags,
)

internal fun JsAnimeDetailDto.toDetails(animeId: String): ComicDetails = ComicDetails(
    id = animeId,
    title = title,
    coverUrl = cover?.takeIf { it.isNotBlank() },
    description = description?.takeIf { it.isNotBlank() },
    tags = parseTags(tags),
    episodes = parseEpisodes(episodes),
    updateTime = updateTime?.takeIf { it.isNotBlank() },
    url = url?.takeIf { it.isNotBlank() },
)

internal fun JsAnimeResolveDto.toResolvedVideo(): ResolvedVideo = ResolvedVideo(
    streams = urls.asSequence()
        .map { it.trimmedStream() }
        .filter { it.url.isNotEmpty() }
        .toList(),
    headers = headers.filterKeys { it.isNotBlank() },
)

private fun JsStreamDto.trimmedStream(): ResolvedStream = ResolvedStream(
    url = url.trim(),
    quality = quality?.takeIf { it.isNotBlank() },
    type = type.trim().ifEmpty { "hls" },
)

/**
 * `episodes` is an array of `{key, name, url, line?, number?}`. Entries with no explicit key get
 * one derived from url+line so the data layer always has a stable identity.
 */
private fun parseEpisodes(element: JsonElement?): List<AnimeEpisode> {
    val array = element as? JsonArray ?: return emptyList()
    return array.mapIndexedNotNull { index, item ->
        val dto = (item as? JsonObject)?.let { obj ->
            JsAnimeEpisodeDto(
                key = obj.stringField("key"),
                name = obj.stringField("name"),
                url = obj.stringField("url"),
                line = (obj["line"] as? JsonPrimitive)?.contentOrNull?.toIntOrNull() ?: 0,
                number = (obj["number"] as? JsonPrimitive)?.contentOrNull?.toDoubleOrNull(),
            )
        } ?: return@mapIndexedNotNull null
        val key = dto.key.ifBlank { dto.url.ifBlank { "ep$index" } }
        val url = dto.url.ifBlank { key }
        AnimeEpisode(
            key = key,
            name = dto.name.ifBlank { key },
            url = url,
            line = dto.line,
            number = dto.number,
        )
    }
}

private fun JsonObject.stringField(name: String): String =
    (this[name] as? JsonPrimitive)?.contentOrNull.orEmpty()
