package app.miyomu.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

@Database(
    entities = [
        LibraryItemEntity::class,
        ReadingProgressEntity::class,
        EpisodeEntity::class,
        ChapterEntity::class,
        VideoResolveEntity::class,
        CategoryEntity::class,
        MangaCategoryEntity::class,
        LibraryUpdateEntity::class,
    ],
    version = 4,
    exportSchema = true,
)
abstract class MiyomuDatabase : RoomDatabase() {
    abstract fun dao(): MiyomuDao

    companion object {
        // createSql strings mirror the exported schemas/*.json so a future
        // migration test can diff them against the generated definitions.
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SQLiteConnection) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `entry_episode` (" +
                        "`itemId` TEXT NOT NULL, `key` TEXT NOT NULL, `name` TEXT NOT NULL, " +
                        "`url` TEXT NOT NULL, `episodeNumber` REAL, `sourceOrder` INTEGER NOT NULL, " +
                        "`seen` INTEGER NOT NULL, `bookmark` INTEGER NOT NULL, `fetchedAt` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`itemId`, `key`))",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_entry_episode_itemId_sourceOrder` " +
                        "ON `entry_episode` (`itemId`, `sourceOrder`)",
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `entry_chapter` (" +
                        "`itemId` TEXT NOT NULL, `key` TEXT NOT NULL, `name` TEXT NOT NULL, " +
                        "`url` TEXT NOT NULL, `chapterNumber` REAL, `sourceOrder` INTEGER NOT NULL, " +
                        "`read` INTEGER NOT NULL, `bookmark` INTEGER NOT NULL, `fetchedAt` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`itemId`, `key`))",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_entry_chapter_itemId_sourceOrder` " +
                        "ON `entry_chapter` (`itemId`, `sourceOrder`)",
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `video_resolve_cache` (" +
                        "`key` TEXT NOT NULL, `streamsJson` TEXT NOT NULL, `headersJson` TEXT NOT NULL, " +
                        "`fetchedAt` INTEGER NOT NULL, PRIMARY KEY(`key`))",
                )
            }
        }

        // createSql mirrors the exported 3.json (verified the same way as 1→2).
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SQLiteConnection) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `category` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, " +
                        "`order` INTEGER NOT NULL, `flags` INTEGER NOT NULL, `hidden` INTEGER NOT NULL)",
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `manga_category` (" +
                        "`itemId` TEXT NOT NULL, `categoryId` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`itemId`, `categoryId`))",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_manga_category_categoryId` " +
                        "ON `manga_category` (`categoryId`)",
                )
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SQLiteConnection) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `library_updates` (" +
                        "`itemId` TEXT NOT NULL, `key` TEXT NOT NULL, `name` TEXT NOT NULL, " +
                        "`chapterNumber` REAL, `fetchedAt` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`itemId`, `key`))",
                )
            }
        }

        val ALL_MIGRATIONS = arrayOf(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
    }
}
