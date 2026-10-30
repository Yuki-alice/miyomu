package app.miyomu.data

import app.miyomu.contract.*
import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import app.miyomu.data.db.MiyomuDatabase
import app.miyomu.model.LibraryItem
import app.miyomu.model.LibraryStatus
import app.miyomu.model.MediaType
import app.miyomu.model.ProgressKind
import app.miyomu.model.ReadingProgress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BackupRoundTripTest {

    @TempDir
    lateinit var tempDir: Path

    private lateinit var database: MiyomuDatabase

    @BeforeTest
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder<MiyomuDatabase>()
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .build()
    }

    @AfterTest
    fun tearDown() {
        database.close()
    }

    @Test
    fun exportThenRestoreIntoFreshDatabase() = runBlocking {
        val source = MiyomuData(database)
        val item = LibraryItem(
            id = "html_fixture:demo-1",
            mediaType = MediaType.COMIC,
            sourceId = "html_fixture",
            sourceKey = "demo-1",
            title = "Demo Comic",
            status = LibraryStatus.ONGOING,
            addedAt = 100,
            updatedAt = 200,
            lastViewedAt = 300,
        )
        source.library.upsert(item)
        source.progress.save(
            ReadingProgress(
                itemId = item.id,
                entryId = "c1",
                kind = ProgressKind.PAGE,
                position = 4.0,
                duration = 20.0,
                updatedAt = 400,
                deviceId = "desktop",
            ),
        )
        source.chapters.replaceChapters(
            item.id,
            listOf(ChapterRecord(key = "c1", name = "第1话", url = "c1", sourceOrder = 0)),
            fetchedAt = 500,
        )
        source.chapters.setRead(item.id, listOf("c1"), read = true)
        source.anime.replaceEpisodes(
            "local:show",
            listOf(EpisodeRecord(key = "e1", name = "E01", url = "e1", sourceOrder = 0)),
            fetchedAt = 600,
        )
        source.anime.setSeen("local:show", listOf("e1"), seen = true)

        val manager = BackupManager(source)
        val jsonText = manager.export(nowMillis = 1234)
        manager.writeToFile(jsonText, tempDir.resolve("backup.json").toFile())
        val readBack = manager.readFromFile(tempDir.resolve("backup.json").toFile())

        // Fresh database: restore with replace and compare.
        val restored = MiyomuData(
            Room.inMemoryDatabaseBuilder<MiyomuDatabase>()
                .setDriver(BundledSQLiteDriver())
                .setQueryCoroutineContext(Dispatchers.IO)
                .build(),
        )
        try {
            val result = BackupManager(restored).restore(readBack, replace = true)
            assertEquals(1, result.library)
            assertEquals(1, result.progress)
            assertEquals(1, result.chapters)
            assertEquals(1, result.episodes)

            assertEquals(item.copy(updatedAt = 200), restored.library.get(item.id))
            assertEquals(4.0, restored.progress.latest(item.id)?.position)
            val chapter = restored.chapters.chapters(item.id).single()
            assertEquals(true, chapter.read)
            val episode = restored.anime.episodes("local:show").single()
            assertEquals(true, episode.seen)
        } finally {
            restored.close()
        }
    }

    @Test
    fun rejectsUnparsableBackup() = runBlocking {
        val data = MiyomuData(database)
        val error = runCatching { BackupManager(data).restore("{ not json", replace = true) }.exceptionOrNull()
        assertTrue(error?.message.orEmpty().contains("无法解析"))
    }

    @Test
    fun exportedJsonParsesAsBackupData() = runBlocking {
        val data = MiyomuData(database)
        val jsonText = BackupManager(data).export(nowMillis = 42)
        val parsed = Json.decodeFromString<BackupData>(jsonText)
        assertEquals(42, parsed.exportedAt)
        assertEquals(1, parsed.version)
    }
}
