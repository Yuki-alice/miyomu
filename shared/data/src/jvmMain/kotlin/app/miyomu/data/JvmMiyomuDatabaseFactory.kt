package app.miyomu.data

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import app.miyomu.data.db.MiyomuDatabase
import kotlinx.coroutines.Dispatchers
import java.io.File

class JvmMiyomuDatabaseFactory(
    private val databasePath: String,
) : MiyomuDatabaseFactory {
    override fun createDatabase(): MiyomuDatabase {
        File(databasePath).parentFile?.mkdirs()
        return Room.databaseBuilder<MiyomuDatabase>(name = databasePath)
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .addMigrations(*MiyomuDatabase.ALL_MIGRATIONS)
            .build()
    }
}
