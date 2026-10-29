package app.miyomu.player

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class FormatTimeTest {

    @Test
    fun `formats minutes and seconds`() {
        assertEquals("00:00", formatTime(0))
        assertEquals("00:05", formatTime(5_000))
        assertEquals("01:05", formatTime(65_000))
        assertEquals("59:59", formatTime(3_599_000))
    }

    @Test
    fun `formats hours when needed`() {
        assertEquals("1:00:00", formatTime(3_600_000))
        assertEquals("2:03:04", formatTime(7_384_000))
    }

    @Test
    fun `negative positions render as zero`() {
        assertEquals("00:00", formatTime(-1))
    }
}
