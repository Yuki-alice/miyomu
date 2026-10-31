package app.miyomu.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import app.miyomu.MiyomuApp
import androidx.room.Room
import app.miyomu.core.database.MIGRATION_1_2
import app.miyomu.core.database.MiyomuDatabase

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val database = Room.databaseBuilder(
            applicationContext,
            MiyomuDatabase::class.java,
            "miyomu.db",
        ).addMigrations(MIGRATION_1_2).build()
        setContent { MiyomuApp(database) }
    }
}
