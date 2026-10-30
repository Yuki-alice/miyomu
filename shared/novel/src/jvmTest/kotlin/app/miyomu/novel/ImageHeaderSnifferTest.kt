package app.miyomu.novel

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ImageHeaderSnifferTest {

    @Test
    fun `reads png dimensions from IHDR`() {
        val bytes = byteArrayOf(
            0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
            0, 0, 0, 13, 0x49, 0x48, 0x44, 0x52,
            0, 0, 0, 24, // width 24
            0, 0, 0, 36, // height 36
            8, 6, 0, 0, 0,
        )
        assertEquals(ImageHeaderSniffer.Info("png", 24, 36), ImageHeaderSniffer.sniff(bytes))
    }

    @Test
    fun `reads jpeg dimensions from SOF marker`() {
        val bytes = byteArrayOf(
            0xFF.toByte(), 0xD8.toByte(),
            0xFF.toByte(), 0xE0.toByte(), 0, 4, 0x4A, 0x46, 0x49, 0x46, 0, // APP0
            0xFF.toByte(), 0xC0.toByte(), 0, 11, 8, 0, 36, 0, 24, 3, 1, 0x11, 0, // SOF0 h=36 w=24
            0xFF.toByte(), 0xDA.toByte(), 0, 2,
        )
        assertEquals(ImageHeaderSniffer.Info("jpeg", 24, 36), ImageHeaderSniffer.sniff(bytes))
    }

    @Test
    fun `reads gif dimensions`() {
        val bytes = "GIF89a".toByteArray() +
            byteArrayOf(24, 0, 36, 0) + // 24x36 little-endian
            byteArrayOf(0x70.toByte(), 0, 0)
        assertEquals(ImageHeaderSniffer.Info("gif", 24, 36), ImageHeaderSniffer.sniff(bytes))
    }

    @Test
    fun `reads webp vp8x canvas size`() {
        val bytes = "RIFF".toByteArray() + byteArrayOf(0, 0, 0, 0) + "WEBP".toByteArray() +
            "VP8X".toByteArray() + byteArrayOf(0, 0, 0, 0) + // chunk size
            byteArrayOf(0, 0, 0, 0) + // flags
            byteArrayOf(23, 0, 0, 35, 0, 0) // canvas 24x36 (minus-one, 3 bytes each)
        assertEquals(ImageHeaderSniffer.Info("webp", 24, 36), ImageHeaderSniffer.sniff(bytes))
    }

    @Test
    fun `unknown bytes return null`() {
        assertNull(ImageHeaderSniffer.sniff("plain text".toByteArray()))
        assertNull(ImageHeaderSniffer.sniff(ByteArray(0)))
    }
}
