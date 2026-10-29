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
    ],
    version = 2,
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

        val ALL_MIGRATIONS = arrayOf(MIGRATION_1_2)
    }
}
