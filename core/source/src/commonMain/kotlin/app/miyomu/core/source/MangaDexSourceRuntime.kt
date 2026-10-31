package app.miyomu.core.source

import io.ktor.client.HttpClient
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.skepsun.kototoro.core.source.SourceChapter
import org.skepsun.kototoro.core.source.SourceContent
import org.skepsun.kototoro.core.source.SourceDescriptor
import org.skepsun.kototoro.core.source.SourceDetailsFetchMode
import org.skepsun.kototoro.core.source.SourceFilter
import org.skepsun.kototoro.core.source.SourceFilterCapabilities
import org.skepsun.kototoro.core.source.SourceFilterOptions
import org.skepsun.kototoro.core.source.SourcePage
import org.skepsun.kototoro.core.source.SourcePagingMode
import org.skepsun.kototoro.core.source.SourceRef
import org.skepsun.kototoro.core.source.SourceRuntime
import org.skepsun.kototoro.core.source.SourceTag

/**
 * First real JSON source: MangaDex public API.
 *
 * It demonstrates the same SourceRuntime contract as DemoSourceRuntime, but with
 * network-backed list/details/chapters/pages. MangaDex uses UUIDs; the app model
 * keeps Long ids, so UUIDs are mapped to stable 64-bit hashes and kept in memory
 * for the lifetime of this runtime.
 */
