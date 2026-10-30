package app.miyomu.local

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteReadChannel
import java.io.File
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.io.TempDir
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail
import kotlin.test.assertTrue

/** P1-6 外部 feed 接入: entry parsing (relative hrefs, escaping) and streamed downloads. */
class OpdsRemoteTest {

    @TempDir
    lateinit var libraryRoot: File

    private val feed = """
        <?xml version="1.0" encoding="UTF-8"?>
        <feed xmlns="http://www.w3.org/2005/Atom">
          <id>urn:lib</id>
          <title>Remote Library</title>
          <entry>
            <id>urn:book:1</id>
            <title>A &amp; B 冒险记</title>
            <updated>2026-10-31T00:00:00Z</updated>
            <link rel="http://opds-spec.org/acquisition" href="files/a-b.cbz" type="application/zip"/>
          </entry>
          <entry>
            <id>urn:book:2</id>
            <title>Plain</title>
            <updated>2026-10-31T00:00:00Z</updated>
            <link rel="http://opds-spec.org/acquisition" href="/abs/plain.pdf" type="application/pdf"/>
          </entry>
        </feed>
    """.trimIndent()

    private fun client() = HttpClient(MockEngine { request ->
        when (request.url.toString()) {
            "https://example.test/feed" -> respond(
                feed,
                headers = headersOf(HttpHeaders.ContentType, "application/atom+xml"),
            )
            "https://example.test/files/a-b.cbz" -> respond(
                ByteReadChannel(byteArrayOf(1, 2, 3)),
                headers = headersOf(HttpHeaders.ContentType, "application/zip"),
            )
            else -> respond("", HttpStatusCode.NotFound)
        }
    })

    @Test
    fun `parses entries and resolves relative hrefs`() = runTest {
        val entries = fetchOpdsFeed(client(), "https://example.test/feed")

        assertEquals(2, entries.size)
        assertEquals("A & B 冒险记", entries[0].title)
        assertEquals("https://example.test/files/a-b.cbz", entries[0].downloadUrl)
        assertEquals("application/zip", entries[0].mime)
        assertEquals("https://example.test/abs/plain.pdf", entries[1].downloadUrl)
    }

    @Test
    fun `downloads stream into the library with conflict avoidance`() = runTest {
        val client = client()
        val entries = fetchOpdsFeed(client, "https://example.test/feed")

        val first = downloadOpdsBook(client, entries[0], libraryRoot)
        // `&` is a legal filename character; sanitize only replaces actually-illegal ones.
        assertEquals("A & B 冒险记.cbz", first.name)
        assertEquals(3, first.length())

        val second = downloadOpdsBook(client, entries[0], libraryRoot)
        assertEquals("A & B 冒险记 (1).cbz", second.name)
        assertTrue(first.readBytes().contentEquals(second.readBytes()))
    }

    @Test
    fun `follows rel next links across pages within the budget`() = runTest {
        val paginated = """
            <feed>
              <entry><title>第一页条目</title>
                <link rel="http://opds-spec.org/acquisition" href="/files/p1.epub" type="application/epub+zip"/>
                <link rel="next" href="/feed?page=2"/>
              </entry>
            </feed>
        """.trimIndent()
        val page2 = """
            <feed>
              <entry><title>第二页条目</title>
                <link rel="http://opds-spec.org/acquisition" href="/files/p2.epub" type="application/epub+zip"/>
              </entry>
            </feed>
        """.trimIndent()
        val fetched = mutableListOf<String>()
        val client = HttpClient(MockEngine { request ->
            fetched += request.url.toString()
            when (request.url.toString()) {
                "https://example.test/feed" -> respond(paginated, headers = headersOf(HttpHeaders.ContentType, "application/atom+xml"))
                "https://example.test/feed?page=2" -> respond(page2, headers = headersOf(HttpHeaders.ContentType, "application/atom+xml"))
                else -> respond("", HttpStatusCode.NotFound)
            }
        })

        val entries = fetchOpdsFeed(client, "https://example.test/feed")

        assertEquals(2, entries.size)
        assertEquals(listOf("第一页条目", "第二页条目"), entries.map { it.title })
        assertEquals(
            listOf("https://example.test/feed", "https://example.test/feed?page=2"),
            fetched,
        )
    }

    @Test
    fun `sends basic auth when credentials are provided`() = runTest {
        var authorization: String? = null
        val client = HttpClient(MockEngine { request ->
            authorization = request.headers[HttpHeaders.Authorization]
            respond(feed, headers = headersOf(HttpHeaders.ContentType, "application/atom+xml"))
        })

        fetchOpdsFeed(client, "https://example.test/feed", credentials = OpdsCredentials("user", "pass"))

        // base64("user:pass")
        assertEquals("Basic dXNlcjpwYXNz", authorization)
    }

    @Test
    fun `feed failures surface as errors`() = runTest {
        try {
            fetchOpdsFeed(client(), "https://example.test/missing")
            fail("expected IllegalStateException")
        } catch (error: IllegalStateException) {
            assertTrue("404" in error.message!!)
        }
    }
}
