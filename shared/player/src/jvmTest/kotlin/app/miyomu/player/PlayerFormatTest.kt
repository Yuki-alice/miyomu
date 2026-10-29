package app.miyomu.player

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.openani.mediamp.InternalMediampApi
import org.openani.mediamp.metadata.AudioTrack
import org.openani.mediamp.metadata.SubtitleTrack
import org.openani.mediamp.metadata.TrackLabel

class PlayerFormatTest {

    @Test
    fun `formats playback speeds`() {
        assertEquals("0.5x", formatSpeed(0.5f))
        assertEquals("0.75x", formatSpeed(0.75f))
        assertEquals("1.0x", formatSpeed(1f))
        assertEquals("1.25x", formatSpeed(1.25f))
        assertEquals("2.0x", formatSpeed(2f))
    }

    @OptIn(InternalMediampApi::class)
    @Test
    fun `audio track prefers labels then name`() {
        val labelled = AudioTrack("a1", "1", "English", listOf(TrackLabel("en", "English (US)")))
        assertEquals("English (US)", labelled.displayName())

        val named = AudioTrack("a2", "2", "Japanese", emptyList())
        assertEquals("Japanese", named.displayName())

        val bare = AudioTrack("a3", "3", null, listOf(TrackLabel("und", " ")))
        assertEquals("3", bare.displayName())
    }

    @OptIn(InternalMediampApi::class)
    @Test
    fun `subtitle track falls back to language then id`() {
        val labelled = SubtitleTrack("s1", "1", "zh", listOf(TrackLabel("zh", "简体中文")))
        assertEquals("简体中文", labelled.displayName())

        val languageOnly = SubtitleTrack("s2", "2", "en", emptyList())
        assertEquals("en", languageOnly.displayName())

        val bare = SubtitleTrack("s3", "3", null, emptyList())
        assertEquals("3", bare.displayName())
    }
}
