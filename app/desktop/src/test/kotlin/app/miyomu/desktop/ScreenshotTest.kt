package app.miyomu.desktop

import androidx.compose.ui.ImageComposeScene
import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import app.miyomu.MiyomuApp
import app.miyomu.core.database.MiyomuDatabase
import app.miyomu.core.database.MiyomuDatabaseConstructor
import org.jetbrains.skia.EncodedImageFormat
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class ScreenshotTest {
    @Test
    fun renderHome() {
        val database = Room.inMemoryDatabaseBuilder<MiyomuDatabase> {
            MiyomuDatabaseConstructor.initialize()
        }.setDriver(BundledSQLiteDriver()).build()
        val scene = ImageComposeScene(width = 1280, height = 820) {
            MiyomuApp(database)
        }
        val image = scene.render()
        val data = image.encodeToData(EncodedImageFormat.PNG)
        val output = File("build/screenshots/home.png").apply { parentFile.mkdirs() }
        output.writeBytes(requireNotNull(data).bytes)
        scene.close()
        database.close()
        assertTrue(output.length() > 10_000)
    }
}
