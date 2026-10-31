package app.miyomu.core.source

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MangaDexSourceRuntimeTest {
    @Test
    fun parsesListResponse() = runTest {
        val engine = MockEngine {
            respond(
                content = """
                    {
                      "result": "ok",
                      "data": [
                        {
                          "id": "e900135b-8eba-4d29-85c9-db3d5ae885c7",
                          "type": "manga",
                          "attributes": {
                            "title": {"en": "The Ghostly Doctor"},
                            "altTitles": [{"zh-ro": "Guiyi"}],
                            "description": {"en": "A doctor sees ghosts."},
                            "status": "ongoing",
                            "contentRating": "safe",
                            "tags": []
                          },
                          "relationships": [
                            {"id": "cover", "type": "cover_art", "attributes": {"fileName": "cover.jpg"}},
                            {"id": "author", "type": "author", "attributes": {"name": "Ghost Writer"}}
                          ]
                        }
                      ]
                    }
                """.trimIndent(),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val runtime = MangaDexSourceRuntime(HttpClient(engine))
        val list = runtime.getList("mangadex", 0, null, null)
        assertEquals(1, list.size)
        val item = list.single()
        assertEquals("The Ghostly Doctor", item.title)
        assertEquals(setOf("Ghost Writer"), item.authors)
        assertTrue(item.coverUrl!!.contains("uploads.mangadex.org/covers"))
        assertTrue(item.id != 0L)
    }
}
