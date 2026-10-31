package app.miyomu.core.database

import androidx.room.ConstructedBy
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.room.Upsert
import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

@Entity(tableName = "app_meta")
data class AppMetaEntity(
    @PrimaryKey val key: String,
    val value: String,
)

@Dao
interface AppMetaDao {
    @Query("SELECT value FROM app_meta WHERE key = :key")
    suspend fun get(key: String): String?

    @Upsert
    suspend fun put(entity: AppMetaEntity)
}

@Database(
    entities = [
        AppMetaEntity::class,
        FavouriteEntity::class,
        ReadingProgressEntity::class,
    ],
    version = 2,
)
@ConstructedBy(MiyomuDatabaseConstructor::class)
abstract class MiyomuDatabase : RoomDatabase() {
    abstract fun appMetaDao(): AppMetaDao
    abstract fun favouriteDao(): FavouriteDao
    abstract fun readingProgressDao(): ReadingProgressDao
}

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `favourite` (
                `contentId` INTEGER NOT NULL,
                `sourceName` TEXT NOT NULL,
                `title` TEXT NOT NULL,
                `coverUrl` TEXT,
                `authors` TEXT NOT NULL,
                `contentType` TEXT NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL,
                PRIMARY KEY(`contentId`)
            )
            """.trimIndent(),
        )
        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `reading_progress` (
                `contentId` INTEGER NOT NULL,
                `chapterId` INTEGER NOT NULL,
                `chapterTitle` TEXT,
                `page` INTEGER NOT NULL,
                `scroll` REAL NOT NULL,
                `percent` REAL NOT NULL,
                `updatedAt` INTEGER NOT NULL,
                PRIMARY KEY(`contentId`)
            )
            """.trimIndent(),
        )
    }
}

@Suppress("KotlinNoActualForExpect")
expect object MiyomuDatabaseConstructor : RoomDatabaseConstructor<MiyomuDatabase> {
    override fun initialize(): MiyomuDatabase
}
