package app.miyomu.track

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandler
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

class BangumiClientTest {

    private fun client(handler: MockRequestHandler): BangumiClient =
        BangumiClient(HttpClient(MockEngine { request -> handler(request) }))

    @Test
    fun `viewer parses the account`() = runTest {
        var userAgent: String? = null
        val bangumi = client { request ->
            userAgent = request.headers[HttpHeaders.UserAgent]
            respond(
                """{"id":7,"username":"kurisu","nickname":"栗子"}""",
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }

        val user = bangumi.viewer("tok")

        assertEquals("7", user.id)
        assertEquals("kurisu", user.name)
        assertTrue(userAgent.orEmpty().startsWith("Miyomu/"), "UA: $userAgent")
    }

    @Test
    fun `searchBooks maps book subjects`() = runTest {
        val bangumi = client { request ->
            respond(
                """{"total":2,"data":[
                    {"id":100,"name":"Manga Original","name_cn":"漫画中文名"},
                    {"id":101,"name":"No Cn Name","name_cn":""}
                ]}""",
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }

        val results = bangumi.searchBooks("tok", "关键词")

        assertEquals(2, results.size)
        assertEquals(100, results[0].id)
        assertEquals("漫画中文名", results[0].title)
        assertEquals("No Cn Name", results[1].title)
    }

    @Test
    fun `updateBookProgress accepts a 204`() = runTest {
        val bangumi = client { respond("", HttpStatusCode.NoContent) }

        bangumi.updateBookProgress("tok", subjectId = 100, episodes = 12)
    }

    @Test
    fun `http failures surface as TrackException`() = runTest {
        val bangumi = client { respondError(HttpStatusCode.Unauthorized, "denied") }

        try {
            bangumi.viewer("bad")
            fail("expected TrackException")
        } catch (error: TrackException) {
            assertTrue("401" in error.message!!)
        }
    }
}
