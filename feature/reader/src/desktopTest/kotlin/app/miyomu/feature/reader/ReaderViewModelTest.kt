package app.miyomu.feature.reader

import app.miyomu.core.data.ContentRepository
import app.miyomu.core.data.LibraryRepository
import app.miyomu.core.data.ReadingProgress
import app.miyomu.core.model.ChapterId
import app.miyomu.core.model.ContentDetails
import app.miyomu.core.model.ContentId
import app.miyomu.core.model.ContentSummary
import app.miyomu.core.model.Outcome
import app.miyomu.core.model.PageSummary
import app.miyomu.core.model.SourceId
import app.miyomu.core.model.SourceOption
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ReaderViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @BeforeEach
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterEach
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun restoresProgressForSameChapter() = runTest(dispatcher.scheduler) {
        val progress = ReadingProgress(
            contentId = ContentId(1),
            chapterId = ChapterId(1001),
            chapterTitle = "Chapter 1",
            page = 1,
            scroll = 0f,
            percent = .6f,
            updatedAt = 1L,
        )
        val viewModel = ReaderViewModel(
            contentRepository = FakeContentRepository(),
            libraryRepository = FakeLibraryRepository(progress),
        )
        viewModel.load("mangadex", "1", "1001")
        advanceUntilIdle()
        assertEquals(3, viewModel.state.value.pages.size)
        assertEquals(1, viewModel.state.value.initialSlot)
        assertFalse(viewModel.state.value.loading)
    }

    @Test
    fun switchesToDoubleAndRtlModes() = runTest(dispatcher.scheduler) {
        val viewModel = ReaderViewModel(
            contentRepository = FakeContentRepository(),
            libraryRepository = FakeLibraryRepository(null),
        )
        viewModel.load("mangadex", "1", "1001")
        advanceUntilIdle()
        viewModel.setMode(ReaderMode.DOUBLE)
        assertEquals(ReaderMode.DOUBLE, viewModel.state.value.mode)
        assertEquals(true, viewModel.state.value.scene?.config?.isDoublePage)
        viewModel.setMode(ReaderMode.RTL)
        assertEquals(org.skepsun.kototoro.reader.core.SceneReadingDirection.RIGHT_TO_LEFT, viewModel.state.value.scene?.readingDirection)
    }

    @Test
    fun ignoresProgressFromAnotherChapter() = runTest(dispatcher.scheduler) {
        val progress = ReadingProgress(
            contentId = ContentId(1),
            chapterId = ChapterId(999),
            chapterTitle = "Other",
            page = 2,
            scroll = 0f,
            percent = 1f,
            updatedAt = 1L,
        )
        val viewModel = ReaderViewModel(
            contentRepository = FakeContentRepository(),
            libraryRepository = FakeLibraryRepository(progress),
        )
        viewModel.load("mangadex", "1", "1001")
        advanceUntilIdle()
        assertEquals(0, viewModel.state.value.initialSlot)
    }

    private class FakeContentRepository : ContentRepository {
        override suspend fun sources(): List<SourceOption> = emptyList()
        override suspend fun filterOptions(sourceName: String): Outcome<app.miyomu.core.model.ContentFilterOptions> =
            Outcome.Success(app.miyomu.core.model.ContentFilterOptions())
        override suspend fun browse(sourceName: String, offset: Int, limit: Int, query: String?, filter: app.miyomu.core.model.ContentFilter): Outcome<List<ContentSummary>> =
            Outcome.Success(emptyList())

        override suspend fun details(sourceName: String, contentId: ContentId): Outcome<ContentDetails> =
            Outcome.Failure(app.miyomu.core.model.AppError.NotFound("not used"))

        override suspend fun pages(sourceName: String, contentId: ContentId, chapterId: ChapterId): Outcome<List<PageSummary>> =
            Outcome.Success(
                (1..3).map { index ->
                    PageSummary(id = index.toLong(), index = index - 1, url = "https://example.com/$index.jpg")
                },
            )
    }

    private class FakeLibraryRepository(private val progress: ReadingProgress?) : LibraryRepository {
        override fun observeFavourites(): Flow<List<ContentSummary>> = flowOf(emptyList())
        override fun observeProgress(contentId: ContentId): Flow<ReadingProgress?> = flowOf(progress)
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
