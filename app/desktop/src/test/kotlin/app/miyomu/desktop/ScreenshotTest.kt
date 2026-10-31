package app.miyomu.desktop

import androidx.compose.ui.ImageComposeScene
import app.miyomu.MiyomuApp
import org.jetbrains.skia.EncodedImageFormat
import org.junit.jupiter.api.Test
import java.io.File
import org.junit.jupiter.api.Assertions.assertTrue

class ScreenshotTest {
    @Test
    fun renderHome() {
        val scene = ImageComposeScene(width = 1280, height = 820) {
            MiyomuApp()
        }
        val image = scene.render()
        val data = image.encodeToData(EncodedImageFormat.PNG)
        val output = File("build/screenshots/home.png").apply { parentFile.mkdirs() }
        output.writeBytes(requireNotNull(data).bytes)
        scene.close()
        assertTrue(output.length() > 10_000)
    }
}
