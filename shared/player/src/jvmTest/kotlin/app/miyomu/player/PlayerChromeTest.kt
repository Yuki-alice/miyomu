package app.miyomu.player

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.openani.mediamp.InternalMediampApi
import org.openani.mediamp.metadata.AudioTrack
import org.openani.mediamp.metadata.SubtitleTrack

@OptIn(ExperimentalTestApi::class, InternalMediampApi::class)
class PlayerChromeTest {

    private val audioTracks = listOf(
        AudioTrack("a1", "1", "Japanese", emptyList()),
        AudioTrack("a2", "2", "English", emptyList()),
    )

    private val subtitleTracks = listOf(
        SubtitleTrack("s1", "1", "zh", emptyList()),
    )

    @Test
    fun `settings content selects speed audio and subtitles`() = runComposeUiTest {
        var speed: Float? = null
        var audio: AudioTrack? = null
        var subtitle: SubtitleTrack? = null

        setContent {
            MaterialTheme {
                PlayerSettingsContent(
                    speed = 1f,
                    onSpeedSelected = { speed = it },
                    audioCandidates = audioTracks,
                    selectedAudio = null,
                    onAudioSelected = { audio = it },
                    subtitleCandidates = subtitleTracks,
                    selectedSubtitle = null,
                    onSubtitleSelected = { subtitle = it },
                )
            }
        }

        onNodeWithText("播放速度").assertExists()
        onNodeWithText("1.5x").performClick()
        assertEquals(1.5f, speed)

        onNodeWithText("English").performClick()
        assertEquals(audioTracks[1], audio)

        onNodeWithText("关闭").performClick()
        assertEquals(null, subtitle)
        onNodeWithText("zh").performClick()
        assertEquals(subtitleTracks[0], subtitle)
    }

    @Test
    fun `settings content hides sections a backend cannot provide`() = runComposeUiTest {
        setContent {
            MaterialTheme {
                PlayerSettingsContent(
                    speed = null,
                    onSpeedSelected = {},
                    audioCandidates = audioTracks.take(1),
                    selectedAudio = null,
                    onAudioSelected = {},
                    subtitleCandidates = emptyList(),
                    selectedSubtitle = null,
                    onSubtitleSelected = {},
                )
            }
        }

        onNodeWithText("播放速度").assertDoesNotExist()
        onNodeWithText("音轨").assertDoesNotExist()
        onNodeWithText("字幕").assertDoesNotExist()
    }

    @Test
    fun `bottom bar shows time and dispatches actions`() = runComposeUiTest {
        var toggled = false
        var settingsOpened = false

        setContent {
            MaterialTheme {
                PlayerBottomBar(
                    positionMillis = 65_000L,
                    durationMillis = 3_600_000L,
                    playWhenReady = false,
                    buffering = false,
                    settingsEnabled = true,
                    onTogglePlay = { toggled = true },
                    onOpenSettings = { settingsOpened = true },
                    onScrub = {},
                    onScrubFinished = {},
                )
            }
        }

        onNodeWithText("01:05").assertExists()
        onNodeWithText("1:00:00").assertExists()
        onNodeWithContentDescription("播放").performClick()
        assertTrue(toggled)
        onNodeWithContentDescription("播放设置").performClick()
        assertTrue(settingsOpened)
    }
}
