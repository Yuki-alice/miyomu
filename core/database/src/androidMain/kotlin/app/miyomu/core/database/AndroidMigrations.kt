package app.miyomu.core.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** Android uses the framework SQLite driver, so it needs the SupportSQLiteDatabase overload. */
object AndroidMigration1To2 : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        MIGRATION_1_2_SQL.forEach(database::execSQL)
    }
}
