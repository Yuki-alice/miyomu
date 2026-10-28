package app.miyomu.source.runtime

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals

class JsModelsTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun parsesGroupedChaptersAndTags() {
        val element = json.parseToJsonElement(
            """
            {
              "title": "Grouped",
              "cover": "",
              "tags": {"author": ["A"], "genre": ["G1", "G2"]},
              "chapters": {
                "Main": {"ep_1": "Chapter 1", "ep_2": "Chapter 2"},
                "Extras": {"ex_1": "Extra 1"}
              },
              "updateTime": "2026-10-28"
            }
            """.trimIndent(),
        ).jsonObject

        val details = json.decodeFromJsonElement<JsComicDetailsDto>(element).toDetails("comic-1")

        assertEquals(listOf("A"), details.tags["author"])
        assertEquals(listOf("G1", "G2"), details.tags["genre"])
        assertEquals(
            listOf(
                Triple("ep_1", "Chapter 1", "Main"),
                Triple("ep_2", "Chapter 2", "Main"),
                Triple("ex_1", "Extra 1", "Extras"),
            ),
            details.chapters.map { Triple(it.id, it.title, it.group) },
        )
    }
}
