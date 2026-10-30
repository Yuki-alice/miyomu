package app.miyomu.track

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandler
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.http.HttpMethod
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

class AniListClientTest {

    private fun client(handler: MockRequestHandler): AniListClient =
        AniListClient(HttpClient(MockEngine(handler)))

    @Test
    fun `viewer parses the account`() = runTest {
        var token: String? = null
        val ani = client { request ->
            token = request.headers[HttpHeaders.Authorization]
            respond(
                """{"data":{"Viewer":{"id":42,"name":"kurisu"}}}""",
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }

        val user = ani.viewer("secret-token")

        assertEquals("42", user.id)
        assertEquals("kurisu", user.name)
        assertEquals("Bearer secret-token", token)
    }

    @Test
    fun `updateProgress posts the mutation with variables`() = runTest {
        var body = ""
        var method: HttpMethod? = null
        val ani = client { request ->
            method = request.method
            body = (request.body as? io.ktor.http.content.TextContent)?.text.orEmpty()
            respond(
                """{"data":{"SaveMediaListEntry":{"id":7,"progress":12}}}""",
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }

        ani.updateProgress("tok", mediaId = 123, progress = 12)

        assertEquals(HttpMethod.Post, method)
        assertTrue("\"mediaId\":123" in body, "body: $body")
        assertTrue("\"progress\":12" in body)
        assertTrue("SaveMediaListEntry" in body)
    }

    @Test
    fun `graphql errors surface as TrackException`() = runTest {
        val ani = client {
            respond(
                """{"errors":[{"message":"Invalid token"}]}""",
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }

        try {
            ani.viewer("bad")
            fail("expected TrackException")
        } catch (error: TrackException) {
            assertTrue("Invalid token" in error.message!!)
        }
    }

    @Test
    fun `http failures surface as TrackException`() = runTest {
        val ani = client { respondError(HttpStatusCode.Unauthorized, "denied") }

        try {
            ani.viewer("bad")
            fail("expected TrackException")
        } catch (error: TrackException) {
            assertTrue("401" in error.message!!)
        }
    }
}
