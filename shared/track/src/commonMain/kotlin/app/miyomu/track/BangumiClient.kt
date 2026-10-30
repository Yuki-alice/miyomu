package app.miyomu.track

import io.ktor.client.HttpClient
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlinx.serialization.json.put

/**
 * Bangumi (bgm.tv) API v0 client (P1-9). Token-based like the AniList client; Bangumi
 * requires a descriptive User-Agent on every request. Book subjects only — a manga binding
 * maps to a Bangumi book subject and progress to its `ep_status`.
 */
class BangumiClient(private val client: HttpClient) {

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun viewer(token: String): TrackUser {
        val body = getJson(token, "$BASE_URL/v0/me")
        return TrackUser(
            id = body.getValue("id").jsonPrimitive.long.toString(),
            name = body.getValue("username").jsonPrimitive.content,
        )
    }

    suspend fun searchBooks(token: String, keyword: String): List<TrackSearchResult> {
        val body = postJson(
            token,
            "$BASE_URL/v0/search/subjects",
            buildJsonObject {
                put("keyword", keyword)
                put("filter", buildJsonObject {
                    put("type", buildJsonArray { add(kotlinx.serialization.json.JsonPrimitive(1)) })
                })
            },
        )
        return body.getValue("data").jsonArray.map { element ->
            val subject = element.jsonObject
            TrackSearchResult(
                id = subject.getValue("id").jsonPrimitive.long,
                title = subject.getValue("name_cn").jsonPrimitive.content
                    .ifBlank { subject.getValue("name").jsonPrimitive.content },
            )
        }
    }

    /** Updates the collection entry's episode progress (type 3 = 在读). */
    suspend fun updateBookProgress(token: String, subjectId: Long, episodes: Int) {
        val response = runCatching {
            client.patch("$BASE_URL/v0/users/-/collections/$subjectId") {
                auth(token)
                contentType(ContentType.Application.Json)
                setBody(buildJsonObject {
                    put("type", 3)
                    put("ep_status", episodes)
                }.toString())
            }
        }.getOrElse { throw TrackException("Bangumi 请求失败：${it.message}", it) }
        if (!response.status.isSuccess()) {
            throw TrackException("Bangumi 返回 ${response.status.value}：${response.bodyAsText().take(200)}")
        }
    }

    private suspend fun getJson(token: String, url: String): JsonObject =
        parse(send { client.get(url) { auth(token) } })

    private suspend fun postJson(token: String, url: String, payload: JsonObject): JsonObject =
        parse(
            send {
                client.post(url) {
                    auth(token)
                    contentType(ContentType.Application.Json)
                    setBody(payload.toString())
                }
            },
        )

    private suspend fun parse(response: HttpResponse): JsonObject {
        val text = runCatching { response.bodyAsText() }
            .getOrElse { throw TrackException("Bangumi 响应读取失败：${it.message}", it) }
        if (!response.status.isSuccess()) {
            throw TrackException("Bangumi 返回 ${response.status.value}：${text.take(200)}")
        }
        return runCatching { json.parseToJsonElement(text).jsonObject }
            .getOrElse { throw TrackException("Bangumi 响应解析失败", it) }
    }

    private suspend fun send(request: suspend () -> HttpResponse): HttpResponse =
        runCatching { request() }.getOrElse { throw TrackException("Bangumi 请求失败：${it.message}", it) }

    private fun io.ktor.client.request.HttpRequestBuilder.auth(token: String) {
        bearerAuth(token)
        header(HttpHeaders.UserAgent, "Miyomu/0.1 (https://github.com/miyomu)")
    }

    private companion object {
        const val BASE_URL = "https://api.bgm.tv"
    }
}
