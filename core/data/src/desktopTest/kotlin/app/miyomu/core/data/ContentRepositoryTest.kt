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
        val result = repository().browse()
        assertTrue(result is Outcome.Success)
        val items = (result as Outcome.Success).value
        assertEquals(8, items.size)
        assertEquals("Sky Archive", items.first().title)
        assertEquals("Mira Okonkwo", items.first().authors.single())
    }

    @Test
    fun detailsReturnsChapters() = runTest {
        val repo = repository()
        val items = (repo.browse() as Outcome.Success).value
        val details = repo.details(items.first().id)
        assertTrue(details is Outcome.Success)
        val value = (details as Outcome.Success).value
        assertNotNull(value.description)
        assertEquals(5, value.chapters.size)
        assertEquals("Chapter 1", value.chapters.first().title)
    }
}
