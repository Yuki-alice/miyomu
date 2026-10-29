package app.miyomu.player

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import org.openani.mediamp.MediampPlayer
import org.openani.mediamp.playUri
import java.io.File
import kotlin.coroutines.EmptyCoroutineContext

/**
 * macOS/Windows desktop builds bundle the LGPL MPV runtime (ADR-0012 option A). Linux has no
 * runtime on purpose, so these checks are skipped there.
 */
class DesktopRuntimeTest {

    @Test
    fun `bundled runtime is detected`() {
        assumeTrue(supportsBundledRuntime())
        assertTrue(isVideoPlaybackAvailable())
    }

    @Test
    fun `mpv player starts and stops`() {
        assumeTrue(isVideoPlaybackAvailable())
        val player = MediampPlayer(Unit, EmptyCoroutineContext)
        try {
            assertNotNull(player)
        } finally {
            player.close()
        }
    }

    @Test
    fun `mpv plays the bundled test video`() {
        assumeTrue(isVideoPlaybackAvailable())
        val video = File(requireNotNull(javaClass.getResource("/test-video.mp4")).toURI())

        runBlocking {
            val player = MediampPlayer(Unit, EmptyCoroutineContext)
            try {
                player.playUri(video.absolutePath, playWhenReady = true)
                withTimeout(15_000) {
                    player.currentPositionMillis.first { it > 500L }
                }
            } finally {
                player.close()
            }
        }
    }

    private fun supportsBundledRuntime(): Boolean {
        val os = System.getProperty("os.name").lowercase()
        return os.contains("mac") || os.contains("win")
    }
}
