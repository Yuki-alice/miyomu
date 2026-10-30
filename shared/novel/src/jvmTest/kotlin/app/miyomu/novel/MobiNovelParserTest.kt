package app.miyomu.novel

import java.io.ByteArrayOutputStream
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * P1-1 regression net for MOBI: header parsing, PalmDOC decompression (literals, escapes,
 * back-references), trailing-entry trimming, and the visible errors for the unsupported
 * variants (DRM, HUFF/CDIC, AZW3/KF8).
 */
class MobiNovelParserTest {

    private val parser = MobiNovelParser()

    @Test
    fun `parses palmdoc mobi with ascii html`() {
        val html = "<html><body><p>The story begins here.</p><p>It continues on.</p></body></html>"
        val bytes = buildMobi(
            textRecords = listOf(palmDocLiterals(html)),
            textLength = html.length,
        )

        val parsed = parser.parse("sample.mobi", bytes)

        assertEquals("sample", parsed.title)
        val text = parsed.chapters.single().text()
        assertTrue("The story begins here." in text, "actual: $text")
        assertTrue("It continues on." in text)
    }

    @Test
    fun `decompresses palm doc back references`() {
        // "abc" as literals, then a pair copying 3 bytes from distance 3 -> "abcabc".
        val stream = palmDocLiterals("abc") + byteArrayOf(0x80.toByte(), 0x18)
        val bytes = buildMobi(
            textRecords = listOf(stream),
            textLength = 6,
        )

        val parsed = parser.parse("refs.mobi", bytes)

        assertEquals("abcabc", parsed.chapters.single().text().trim())
    }

    @Test
    fun `trims trailing entries selected by extra flags`() {
        val html = "<html><body><p>Trailing entry test body.</p></body></html>"
        // Record payload: text + a 4-byte trailing entry encoded as [payload][backward size].
        val payload = palmDocLiterals(html) +
            byteArrayOf(0xAA.toByte(), 0xBB.toByte(), 0xCC.toByte(), 0xDD.toByte()) +
            byteArrayOf(0x04)
        val bytes = buildMobi(
            textRecords = listOf(payload),
            textLength = html.length,
            extraFlags = 1 shl 2,
        )

        val parsed = parser.parse("trailing.mobi", bytes)

        assertTrue("Trailing entry test body." in parsed.chapters.single().text())
    }

    @Test
    fun `rejects huffcdic compression with an actionable error`() {
        val bytes = buildMobi(textRecords = listOf(byteArrayOf(1)), textLength = 1, compression = 17480)

        val error = assertThrows<NovelParseException> { parser.parse("huff.mobi", bytes) }

        assertTrue("HUFF/CDIC" in error.message!!)
    }

    @Test
    fun `rejects drm encrypted mobi`() {
        val bytes = buildMobi(textRecords = listOf(byteArrayOf(1)), textLength = 1, encryption = 1)

        val error = assertThrows<NovelParseException> { parser.parse("drm.mobi", bytes) }

        assertTrue("DRM" in error.message!!)
    }

    @Test
    fun `rejects azw3 kf8 containers`() {
        val bytes = buildMobi(textRecords = listOf(byteArrayOf(1)), textLength = 1, fileVersion = 8)

        val error = assertThrows<NovelParseException> { parser.parse("book.azw3", bytes) }

        assertTrue("AZW3/KF8" in error.message!!)
    }

    // --- fixture builders -----------------------------------------------------------------

    /**
     * Builds a minimal PDB/MOBI file: PDB header + record list, record 0 = PalmDOC + MOBI
     * headers, then one record per [textRecords] entry (already PalmDOC-encoded streams).
     */
    private fun buildMobi(
        textRecords: List<ByteArray>,
        textLength: Int,
        compression: Int = 2,
        encryption: Int = 0,
        fileVersion: Int = 4,
        extraFlags: Int = 0,
    ): ByteArray {
        val record0Bytes = run {
            val out = ByteArrayOutputStream()
            fun u16(v: Int) {
                out.write((v shr 8) and 0xFF)
                out.write(v and 0xFF)
            }

            fun u32(v: Int) {
                out.write((v shr 24) and 0xFF)
                out.write((v shr 16) and 0xFF)
                out.write((v shr 8) and 0xFF)
                out.write(v and 0xFF)
            }

            // PalmDOC header
            u16(compression)
            u16(0)
            u32(textLength)
            u16(textRecords.size)
            u16(4096)
            u16(encryption)
            u16(0xFFFF)
            // MOBI header (232 bytes, extra flags at offset 0xF0)
            out.write("MOBI".toByteArray(Charsets.US_ASCII))
            val mobiHeaderLength = 232
            u32(mobiHeaderLength)
            u32(2) // mobiType: book
            u32(65001) // textEncoding: UTF-8
            u32(0) // unique id
            u32(fileVersion)
            // The record continues past the declared header length; extra flags sit at offset
            // 0xF0 of the MOBI header, so the tail must extend to 0xF2 bytes. mobiRest starts
            // at M+24 (after the identifier and five u32 fields), hence the -24 offset.
            val mobiRest = ByteArray(0xF2 - 24)
            mobiRest[0xF0 - 24] = ((extraFlags shr 8) and 0xFF).toByte()
            mobiRest[0xF1 - 24] = (extraFlags and 0xFF).toByte()
            out.write(mobiRest)
            // Pad record 0 a little past the extra-flags offset.
            while (out.size() < 16 + mobiHeaderLength + 16) out.write(0)
            out.toByteArray()
        }

        val records = listOf(record0Bytes) + textRecords
        val header = ByteArrayOutputStream()
        fun u16(v: Int) {
            header.write((v shr 8) and 0xFF)
            header.write(v and 0xFF)
        }

        fun u32(v: Int) {
            header.write((v shr 24) and 0xFF)
            header.write((v shr 16) and 0xFF)
            header.write((v shr 8) and 0xFF)
            header.write(v and 0xFF)
        }

        val name = "Sample".toByteArray(Charsets.US_ASCII)
        header.write(name + ByteArray(32 - name.size))
        u16(0) // attributes
        u16(0) // version
        u32(0); u32(0); u32(0); u32(0) // dates + modification number
        u32(0); u32(0) // appInfoID, sortInfoID
        header.write("BOOK".toByteArray(Charsets.US_ASCII))
        header.write("MOBI".toByteArray(Charsets.US_ASCII))
        u32(0) // uniqueIDseed
        u32(0) // nextRecordListID
        u16(records.size)
        // Record list starts right after the 78-byte header (no gap byte in this fixture).
        var offset = PDB_HEADER_SIZE + records.size * 8
        records.forEach { record ->
            u32(offset)
            header.write(0) // attributes (1 byte)
            header.write(0) // uniqueID (3 bytes, value irrelevant)
            header.write(0)
            header.write(0)
            offset += record.size
        }
        return header.toByteArray() + records.reduce { acc, bytes -> acc + bytes }
    }

    /**
     * Encodes ASCII text as a valid PalmDOC stream: every byte below 0x09 is escaped with a
     * leading 0x00, everything else is a plain literal.
     */
    private fun palmDocLiterals(text: String): ByteArray {
        val out = ByteArrayOutputStream()
        text.toByteArray(Charsets.US_ASCII).forEach { byte ->
            val value = byte.toInt() and 0xFF
            if (value < 0x09) {
                out.write(0)
            }
            out.write(value)
        }
        return out.toByteArray()
    }

    private companion object {
        const val PDB_HEADER_SIZE = 78
    }
}
