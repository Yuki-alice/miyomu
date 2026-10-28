package org.projectv.spike

import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.runComposeUiTest
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class AppSmokeTest {

    @Test
    fun libraryDetailSettingsAndStress() = runComposeUiTest {
        setContent { App() }

        onNodeWithText("Aster Bloom").assertExists()
        capture("s3-library")

        onNodeWithTag("comic-card-1").performClick()
        onNodeWithText("Chapters · 128").assertExists()
        capture("s3-detail")

        onNodeWithTag("nav-settings").performClick()
        onNodeWithText("Dark theme").assertExists()
        onNodeWithText("No sources are bundled. Repositories are added by the user.").assertExists()
        capture("s3-settings")

        onNodeWithTag("nav-library").performClick()
        onNodeWithTag("stress-toggle").performClick()
        onNodeWithText("Stress Title 1").assertExists()

        val start = System.nanoTime()
        onNodeWithTag("library-grid").performScrollToIndex(999)
        onNodeWithText("Stress Title 1000").assertExists()
        val elapsedMs = (System.nanoTime() - start) / 1_000_000

        println("S3_STRESS_1000_SCROLL_MS $elapsedMs")
        assertTrue(elapsedMs < 5_000, "Stress grid scroll took ${elapsedMs}ms")
        capture("s3-stress")
    }

    private fun androidx.compose.ui.test.ComposeUiTest.capture(name: String) {
        val directory = File("build/reports/s3-ui").apply { mkdirs() }
        val bitmap = onRoot().captureToImage().asSkiaBitmap()
        val data = Image.makeFromBitmap(bitmap).encodeToData(EncodedImageFormat.PNG)
            ?: error("Failed to encode $name")
        File(directory, "$name.png").writeBytes(data.bytes)
    }
}
