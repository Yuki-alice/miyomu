package app.miyomu.ui.core

import app.miyomu.contract.ChapterRecord
import app.miyomu.data.JvmMiyomuDatabaseFactory
import app.miyomu.data.createMiyomuData
import app.miyomu.local.FileLocalComicRepository
import app.miyomu.local.FileLocalFileRef
import app.miyomu.local.LocalLibraryRepository
import app.miyomu.novel.LocalNovelRepository
import app.miyomu.novel.NovelLibraryRepository
import app.miyomu.model.ComicDetails
import app.miyomu.model.ComicSummary
import app.miyomu.model.LibraryItem
import app.miyomu.model.MediaType
import app.miyomu.model.PageRef
import app.miyomu.model.Paged
import app.miyomu.model.ResolvedVideo
import app.miyomu.source.manager.SourceRuntimeManager
import app.miyomu.source.manager.SourceRuntimeState
import app.miyomu.source.repo.SourceCatalog
import app.miyomu.source.repo.SourceInstallManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LibraryUpdaterTest {

    @TempDir
    lateinit var root: File

    private lateinit var scope: CoroutineScope

    @BeforeTest
    fun setUp() {
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }

    @AfterTest
    fun tearDown() {
        scope.coroutineContext.cancelChildren()
    }

    @Test
    fun recordsNewChaptersAndKeepsMarks() = runBlocking {
        val data = createMiyomuData(JvmMiyomuDatabaseFactory(File(root, "updates.db").absolutePath))
        val itemId = "html_fixture:demo-1"
        data.library.upsert(
            LibraryItem(
                id = itemId,
                mediaType = MediaType.COMIC,
                sourceId = "html_fixture",
                sourceKey = "demo-1",
                title = "Demo Comic",
                addedAt = 100,
                updatedAt = 100,
            ),
        )
        data.chapters.replaceChapters(
            itemId,
            listOf(
                ChapterRecord(key = "c1", name = "第1话", url = "c1", sourceOrder = 0),
                ChapterRecord(key = "c2", name = "第2话", url = "c2", sourceOrder = 1),
            ),
            fetchedAt = 200,
        )
        data.chapters.setRead(itemId, listOf("c1", "c2"), read = true)

        val runtime = MutableDetailsRuntime(
            first = listOf("c1" to "第1话", "c2" to "第2话"),
            second = listOf("c1" to "第1话", "c2" to "第2话", "c3" to "第3话"),
        )
        // Run the check off the test's single-threaded runBlocking so mutexes can proceed.
        val graph = app.miyomu.ui.MiyomuGraph(
            library = LocalLibraryRepository(),
            comics = FileLocalComicRepository(),
            novels = LocalNovelRepository(),
            novelLibrary = NovelLibraryRepository(),
            data = data,
            root = FileLocalFileRef(root),
            deviceId = "test",
            nowMillis = { 1_000L },
            preferenceStore = tachiyomi.core.common.preference.InMemoryPreferenceStore(),
            sourceInstalls = FakeInstalls(),
            sourceRuntime = runtime,
        )
        val updater = LibraryUpdater(graph, scope)
        withContext(Dispatchers.Default) { updater.checkNowAndWait() }

        // Source still reports two chapters: nothing new.
        assertEquals(0, updater.state.value.newChapters)
        assertEquals(0, data.libraryUpdates.observeUpdates().first().size)

        // A new chapter appears upstream: only it lands in the updates feed, marks survive.
        runtime.useSecondVersion()
        withContext(Dispatchers.Default) { updater.checkNowAndWait() }
        assertEquals(1, updater.state.value.newChapters)
        val updates = data.libraryUpdates.observeUpdates().first()
        assertEquals(listOf("c3"), updates.map { it.key })
        val chapters = data.chapters.chapters(itemId)
        assertEquals(3, chapters.size)
        assertEquals(true, chapters.first { it.key == "c1" }.read)

        data.close()
    }
}

/** Serves an evolving chapter list so a second check discovers a new chapter. */
private class MutableDetailsRuntime(
    first: List<Pair<String, String>>,
    second: List<Pair<String, String>>,
) : SourceRuntimeManager {

    private val versions = listOf(first, second)
    private var stage = 0
    private val stateFlow = MutableStateFlow(SourceRuntimeState())
    override val state: StateFlow<SourceRuntimeState> = stateFlow

    fun useSecondVersion() {
        stage = 1
    }

    override suspend fun refresh() = Unit

    override suspend fun search(sourceId: String, query: String, page: Int): Paged<ComicSummary> =
        Paged(emptyList(), hasNext = false)

    override suspend fun details(sourceId: String, comicId: String): ComicDetails {
        val version = versions[stage.coerceAtMost(versions.lastIndex)]
        return ComicDetails(
            id = comicId,
            title = "Demo Comic",
            chapters = version.map { (key, name) -> app.miyomu.model.Chapter(id = key, title = name) },
        )
    }

    override suspend fun pages(sourceId: String, comicId: String, chapterId: String): List<PageRef> = emptyList()

    override suspend fun resolveVideo(sourceId: String, episodeUrl: String): ResolvedVideo = ResolvedVideo()

    override suspend fun loadImage(sourceId: String, url: String, headers: Map<String, String>): ByteArray =
        ByteArray(0)

    override fun close() = Unit
}

private class FakeInstalls : SourceInstallManager {
    private val empty = MutableStateFlow<List<app.miyomu.source.repo.InstalledSource>>(emptyList())
    override val installed = empty
    override val repositories = MutableStateFlow<List<String>>(emptyList())
    override suspend fun ensureLoaded() = Unit
    override suspend fun addRepository(url: String) = Unit
    override suspend fun removeRepository(url: String) = Unit
    override suspend fun fetchCatalog() = SourceCatalog()
    override suspend fun getById(id: String) = null
    override suspend fun install(source: app.miyomu.source.repo.AvailableSource) = error("unused")
    override suspend fun uninstall(id: String) = Unit
}
