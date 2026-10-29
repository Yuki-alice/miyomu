package app.miyomu.ui

import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import app.miyomu.data.JvmMiyomuDatabaseFactory
import app.miyomu.data.createMiyomuData
import app.miyomu.local.FileLocalComicRepository
import app.miyomu.local.FileLocalFileRef
import app.miyomu.local.LocalLibraryRepository
import app.miyomu.player.isVideoPlaybackAvailable
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

@OptIn(ExperimentalTestApi::class)
class VideoDetailFlowTest {

    @TempDir
    lateinit var root: File

    @Test
    fun videoBookOpensAnimeDetailAndPlayer() = runComposeUiTest {
        File(root, "Episode 1.mkv").writeBytes(byteArrayOf(1, 2, 3))

        val data = createMiyomuData(
            JvmMiyomuDatabaseFactory(File(root, "test.db").absolutePath),
        )

        try {
            setContent {
                MiyomuApp(
                    library = LocalLibraryRepository(),
                    comics = FileLocalComicRepository(),
                    data = data,
                    root = FileLocalFileRef(root),
                    deviceId = "test",
                    nowMillis = { 1_000L },
                )
            }

            waitUntil(timeoutMillis = 5_000) {
                onAllNodesWithText("Episode 1").fetchSemanticsNodes().isNotEmpty()
            }
            onNodeWithText("Episode 1").performClick()

            // The anime detail header names the local source; the episode list repeats the title.
            waitUntil(timeoutMillis = 5_000) {
                onAllNodesWithText("本地文件").fetchSemanticsNodes().isNotEmpty()
            }
            waitUntil(timeoutMillis = 5_000) {
                onAllNodesWithText("Episode 1").fetchSemanticsNodes().isNotEmpty()
            }
            capture("video-detail")

            val actionLabels = listOf("开始", "Start", "继续观看", "Resume")
            waitUntil(timeoutMillis = 5_000) {
                actionLabels.any { label -> onAllNodesWithText(label).fetchSemanticsNodes().isNotEmpty() }
            }
            val actionLabel = actionLabels.first { label ->
                onAllNodesWithText(label).fetchSemanticsNodes().isNotEmpty()
            }
            onNodeWithText(actionLabel).performClick()

            // macOS/Windows bundle the LGPL MPV runtime (ADR-0012 option A); Linux does not.
            if (isVideoPlaybackAvailable()) {
                waitUntil(timeoutMillis = 5_000) {
                    onAllNodesWithContentDescription("返回").fetchSemanticsNodes().isNotEmpty()
                }
                capture("video-player")
            } else {
                waitUntil(timeoutMillis = 5_000) {
                    onAllNodesWithText("此版本未内置桌面播放器").fetchSemanticsNodes().isNotEmpty()
                }
                capture("video-player-unavailable")
            }
        } finally {
            data.close()
        }
    }

    @Test
    fun videoFolderListsEpisodes() = runComposeUiTest {
        val show = File(root, "Show").apply { mkdirs() }
        File(show, "S01E01.mkv").writeBytes(byteArrayOf(1))
        File(show, "S01E02.mkv").writeBytes(byteArrayOf(1))

        val data = createMiyomuData(
            JvmMiyomuDatabaseFactory(File(root, "episodes.db").absolutePath),
        )

        try {
            setContent {
                MiyomuApp(
                    library = LocalLibraryRepository(),
                    comics = FileLocalComicRepository(),
                    data = data,
                    root = FileLocalFileRef(root),
                    deviceId = "test",
                    nowMillis = { 1_000L },
                )
            }

            waitUntil(timeoutMillis = 5_000) {
                onAllNodesWithText("Show").fetchSemanticsNodes().isNotEmpty()
            }
            onNodeWithText("Show").performClick()

            waitUntil(timeoutMillis = 5_000) {
                onAllNodesWithText("S01E01").fetchSemanticsNodes().isNotEmpty()
            }
            waitUntil(timeoutMillis = 5_000) {
                onAllNodesWithText("S01E02").fetchSemanticsNodes().isNotEmpty()
            }
            capture("video-episodes")
        } finally {
            data.close()
        }
    }

