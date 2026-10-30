package app.miyomu.ui.history

import app.miyomu.contract.ChapterRecord
import app.miyomu.data.JvmMiyomuDatabaseFactory
import app.miyomu.contract.DataPort
import app.miyomu.data.createMiyomuData
import app.miyomu.local.FileLocalComicRepository
import app.miyomu.local.LocalLibraryRepository
import app.miyomu.novel.LocalNovelRepository
import app.miyomu.novel.NovelLibraryRepository
import app.miyomu.model.LibraryItem
import app.miyomu.model.MediaType
import app.miyomu.model.ProgressKind
import app.miyomu.model.ReadingProgress
import app.miyomu.source.manager.SourceRuntimeManager
import app.miyomu.source.repo.SourceInstallManager
import app.miyomu.ui.FakeSourceRuntimeManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MangaHistoryModelTest {

    @TempDir
    lateinit var root: File

    private lateinit var scope: CoroutineScope
    private lateinit var data: DataPort

    @BeforeTest
    fun setUp() {
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        data = createMiyomuData(
            JvmMiyomuDatabaseFactory(File(root, "history-test.db").absolutePath),
        )
    }

    @AfterTest
    fun tearDown() {
        scope.coroutineContext.cancelChildren()
        data.close()
    }

    private fun graph(): app.miyomu.ui.MiyomuGraph {
        val sourceRuntime: SourceRuntimeManager = FakeSourceRuntimeManager()
        return app.miyomu.ui.MiyomuGraph(
            library = LocalLibraryRepository(),
            comics = FileLocalComicRepository(),
            novels = LocalNovelRepository(),
            novelLibrary = NovelLibraryRepository(),
            data = data,
            root = app.miyomu.local.FileLocalFileRef(root),
            deviceId = "test",
            nowMillis = { 1_000L },
            preferenceStore = tachiyomi.core.common.preference.InMemoryPreferenceStore(),
            sourceInstalls = FakeInstalls(),
            sourceRuntime = sourceRuntime,
        )
    }

    @Test
    fun composesGroupedHistoryWithMarks() = runBlocking {
        val itemId = "html_fixture:demo-1"
        data.library.upsert(
            LibraryItem(
                id = itemId,
                mediaType = MediaType.COMIC,
                sourceId = "html_fixture",
                sourceKey = "demo-1",
                title = "Demo Comic",
                coverUrl = "https://example.invalid/c.jpg",
                addedAt = 100,
                updatedAt = 100,
            ),
        )
        data.chapters.replaceChapters(
            itemId,
            listOf(ChapterRecord(key = "c1", name = "第1话", url = "c1", chapterNumber = 1.0, sourceOrder = 0)),
            fetchedAt = 200,
        )
        data.progress.save(
            ReadingProgress(
                itemId = itemId,
                entryId = "c1",
                kind = ProgressKind.PAGE,
                position = 3.0,
                updatedAt = 1_700_000_000_000,
                deviceId = "test",
            ),
        )

        val model = MangaHistoryScreenModelMiyomu(graph(), scope)
        withTimeout(15_000) {
            model.state.first { it.list != null && it.list!!.isNotEmpty() }
        }

        val list = model.state.value.list!!
        assertEquals(2, list.size) // one header + one item
        val item = list.filterIsInstance<eu.kanade.presentation.history.manga.MangaHistoryUiModel.Item>().single()
        assertEquals("Demo Comic", item.item.title)
        assertEquals(1.0, item.item.chapterNumber)
        assertEquals(true, item.item.coverData.isMangaFavorite)

        // Query filtering hides the entry.
        model.search("其他")
        withTimeout(15_000) {
            model.state.first { it.list?.none { it is eu.kanade.presentation.history.manga.MangaHistoryUiModel.Item } == true }
        }

        // Clear the filter first, otherwise an empty filtered list would satisfy the
        // post-delete wait before the database work completes.
        model.search(null)
        withTimeout(15_000) {
            model.state.first { state ->
                state.list?.any { it is eu.kanade.presentation.history.manga.MangaHistoryUiModel.Item } == true
            }
        }

        // Delete removes the progress row (reset also clears the chapter mark).
        model.deleteHistory(item.item, removeEverything = true)
        withTimeout(15_000) {
            model.state.first { it.list?.isEmpty() == true }
        }
        assertTrue(data.progress.observeAll().first().isEmpty())
        assertEquals(false, data.chapters.chapters(itemId).single().read)
    }
}

private class FakeInstalls : SourceInstallManager {
    private val empty = kotlinx.coroutines.flow.MutableStateFlow<List<app.miyomu.source.repo.InstalledSource>>(emptyList())
    override val installed = empty
    override val repositories = kotlinx.coroutines.flow.MutableStateFlow<List<String>>(emptyList())
    override suspend fun ensureLoaded() = Unit
    override suspend fun addRepository(url: String) = Unit
    override suspend fun removeRepository(url: String) = Unit
    override suspend fun fetchCatalog() = app.miyomu.source.repo.SourceCatalog()
    override suspend fun getById(id: String) = null
    override suspend fun install(source: app.miyomu.source.repo.AvailableSource) = error("unused")
    override suspend fun uninstall(id: String) = Unit
}
