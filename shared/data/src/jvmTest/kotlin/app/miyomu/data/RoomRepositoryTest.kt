package app.miyomu.data

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import app.miyomu.data.db.MiyomuDatabase
import app.miyomu.model.LibraryItem
import app.miyomu.model.LibraryStatus
import app.miyomu.model.MediaType
import app.miyomu.model.ProgressKind
import app.miyomu.model.ResolvedStream
import app.miyomu.model.ResolvedVideo
import app.miyomu.model.ReadingProgress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.io.TempDir
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RoomRepositoryTest {

    @TempDir
    lateinit var tempDir: java.nio.file.Path

    private lateinit var database: MiyomuDatabase
    private lateinit var data: MiyomuData

    @BeforeTest
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder<MiyomuDatabase>()
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .build()
        data = MiyomuData(database)
    }

    @AfterTest
    fun tearDown() {
        database.close()
    }

    @Test
    fun upsertsObservesAndDeletesLibraryItem() = runBlocking {
        val item = demoItem()

        data.library.upsert(item)
        assertEquals(item, data.library.get(item.id))
        assertEquals(listOf(item), data.library.observeLibrary().first())

        data.library.markViewed(item.id, viewedAt = 200)
        val viewed = data.library.get(item.id)
        assertEquals(200, viewed?.lastViewedAt)
        assertEquals(200, viewed?.updatedAt)

        data.library.delete(item.id)
        assertNull(data.library.get(item.id))
    }

    @Test
    fun keepsLatestProgressPerItem() = runBlocking {
        val itemId = demoItem().id
        val first = ReadingProgress(
            itemId = itemId,
            entryId = "ep_1",
            kind = ProgressKind.PAGE,
            position = 3.0,
            updatedAt = 100,
            deviceId = "phone",
        )
        val second = first.copy(
            entryId = "ep_2",
            position = 1.0,
            updatedAt = 200,
            deviceId = "desktop",
        )

        data.progress.save(first)
        data.progress.save(second)

        assertEquals(second, data.progress.latest(itemId))
        assertEquals(second, data.progress.observeLatest(itemId).first())

        data.progress.clear(itemId)
        assertNull(data.progress.latest(itemId))
    }

    @Test
    fun persistsAcrossReopenAtFilePath() = runBlocking {
        val item = demoItem()
        val path = tempDir.resolve("miyomu.db").toString()

        val first = createMiyomuData(JvmMiyomuDatabaseFactory(path))
        try {
            first.library.upsert(item)
        } finally {
            first.close()
        }

        val second = createMiyomuData(JvmMiyomuDatabaseFactory(path))
        try {
            assertEquals(item, second.library.get(item.id))
        } finally {
            second.close()
        }
    }

    private fun demoItem(): LibraryItem = LibraryItem(
        id = LibraryItem.idOf("html_fixture", "demo-1"),
        mediaType = MediaType.COMIC,
        sourceId = "html_fixture",
        sourceKey = "demo-1",
        title = "Demo Comic",
        coverUrl = "https://example.invalid/cover.jpg",
        author = "Demo Author",
        status = LibraryStatus.ONGOING,
        addedAt = 100,
        updatedAt = 100,
    )

    @Test
    fun replaceEpisodesPreservesMarksAndReconcilesKeys() = runBlocking {
        val itemId = "local:show"
        data.anime.replaceEpisodes(
            itemId,
            listOf(
                EpisodeRecord(key = "/a/e1.mp4", name = "E01", url = "/a/e1.mp4", sourceOrder = 0),
                EpisodeRecord(key = "/a/e2.mp4", name = "E02", url = "/a/e2.mp4", sourceOrder = 1),
            ),
            fetchedAt = 100,
        )
        data.anime.setSeen(itemId, listOf("/a/e1.mp4"), seen = true)
        data.anime.setBookmarked(itemId, listOf("/a/e2.mp4"), bookmarked = true)

        // Refresh with E01 renamed and E03 replacing E02: marks follow keys, E02 disappears.
        data.anime.replaceEpisodes(
            itemId,
            listOf(
                EpisodeRecord(key = "/a/e1.mp4", name = "第 1 集", url = "/a/e1.mp4", sourceOrder = 0),
                EpisodeRecord(key = "/a/e3.mp4", name = "E03", url = "/a/e3.mp4", sourceOrder = 1),
            ),
            fetchedAt = 200,
        )

        val episodes = data.anime.episodes(itemId)
        assertEquals(listOf("/a/e1.mp4", "/a/e3.mp4"), episodes.map { it.key })
        assertEquals(true, episodes.first { it.key == "/a/e1.mp4" }.seen)
        assertEquals("第 1 集", episodes.first { it.key == "/a/e1.mp4" }.name)
        assertNull(episodes.firstOrNull { it.key == "/a/e2.mp4" })

        data.anime.clear(itemId)
        assertEquals(emptyList(), data.anime.episodes(itemId))
    }

    @Test
    fun replaceChaptersPreservesReadMark() = runBlocking {
        val itemId = "html_fixture:demo-1"
        data.chapters.replaceChapters(
            itemId,
            listOf(
                ChapterRecord(key = "c1", name = "第1话", url = "https://example.invalid/c1", sourceOrder = 0),
                ChapterRecord(key = "c2", name = "第2话", url = "https://example.invalid/c2", sourceOrder = 1),
            ),
            fetchedAt = 100,
        )
        data.chapters.setRead(itemId, listOf("c1"), read = true)
        data.chapters.setBookmarked(itemId, listOf("c2"), bookmarked = true)

        data.chapters.replaceChapters(
            itemId,
            listOf(
                ChapterRecord(key = "c1", name = "第1话", url = "https://example.invalid/c1", sourceOrder = 0),
            ),
            fetchedAt = 200,
        )

        val chapters = data.chapters.chapters(itemId)
        assertEquals(listOf("c1"), chapters.map { it.key })
        assertEquals(true, chapters.single().read)

        val observed = data.chapters.observeChapters(itemId).first()
        assertEquals(1, observed.size)
    }

    @Test
    fun resolveCacheRespectsTtl() = runBlocking {
        val video = ResolvedVideo(
            streams = listOf(ResolvedStream(url = "https://cdn.example.invalid/e1.m3u8", quality = "1080P")),
            headers = mapOf("Referer" to "https://example.invalid/", "User-Agent" to "MiyomuTest"),
        )
        data.videoResolve.put("ep1", video, now = 1_000)

        assertEquals(video, data.videoResolve.get("ep1", maxAgeMillis = 10_000, now = 5_000))
        assertNull(data.videoResolve.get("ep1", maxAgeMillis = 10_000, now = 20_000))

        data.videoResolve.put("ep2", video, now = 1_000)
        data.videoResolve.clearExpired(now = 20_000, maxAgeMillis = 10_000)
        assertNull(data.videoResolve.get("ep2", maxAgeMillis = Long.MAX_VALUE, now = 0))
    }
}
