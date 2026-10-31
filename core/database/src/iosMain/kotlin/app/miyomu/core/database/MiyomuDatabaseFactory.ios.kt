package app.miyomu.core.database

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver

fun createMiyomuDatabase(path: String): MiyomuDatabase =
    Room.databaseBuilder<MiyomuDatabase>(name = path)
        .setDriver(BundledSQLiteDriver())
        .addMigrations(MIGRATION_1_2)
        .build()
