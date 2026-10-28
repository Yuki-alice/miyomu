package app.miyomu.data

import android.content.Context
import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import app.miyomu.data.db.MiyomuDatabase
import kotlinx.coroutines.Dispatchers

class AndroidMiyomuDatabaseFactory(
    private val context: Context,
    private val databaseName: String = "miyomu.db",
) : MiyomuDatabaseFactory {
    override fun createDatabase(): MiyomuDatabase =
        Room.databaseBuilder<MiyomuDatabase>(context, databaseName)
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .build()
}
