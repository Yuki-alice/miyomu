package app.miyomu.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [LibraryItemEntity::class, ReadingProgressEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class MiyomuDatabase : RoomDatabase() {
    abstract fun dao(): MiyomuDao
}