class MangaDexSourceRuntime(
    private val client: HttpClient = defaultMangaDexClient(),
) : SourceRuntime {
    private val source = SourceRef(name = "mangadex", locale = "en", contentType = "MANGA")
    private val json = Json { ignoreUnknownKeys = true }
    private val mangaIds = mutableMapOf<Long, String>()
    private val chapterIds = mutableMapOf<Long, String>()

    override suspend fun getSources(): List<SourceRef> = listOf(source)

    override suspend fun describe(sourceName: String): SourceDescriptor = SourceDescriptor(
        source = source,
        sortOrders = setOf("FOLLOWED", "LATEST", "RELEVANCE"),
        defaultSortOrder = "FOLLOWED",
        filterCapabilities = SourceFilterCapabilities(
            isMultipleTagsSupported = true,
            isTagsExclusionSupported = false,
            isSearchSupported = true,
            isSearchWithFiltersSupported = false,
            isYearSupported = true,
            isYearRangeSupported = false,
            isOriginalLocaleSupported = false,
            isAuthorSearchSupported = false,
        ),
        pagingMode = SourcePagingMode.OFFSET,
        isImageFetchingSupported = false,
        homepage = "https://mangadex.org",
    )

    override suspend fun getFilterOptions(sourceName: String): SourceFilterOptions = SourceFilterOptions(
        availableTags = emptySet(),
        tagGroups = emptyList(),
        availableStates = emptySet(),
        availableContentRating = emptySet(),
        availableContentTypes = emptySet(),
        availableDemographics = emptySet(),
        availableLocales = emptySet(),
    )

    override suspend fun getList(
        sourceName: String,
        offset: Int,
        order: String?,
        filter: SourceFilter?,
    ): List<SourceContent> {
        val query = filter?.query?.trim().orEmpty()
        val response = client.get("$API/manga") {
            parameter("limit", 20)
            parameter("offset", offset)
            parameter("includes[]", "cover_art")
            parameter("includes[]", "author")
            parameter("contentRating[]", "safe")
            parameter("contentRating[]", "suggestive")
            if (query.isNotBlank()) parameter("title", query)
            when (order) {
                "LATEST" -> parameter("order[latestUploadedChapter]", "desc")
                "RELEVANCE" -> parameter("order[relevance]", "desc")
                else -> parameter("order[followedCount]", "desc")
            }
        }
        val data = json.parseToJsonElement(response.bodyAsText()).jsonObject["data"]?.jsonArray
            ?: return emptyList()
        return data.mapNotNull { it.toContent() }
    }

    override suspend fun getDetails(content: SourceContent, fetchMode: SourceDetailsFetchMode): SourceContent {
        val uuid = mangaIds[content.id] ?: return content
        val response = client.get("$API/manga/$uuid") {
            parameter("includes[]", "cover_art")
            parameter("includes[]", "author")
        }
        val detailed = json.parseToJsonElement(response.bodyAsText())
            .jsonObject["data"]?.toContent() ?: content
        val chapters = loadChapters(uuid)
        return detailed.copy(chapters = chapters)
    }

    override suspend fun getPages(chapter: SourceChapter, nextChapterUrl: String?): List<SourcePage> {
        val uuid = chapterIds[chapter.id] ?: chapter.url
        val response = client.get("$API/at-home/server/$uuid")
        val root = json.parseToJsonElement(response.bodyAsText()).jsonObject
        val baseUrl = root["baseUrl"]?.jsonPrimitive?.contentOrNull ?: return emptyList()
        val chapterNode = root["chapter"]?.jsonObject ?: return emptyList()
        val hash = chapterNode["hash"]?.jsonPrimitive?.contentOrNull ?: return emptyList()
        val files = chapterNode["data"]?.jsonArray ?: return emptyList()
        return files.mapIndexed { index, element ->
            val file = element.jsonPrimitive.content
            SourcePage(
                id = stableId("$uuid:$index"),
                url = "$baseUrl/data/$hash/$file",
                preview = null,
                source = source,
                headers = mapOf("Referer" to "https://mangadex.org/"),
            )
        }
    }

    override suspend fun getPageUrl(page: SourcePage): String = page.url

    override suspend fun getRelated(content: SourceContent): List<SourceContent> = emptyList()

    private suspend fun loadChapters(uuid: String): List<SourceChapter> {
        val response = client.get("$API/manga/$uuid/feed") {
            parameter("translatedLanguage[]", "en")
            parameter("order[chapter]", "desc")
            parameter("limit", 100)
            parameter("includes[]", "scanlation_group")
        }
        val data = json.parseToJsonElement(response.bodyAsText()).jsonObject["data"]?.jsonArray
            ?: return emptyList()
        return data.mapNotNull { element ->
            val obj = element.jsonObject
            val chapterUuid = obj["id"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
            val attrs = obj["attributes"]?.jsonObject ?: return@mapNotNull null
            val number = attrs["chapter"]?.jsonPrimitive?.contentOrNull?.toFloatOrNull() ?: 0f
            val chapter = SourceChapter(
                id = stableId(chapterUuid),
                title = attrs["title"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
                    ?: "Chapter $number",
                number = number,
                volume = attrs["volume"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: 0,
                url = chapterUuid,
                scanlator = obj["relationships"]?.jsonArray
                    ?.firstOrNull { it.jsonObject["type"]?.jsonPrimitive?.contentOrNull == "scanlation_group" }
                    ?.jsonObject?.get("attributes")?.jsonObject?.get("name")?.jsonPrimitive?.contentOrNull,
                uploadDate = 0L,
                branch = attrs["translatedLanguage"]?.jsonPrimitive?.contentOrNull,
                source = source,
            )
            chapterIds[chapter.id] = chapterUuid
            chapter
        }
    }

    private fun JsonElement.toContent(): SourceContent? {
        val obj = jsonObject
        val uuid = obj["id"]?.jsonPrimitive?.contentOrNull ?: return null
        val attrs = obj["attributes"]?.jsonObject ?: return null
        val title = attrs["title"]?.jsonObject?.values?.firstOrNull()?.jsonPrimitive?.contentOrNull
            ?: return null
        val relationships = obj["relationships"]?.jsonArray ?: JsonArray(emptyList())
        val coverFile = relationships
            .firstOrNull { it.jsonObject["type"]?.jsonPrimitive?.contentOrNull == "cover_art" }
            ?.jsonObject?.get("attributes")?.jsonObject?.get("fileName")?.jsonPrimitive?.contentOrNull
        val authors = relationships
            .filter { it.jsonObject["type"]?.jsonPrimitive?.contentOrNull == "author" }
            .mapNotNull {
                it.jsonObject["attributes"]?.jsonObject?.get("name")?.jsonPrimitive?.contentOrNull
            }
            .toSet()
        val tags = attrs["tags"]?.jsonArray?.mapNotNull { tag ->
            val tagObj = tag.jsonObject
            val name = tagObj["attributes"]?.jsonObject?.get("name")?.jsonObject
                ?.get("en")?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
            SourceTag(title = name, key = tagObj["id"]?.jsonPrimitive?.contentOrNull ?: name, source = source)
        }?.toSet() ?: emptySet()
        val id = stableId(uuid)
        mangaIds[id] = uuid
        return SourceContent(
            id = id,
            title = title,
            altTitles = attrs["altTitles"]?.jsonArray?.mapNotNull {
                it.jsonObject.values.firstOrNull()?.jsonPrimitive?.contentOrNull
            }?.toSet() ?: emptySet(),
            url = "https://mangadex.org/title/$uuid",
            publicUrl = "https://mangadex.org/title/$uuid",
            rating = 0f,
            contentRating = attrs["contentRating"]?.jsonPrimitive?.contentOrNull,
            coverUrl = coverFile?.let { "https://uploads.mangadex.org/covers/$uuid/$it.512.jpg" },
            tags = tags,
            state = attrs["status"]?.jsonPrimitive?.contentOrNull,
            authors = authors,
            source = source,
            description = attrs["description"]?.jsonObject?.get("en")?.jsonPrimitive?.contentOrNull,
        )
    }

    private fun stableId(value: String): Long {
        var hash = 1125899906842597L
        for (char in value) hash = 31 * hash + char.code
        return hash
    }

    private companion object {
        const val API = "https://api.mangadex.org"
    }
}

internal fun defaultMangaDexClient(): HttpClient = HttpClient {
    defaultRequest {
        header(HttpHeaders.UserAgent, "Miyomu/0.1 (https://github.com/miyomu)")
        header(HttpHeaders.Accept, "application/json")
    }
}
