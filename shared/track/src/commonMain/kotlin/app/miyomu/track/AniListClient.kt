package app.miyomu.track

import io.ktor.client.HttpClient
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlinx.serialization.json.put

/**
 * AniList GraphQL client (P1-9). Token-based (a personal access token from the AniList
 * account settings) because an in-app OAuth browser round trip is a separate slice; the
 * client is deliberately small: viewer check, search, and progress updates.
 */
class AniListClient(private val client: HttpClient) {

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun viewer(token: String): TrackUser = execute(token, VIEWER_QUERY, "Viewer") { data ->
        val viewer = data.getValue("Viewer").jsonObject
        TrackUser(
            id = viewer.getValue("id").jsonPrimitive.long.toString(),
            name = viewer.getValue("name").jsonPrimitive.content,
        )
    }

    suspend fun search(token: String, query: String): List<TrackSearchResult> =
        execute(token, buildJsonObject { put("query", SEARCH_QUERY); put("variables", buildJsonObject { put("search", query) }) }.toString(), "Search") { data ->
            data.getValue("Page").jsonObject.getValue("media").jsonArray.map { element ->
                val media = element.jsonObject
                TrackSearchResult(
                    id = media.getValue("id").jsonPrimitive.long,
                    title = media.getValue("title").jsonObject.getValue("romaji").jsonPrimitive.content,
                )
            }
        }

    /** Pushes [progress] (chapter number) onto the bound series' list entry. */
    suspend fun updateProgress(token: String, mediaId: Long, progress: Int) {
        execute(token, buildJsonObject {
            put("query", SAVE_PROGRESS_MUTATION)
            put("variables", buildJsonObject {
                put("mediaId", mediaId)
                put("progress", progress)
            })
        }.toString(), "SaveMediaListEntry") { data ->
            data.getValue("SaveMediaListEntry").jsonObject.getValue("progress").jsonPrimitive.int
        }
    }

    private suspend fun <T> execute(
        token: String,
        body: String,
        operation: String,
        parse: (JsonObject) -> T,
    ): T {
        if (token.isBlank()) throw TrackException("AniList 令牌为空，请先在 AniList 账户设置中生成")
        val response = runCatching {
            client.post(BASE_URL) {
                bearerAuth(token)
                contentType(ContentType.Application.Json)
                setBody(body)
            }
        }.getOrElse { throw TrackException("AniList 请求失败：${it.message}", it) }
        val text = runCatching { response.bodyAsText() }
            .getOrElse { throw TrackException("AniList 响应读取失败：${it.message}", it) }
        if (response.status.value >= 400) {
            throw TrackException("AniList 返回 ${response.status.value}：${text.take(200)}")
        }
        val root = runCatching { json.parseToJsonElement(text).jsonObject }
            .getOrElse { throw TrackException("AniList 响应解析失败", it) }
        root["errors"]?.let { errors ->
            throw TrackException("AniList 错误：${errors.toString().take(200)}")
        }
        val data = root.getValue("data").jsonObject
        if (!data.containsKey(operation)) {
            throw TrackException("AniList 响应缺少 $operation")
        }
        return parse(data)
    }

    private companion object {
        const val BASE_URL = "https://graphql.anilist.co"

        val VIEWER_QUERY = """
            query Viewer {
              Viewer { id name }
            }
        """.trimIndent()

        val SEARCH_QUERY = """
            query Search(${'$'}search: String) {
              Page(perPage: 10) {
                media(search: ${'$'}search, type: MANGA) {
                  id
                  title { romaji }
                }
              }
            }
        """.trimIndent()

        val SAVE_PROGRESS_MUTATION = """
            mutation SaveMediaListEntry(${'$'}mediaId: Int, ${'$'}progress: Int) {
              SaveMediaListEntry(mediaId: ${'$'}mediaId, progress: ${'$'}progress) {
                id
                progress
              }
            }
        """.trimIndent()
    }
}
