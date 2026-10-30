package app.miyomu.download

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandler
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

/** P1-2 HLS 切片: media playlist concat, master-variant descent and empty-playlist errors. */
class HlsDownloaderTest {

    @TempDir
    lateinit var root: File

    private val mediaPlaylist = """
        #EXTM3U
        #EXT-X-TARGETDURATION:4
        #EXTINF:4.0,
        seg-1.ts
        #EXTINF:4.0,
        /seg/seg-2.ts
        #EXT-X-ENDLIST
    """.trimIndent()

    private fun client(handler: MockRequestHandler): HttpClient =
        HttpClient(MockEngine(handler))

    private fun serve(bodies: Map<String, String>): HttpClient = client { request ->
        val url = request.url.toString()
        val body = bodies[url]
        if (body == null) {
            respond("", HttpStatusCode.NotFound)
        } else {
            respond(
                body,
                headers = io.ktor.http.headersOf(
                    io.ktor.http.HttpHeaders.ContentType to listOf("application/vnd.apple.mpegurl"),
                ),
            )
        }
    }

    @Test
    fun `concatenates media segments in order`() = runTest {
        val bodies = mapOf(
            "https://cdn.test/ep.m3u8" to mediaPlaylist,
            "https://cdn.test/seg-1.ts" to "AAAA",
            "https://cdn.test/seg/seg-2.ts" to "BBBB",
        )
        val target = File(root, "video.ts")

        val bytes = downloadHls(serve(bodies), "https://cdn.test/ep.m3u8", target) { }

        assertEquals(8L, bytes)
        assertEquals("AAAABBBB", target.readText())
    }

    @Test
    fun `descends through a master playlist to the first variant`() = runTest {
        val master = """
            #EXTM3U
            #EXT-X-STREAM-INF:BANDWIDTH=2000000
            720p/playlist.m3u8
        """.trimIndent()
        val variant = """
            #EXTM3U
            #EXTINF:2.0,
            https://cdn.test/only.ts
        """.trimIndent()
        val bodies = mapOf(
            "https://cdn.test/master.m3u8" to master,
            "https://cdn.test/720p/playlist.m3u8" to variant,
            "https://cdn.test/only.ts" to "XY",
        )
        val target = File(root, "video.ts")

        val bytes = downloadHls(serve(bodies), "https://cdn.test/master.m3u8", target) { }

        assertEquals(2L, bytes)
        assertEquals("XY", target.readText())
    }

    @Test
    fun `empty playlist surfaces an error`() = runTest {
        val target = File(root, "video.ts")

        try {
            downloadHls(
                serve(mapOf("https://cdn.test/empty.m3u8" to "#EXTM3U\n#EXT-X-ENDLIST")),
                "https://cdn.test/empty.m3u8",
                target,
            ) { }
            fail("expected IllegalStateException")
        } catch (error: IllegalStateException) {
            assertTrue("分段" in error.message!!)
        }
    }
}
