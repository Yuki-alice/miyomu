package app.miyomu.core.data

import app.miyomu.core.data.demo.DemoSourceRuntime
import app.miyomu.core.model.Outcome
import app.miyomu.core.source.SourceRegistry
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ContentRepositoryTest {
    private fun repository() = DefaultContentRepository(
        SourceRegistry(listOf(DemoSourceRuntime())),
    )

    @Test
    fun browseReturnsFixtureContent() = runTest {
        val result = repository().browse("demo")
        assertTrue(result is Outcome.Success)
        val items = (result as Outcome.Success).value
        assertEquals(8, items.size)
        assertEquals("Sky Archive", items.first().title)
        assertEquals("Mira Okonkwo", items.first().authors.single())
    }

    @Test
    fun detailsReturnsChapters() = runTest {
        val repo = repository()
        val items = (repo.browse("demo") as Outcome.Success).value
        val details = repo.details("demo", items.first().id)
        assertTrue(details is Outcome.Success)
        val value = (details as Outcome.Success).value
        assertNotNull(value.description)
        assertEquals(5, value.chapters.size)
        assertEquals("Chapter 1", value.chapters.first().title)
    }

    @Test
    fun pagesReturnsFixturePages() = runTest {
        val repo = repository()
        val items = (repo.browse("demo") as Outcome.Success).value
        val details = (repo.details("demo", items.first().id) as Outcome.Success).value
        val pages = repo.pages("demo", items.first().id, details.chapters.first().id)
        assertTrue(pages is Outcome.Success)
        val value = (pages as Outcome.Success).value
        assertEquals(3, value.size)
        assertEquals(0, value.first().index)
        assertTrue(value.first().url.contains("example.com/page"))
    }
}
