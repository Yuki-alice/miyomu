package app.miyomu.desktop

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import app.miyomu.MiyomuApp
import app.miyomu.core.database.createMiyomuDatabase
import java.io.File

fun main() {
    val dataDir = File(System.getProperty("user.home"), ".miyomu").apply { mkdirs() }
    val database = createMiyomuDatabase(File(dataDir, "miyomu.db").absolutePath)

    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "Miyomu",
            state = rememberWindowState(width = 1280.dp, height = 820.dp),
        ) {
            MiyomuApp(database)
        }
    }
}
