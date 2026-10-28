package app.miyomu.data

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import app.miyomu.data.db.MiyomuDatabase
import app.miyomu.model.LibraryItem
import app.miyomu.model.LibraryStatus
import app.miyomu.model.MediaType
import app.miyomu.model.ProgressKind
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
}