    @Test
    fun seasonFoldersOpenSeasonDetail() = runComposeUiTest {
        val show = File(root, "Show").apply { mkdirs() }
        File(show, "Season 1").apply { mkdirs() }
        File(show, "Season 1/E01.mkv").writeBytes(byteArrayOf(1))
        File(show, "Season 2").apply { mkdirs() }
        File(show, "Season 2/E01.mkv").writeBytes(byteArrayOf(1))

        val data = createMiyomuData(
            JvmMiyomuDatabaseFactory(File(root, "seasons.db").absolutePath),
        )

        try {
            setContent {
                MiyomuApp(
                    library = LocalLibraryRepository(),
                    comics = FileLocalComicRepository(),
                    data = data,
                    root = FileLocalFileRef(root),
                    deviceId = "test",
                    nowMillis = { 1_000L },
                )
            }

            waitUntil(timeoutMillis = 5_000) {
                onAllNodesWithText("Show").fetchSemanticsNodes().isNotEmpty()
            }
            onNodeWithText("Show").performClick()

            waitUntil(timeoutMillis = 5_000) {
                onAllNodesWithText("Season 1").fetchSemanticsNodes().isNotEmpty()
            }
            capture("video-seasons")
            onNodeWithText("Season 1").performClick()

            waitUntil(timeoutMillis = 5_000) {
                onAllNodesWithText("E01").fetchSemanticsNodes().isNotEmpty()
            }
        } finally {
            data.close()
        }
    }

    @Test
    fun filterActionOpensEpisodeSettings() = runComposeUiTest {
        val show = File(root, "Show").apply { mkdirs() }
        File(show, "S01E01.mkv").writeBytes(byteArrayOf(1))

        val data = createMiyomuData(
            JvmMiyomuDatabaseFactory(File(root, "settings.db").absolutePath),
        )

        try {
            setContent {
                MiyomuApp(
                    library = LocalLibraryRepository(),
                    comics = FileLocalComicRepository(),
                    data = data,
                    root = FileLocalFileRef(root),
                    deviceId = "test",
                    nowMillis = { 1_000L },
                )
            }

            waitUntil(timeoutMillis = 5_000) {
                onAllNodesWithText("Show").fetchSemanticsNodes().isNotEmpty()
            }
            onNodeWithText("Show").performClick()
            waitUntil(timeoutMillis = 5_000) {
                onAllNodesWithText("S01E01").fetchSemanticsNodes().isNotEmpty()
            }

            val filterLabels = listOf("Filter", "筛选")
            val filterLabel = filterLabels.firstOrNull { label ->
                onAllNodesWithContentDescription(label).fetchSemanticsNodes().isNotEmpty()
            } ?: error("filter action not found")
            onNodeWithContentDescription(filterLabel).performClick()

            waitUntil(timeoutMillis = 5_000) {
                listOf("Filter", "筛选").any { label ->
                    onAllNodesWithText(label).fetchSemanticsNodes().isNotEmpty()
                }
            }
            waitUntil(timeoutMillis = 5_000) {
                listOf("Display", "显示").any { label ->
                    onAllNodesWithText(label).fetchSemanticsNodes().isNotEmpty()
                }
            }
        } finally {
            data.close()
        }
    }

    private fun androidx.compose.ui.test.ComposeUiTest.capture(name: String) {
        val directory = File("build/reports/miyomu-ui").apply { mkdirs() }
        val bitmap = onRoot().captureToImage().asSkiaBitmap()
        val bytes = Image.makeFromBitmap(bitmap).encodeToData(EncodedImageFormat.PNG)
            ?: error("failed to encode $name")
        File(directory, "$name.png").writeBytes(bytes.bytes)
    }
}
