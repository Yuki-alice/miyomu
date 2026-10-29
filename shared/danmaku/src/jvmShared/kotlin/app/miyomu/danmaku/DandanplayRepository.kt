package app.miyomu.danmaku

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * dandanplay open-API client (https://api.dandanplay.net). Requests must carry the developer
 * AppId plus an HMAC-SHA256 signature over `appId + timestamp + path`, so credentials are
 * user-configured in settings and danmaku stays off until then.
 */
class DandanplayRepository(
    private val httpClient: HttpClient,
    initialCredentials: DanmakuCredentials,
    private val baseUrl: String = "https://api.dandanplay.net",
    private val nowSeconds: () -> Long = { System.currentTimeMillis() / 1000 },
) : DanmakuRepository {

    private val json = Json { ignoreUnknownKeys = true }
    private val _credentials = MutableStateFlow(initialCredentials)
    override val credentials: StateFlow<DanmakuCredentials> = _credentials.asStateFlow()

    override suspend fun setCredentials(credentials: DanmakuCredentials) {
        _credentials.value = credentials
    }

    override suspend fun searchEpisodes(animeTitle: String): List<DanmakuEpisode> {
        val body = authorizedGet("/api/v2/search/episodes?anime=${urlEncode(animeTitle)}")
        val episodes = json.parseToJsonElement(body).jsonObject["animes"]?.jsonArray ?: return emptyList()
        return episodes.flatMap { anime ->
            val obj = anime.jsonObject
            val animeTitle = obj.stringField("animeTitle")
            (obj["episodes"]?.jsonArray ?: JsonArray(emptyList())).map { episode ->
                val eo = episode.jsonObject
                DanmakuEpisode(
                    episodeId = eo.stringField("episodeId"),
                    animeTitle = animeTitle,
                    episodeTitle = eo.stringField("episodeTitle"),
                )
            }
        }
    }

    override suspend fun loadTrack(episodeId: String): DanmakuTrack {
        val body = authorizedGet("/api/v2/comment/$episodeId?withRelated=true&chConvert=1")
        val root = json.parseToJsonElement(body).jsonObject
        // Comment objects: { "m": text, "p": "timeSeconds,mode,color,source" }.
        val comments = (root["comments"]?.jsonArray ?: JsonArray(emptyList())).mapNotNull { element ->
            val obj = element as? JsonObject ?: return@mapNotNull null
            val text = obj.stringField("m")
            if (text.isBlank()) return@mapNotNull null
            val parts = obj.stringField("p").split(",")
            val timeMs = parts.getOrNull(0)?.toDoubleOrNull()?.times(1000)?.toLong() ?: return@mapNotNull null
            val mode = parts.getOrNull(1)?.toIntOrNull() ?: 1
            val color = parts.getOrNull(2)?.toIntOrNull()?.and(0xFFFFFF) ?: 0xFFFFFF
            DanmakuComment(timeMs = timeMs, text = text, mode = mode, color = color)
        }.sortedBy { it.timeMs }
        return DanmakuTrack(episodeId = episodeId, comments = comments)
    }

    private suspend fun authorizedGet(path: String): String {
        val credentials = _credentials.value
        check(credentials.isConfigured) { "弹幕 API 未配置 AppId/AppSecret" }
        val timestamp = nowSeconds()
        val response = httpClient.get(baseUrl + path) {
            header("X-AppId", credentials.appId)
            header("X-Signature", sign(credentials, timestamp, path))
            header("X-Timestamp", timestamp.toString())
        }
        check(response.status.isSuccess()) { "弹幕 API 请求失败：HTTP ${response.status.value}" }
        return response.bodyAsText()
    }

    internal fun sign(credentials: DanmakuCredentials, timestampSeconds: Long, path: String): String {
        val data = credentials.appId + timestampSeconds + path
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(credentials.appSecret.encodeToByteArray(), "HmacSHA256"))
        return Base64.getEncoder().encodeToString(mac.doFinal(data.encodeToByteArray()))
    }

    private fun JsonObject.stringField(name: String): String =
        (this[name] as? JsonPrimitive)?.contentOrNull.orEmpty()

    private fun urlEncode(value: String): String =
        java.net.URLEncoder.encode(value, Charsets.UTF_8)
}
