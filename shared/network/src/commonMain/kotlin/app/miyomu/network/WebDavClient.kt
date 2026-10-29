package app.miyomu.network

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * Minimal WebDAV client for backup sync (ADR: C3c). The endpoint is a full file URL the user
 * configures, e.g. `https://dav.example.com/miyomu/backup.json`; parent collection must exist.
 */
class WebDavClient(
    private val httpClient: HttpClient = HttpClient(),
) {

    suspend fun upload(
        fileUrl: String,
        username: String,
        password: String,
        bytes: ByteArray,
    ) {
        val response: HttpResponse = httpClient.put(fileUrl) {
            basicAuth(username, password)
            header(HttpHeaders.ContentType, "application/json")
            setBody(bytes)
        }
        if (!response.status.isSuccess() && response.status != HttpStatusCode.Created) {
            throw WebDavException("upload failed: HTTP ${response.status.value}")
        }
    }

    suspend fun download(
        fileUrl: String,
        username: String,
        password: String,
    ): ByteArray {
        val response: HttpResponse = httpClient.get(fileUrl) {
            basicAuth(username, password)
        }
        if (response.status == HttpStatusCode.NotFound) {
            throw WebDavException("remote backup does not exist yet")
        }
        if (!response.status.isSuccess()) {
            throw WebDavException("download failed: HTTP ${response.status.value}")
        }
        return response.bodyAsText().encodeToByteArray()
    }

    suspend fun exists(fileUrl: String, username: String, password: String): Boolean =
        runCatching { download(fileUrl, username, password) }
            .isSuccess

    @OptIn(ExperimentalEncodingApi::class)
    private fun io.ktor.client.request.HttpRequestBuilder.basicAuth(user: String, pass: String) {
        val token = Base64.encode("$user:$pass".encodeToByteArray())
        header(HttpHeaders.Authorization, "Basic $token")
    }
}

class WebDavException(message: String) : Exception(message)
