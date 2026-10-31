package app.miyomu.feature.library

import app.miyomu.core.data.LibraryRepository
import app.miyomu.core.data.ReadingProgress
import app.miyomu.core.model.ChapterId
import app.miyomu.core.model.ContentId
import app.miyomu.core.model.ContentSummary
import app.miyomu.core.model.ContentType
import app.miyomu.core.model.SourceId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach

@OptIn(ExperimentalCoroutinesApi::class)
class LibraryViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @BeforeEach
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterEach
    fun tearDown() = Dispatchers.resetMain()

    @org.junit.jupiter.api.Test
    fun exposesFavouritesFromRepository() = runTest(dispatcher.scheduler) {
        val summary = ContentSummary(
            id = ContentId(1),
            source = SourceId("mangadex"),
            title = "Sky Archive",
            contentType = ContentType.MANGA,
        )
        val viewModel = LibraryViewModel(FakeLibraryRepository(listOf(summary)))
        val favourites = viewModel.favourites.first { it.isNotEmpty() }
        assertEquals(1, favourites.size)
        assertEquals("Sky Archive", favourites.first().title)
    }

    private class FakeLibraryRepository(
        private val favourites: List<ContentSummary>,
    ) : LibraryRepository {
        override fun observeFavourites(): Flow<List<ContentSummary>> = flowOf(favourites)
        override fun observeProgress(contentId: ContentId): Flow<ReadingProgress?> = flowOf(null)
        override suspend fun setFavourite(summary: ContentSummary, favourite: Boolean) = Unit
        override suspend fun saveProgress(
            contentId: ContentId,
            chapterId: ChapterId,
            chapterTitle: String?,
            page: Int,
            scroll: Float,
            percent: Float,
        ) = Unit
    }
}
