package app.miyomu.desktop

import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import app.miyomu.application.MiyomuAppEnvironment
import app.miyomu.application.MiyomuApplication
import app.miyomu.data.JvmMiyomuDatabaseFactory
import app.miyomu.local.FileLocalFileRef
import java.awt.Desktop
import java.io.File

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Miyomu",
        state = rememberWindowState(width = 1200.dp, height = 820.dp),
    ) {
        val environment = remember {
            val home = File(System.getProperty("user.home"), ".miyomu").apply { mkdirs() }
            val libraryRoot = File(home, "library").apply { mkdirs() }
            MiyomuAppEnvironment(
                dataDir = home,
                libraryRoot = FileLocalFileRef(libraryRoot),
                databaseFactory = JvmMiyomuDatabaseFactory(File(home, "miyomu.db").absolutePath),
                deviceId = "desktop",
                nowMillis = System::currentTimeMillis,
                onOpenLibraryFolder = {
                    runCatching { Desktop.getDesktop().open(libraryRoot) }
                },
                onExit = { exitApplication() },
            )
        }
        MiyomuApplication(environment)
    }
}
