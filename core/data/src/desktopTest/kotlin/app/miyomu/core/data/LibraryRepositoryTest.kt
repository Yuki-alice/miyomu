package app.miyomu.core.data

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import app.miyomu.core.database.MiyomuDatabase
import app.miyomu.core.database.MiyomuDatabaseConstructor
import app.miyomu.core.model.ContentId
import app.miyomu.core.model.ContentSummary
import app.miyomu.core.model.ContentType
import app.miyomu.core.model.SourceId
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class LibraryRepositoryTest {
    private fun database(): MiyomuDatabase =
        Room.inMemoryDatabaseBuilder<MiyomuDatabase> { MiyomuDatabaseConstructor.initialize() }
            .setDriver(BundledSQLiteDriver())
            .build()

    private val summary = ContentSummary(
        id = ContentId(1),
        source = SourceId("demo"),
        title = "Sky Archive",
        authors = listOf("Mira Okonkwo"),
        contentType = ContentType.MANGA,
    )

    @Test
    fun favouritePersistsAndCanBeRemoved() = runTest {
        val db = database()
        val repo = DefaultLibraryRepository(db)

        assertTrue(repo.observeFavourites().first().isEmpty())
        repo.setFavourite(summary, true)
        assertEquals(1, repo.observeFavourites().first().size)
        assertEquals("Sky Archive", repo.observeFavourites().first().first().title)

        repo.setFavourite(summary, false)
        assertTrue(repo.observeFavourites().first().isEmpty())
        db.close()
    }

    @Test
    fun readingProgressPersists() = runTest {
        val db = database()
        val repo = DefaultLibraryRepository(db)

        repo.saveProgress(
            contentId = summary.id,
            chapterId = app.miyomu.core.model.ChapterId(1001),
            chapterTitle = "Chapter 1",
            page = 2,
            scroll = 0.4f,
            percent = 0.33f,
        )
        val progress = repo.observeProgress(summary.id).first()
        assertNotNull(progress)
        assertEquals(2, progress!!.page)
        assertEquals(0.33f, progress.percent)
        assertTrue(progress.updatedAt > 0)
        db.close()
    }
}
