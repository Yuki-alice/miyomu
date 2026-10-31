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

@Database(entities = [AppMetaEntity::class], version = 1)
@ConstructedBy(MiyomuDatabaseConstructor::class)
abstract class MiyomuDatabase : RoomDatabase() {
    abstract fun appMetaDao(): AppMetaDao
}

@Suppress("KotlinNoActualForExpect")
expect object MiyomuDatabaseConstructor : RoomDatabaseConstructor<MiyomuDatabase> {
    override fun initialize(): MiyomuDatabase
}
