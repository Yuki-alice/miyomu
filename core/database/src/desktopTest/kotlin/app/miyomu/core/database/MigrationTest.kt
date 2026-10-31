package app.miyomu.core.database

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.nio.file.Files

class MigrationTest {
    @Test
    fun migratesV1ToV2AndCreatesNewTables() = runBlocking {
        val dir = Files.createTempDirectory("miyomu-migration")
        val path = dir.resolve("miyomu.db").toString()

        BundledSQLiteDriver().open(path).use { connection ->
            connection.execSQL(
                "CREATE TABLE IF NOT EXISTS `app_meta` (`key` TEXT NOT NULL, `value` TEXT NOT NULL, PRIMARY KEY(`key`))",
            )
            connection.execSQL("PRAGMA user_version = 1")
        }

        val database = Room.databaseBuilder<MiyomuDatabase>(name = path)
            .setDriver(BundledSQLiteDriver())
            .addMigrations(MIGRATION_1_2)
            .build()
        database.favouriteDao().upsert(
            FavouriteEntity(
                contentId = 1L,
                sourceName = "demo",
                title = "Sky Archive",
                coverUrl = null,
                authors = "Mira",
                contentType = "MANGA",
                createdAt = 1L,
                updatedAt = 1L,
            ),
        )
        assertEquals(1, database.favouriteDao().observeAll().first().size)
        database.close()
    }
}
