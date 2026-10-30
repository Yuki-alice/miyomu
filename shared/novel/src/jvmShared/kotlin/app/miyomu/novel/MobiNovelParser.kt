package app.miyomu.novel

import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets

/**
 * Parses unencrypted MOBI files (PalmDOC compression) by decompressing the text records into
 * HTML markup and delegating chapter extraction to [HtmlNovelParser].
 *
 * Supported: uncompressed (1) and PalmDOC (2) text records, MOBI file version < 8.
 * Rejected with an actionable error: DRM (encryption != 0), HUFF/CDIC (compression 17480),
 * KF8-only containers (version >= 8, i.e. AZW3) and the variable-width trailing-data flag —
 * those need a conversion to EPUB/MOBI first. Trailing extra-data entries (extra flags
 * bits 1-15) are trimmed before decompression per the MOBI header's extra flags.
 */
class MobiNovelParser(
    private val htmlParser: HtmlNovelParser = HtmlNovelParser(),
    val maxBytes: Long = DEFAULT_MAX_BYTES,
) {

    fun parse(fileName: String, bytes: ByteArray): ParsedNovel {
        if (bytes.size.toLong() > maxBytes) {
            throw NovelParseException(
                "MOBI 文件 ${bytes.size} 字节，超过上限 $maxBytes；如需支持请调大 MobiNovelParser.maxBytes",
            )
        }
        val recordOffsets = pdbRecordOffsets(bytes)
        require(recordOffsets.size >= 2) { "MOBI 无文本记录" }
        val record0 = recordOffsets[0].toInt()
        val recordEnd = recordOffsets[1].toInt()

        fun u16(offset: Int) = ((bytes[offset].toInt() and 0xFF) shl 8) or (bytes[offset + 1].toInt() and 0xFF)
        fun u32(offset: Int): Int {
            var value = 0
            for (i in 0 until 4) value = (value shl 8) or (bytes[offset + i].toInt() and 0xFF)
            return value
        }

        val compression = u16(record0)
        val textLength = u32(record0 + 4)
        val recordCount = u16(record0 + 8)
        val encryption = u16(record0 + 12)
        if (encryption != 0) {
            throw NovelParseException("加密的 MOBI（DRM）不受支持，请先解除 DRM 或转换为 EPUB")
        }
        when (compression) {
            COMPRESSION_NONE, COMPRESSION_PALMDOC -> Unit
            COMPRESSION_HUFFCDIC -> throw NovelParseException(
                "该 MOBI 使用 HUFF/CDIC 压缩，暂不支持；请转换为 EPUB 或重新生成 MOBI",
            )
            else -> throw NovelParseException("未知的 MOBI 压缩类型：$compression")
        }

        var textEncoding = 65001
        var extraFlags = 0
        if (recordEnd - record0 > MOBI_HEADER_START + 8 && hasAscii(bytes, record0 + MOBI_HEADER_START, "MOBI")) {
            val headerLength = u32(record0 + MOBI_HEADER_START + 4)
            val fileVersion = u32(record0 + MOBI_HEADER_START + 20)
            if (fileVersion >= 8) {
                throw NovelParseException("AZW3/KF8 格式暂不支持，请转换为 EPUB 或 MOBI 后导入")
            }
            textEncoding = u32(record0 + MOBI_HEADER_START + 12)
            if (headerLength >= 0xE4) {
                val flagsOffset = record0 + MOBI_HEADER_START + 0xF0
                if (flagsOffset + 2 <= recordEnd) {
                    extraFlags = u16(flagsOffset)
                }
            }
        }
        if (extraFlags and EXTRA_FLAG_VARIABLE_WIDTH != 0) {
            throw NovelParseException("该 MOBI 使用可变宽度尾随数据（extra flag 0x1），暂不支持；请转换为 EPUB")
        }

        val htmlBytes = ByteArrayOutputStream(textLength.coerceAtLeast(0))
        for (record in 1..recordCount) {
            val start = recordOffsets[record].toInt()
            val end = recordOffsets.getOrNull(record + 1)?.toInt() ?: bytes.size
            val trimmed = trimTrailingBytes(bytes, start, end, extraFlags)
            decompressPalmDoc(trimmed, compressed = compression == COMPRESSION_PALMDOC, htmlBytes)
        }

        val markup = when (textEncoding) {
            1252 -> String(htmlBytes.toByteArray(), 0, htmlBytes.size(), charset("windows-1252"))
                .toByteArray(StandardCharsets.UTF_8)
            else -> htmlBytes.toByteArray()
        }
        return htmlParser.parse(fileName, markup)
    }

    /** PDB record offsets: 78-byte header, then one 8-byte entry per record. */
    private fun pdbRecordOffsets(bytes: ByteArray): List<Int> {
        fun u16(offset: Int) = ((bytes[offset].toInt() and 0xFF) shl 8) or (bytes[offset + 1].toInt() and 0xFF)
        fun u32(offset: Int): Int {
            var value = 0
            for (i in 0 until 4) value = (value shl 8) or (bytes[offset + i].toInt() and 0xFF)
            return value
        }
        val count = u16(PDB_NUM_RECORDS_OFFSET)
        return (0 until count).map { index ->
            val entry = PDB_HEADER_SIZE + index * 8
            u32(entry)
        }
    }

    /** Removes the trailing extra-data entries selected by [extraFlags] (bits 1-15). */
    private fun trimTrailingBytes(bytes: ByteArray, start: Int, end: Int, extraFlags: Int): ByteArray {
        var end = end
        for (bit in 15 downTo 1) {
            if (extraFlags and (1 shl bit) == 0) continue
            // The encoded size counts the entry's payload bytes only (KindleUnpack semantics).
            end -= readBackwardSize(bytes, start, end)
        }
        return bytes.copyOfRange(start, end)
    }

    /**
     * Backward-encoded size at [end]: up to four bytes read backwards, continuation detected by
     * a set high bit (which stops the scan without being part of the number).
     */
    private fun readBackwardSize(bytes: ByteArray, start: Int, end: Int): Int {
        var size = 0
        for (i in 0 until 4) {
            val index = end - 1 - i
            if (index < start) break
            val v = bytes[index].toInt() and 0xFF
            if (v and 0x80 != 0) break
            size = (size shl 7) or v
        }
        return size
    }

    private fun decompressPalmDoc(input: ByteArray, compressed: Boolean, out: ByteArrayOutputStream) {
        if (!compressed) {
            out.write(input)
            return
        }
        val buffer = ByteAccumulator(input.size)
        var i = 0
        while (i < input.size) {
            val b = input[i].toInt() and 0xFF
            when {
                b == 0 -> {
                    // Escape: the next byte is a literal (covers 0x00 and >= 0x80 bytes).
                    if (i + 1 >= input.size) break
                    buffer.write(input[i + 1].toInt() and 0xFF)
                    i += 2
                }
                b < 8 -> {
                    // 1..8 literal run.
                    val run = b.coerceAtMost(input.size - i - 1)
                    buffer.write(input, i + 1, run)
                    i += 1 + b
                }
                b < 0x80 -> {
                    buffer.write(b)
                    i += 1
                }
                b < 0xC0 -> {
                    // LZ77 pair: 11-bit distance, 3-bit length (+3).
                    if (i + 1 >= input.size) break
                    val pair = (b shl 8) or (input[i + 1].toInt() and 0xFF)
                    val distance = (pair shr 3) and 0x7FF
                    val length = (pair and 0x7) + 3
                    repeat(length) { k -> buffer.write(buffer.byteAt(buffer.length - distance)) }
                    i += 2
                }
                else -> {
                    // Space + char pair; the next byte is processed independently.
                    buffer.write(' '.code)
                    buffer.write(b and 0x7F)
                    i += 1
                }
            }
        }
        out.write(buffer.toByteArray())
    }

    private fun hasAscii(bytes: ByteArray, offset: Int, text: String): Boolean {
        if (offset + text.length > bytes.size) return false
        for ((index, expected) in text.withIndex()) {
            if (bytes[offset + index].toInt() != expected.code) return false
        }
        return true
    }

    private class ByteAccumulator(initial: Int) {
        private var buffer = ByteArray(initial.coerceAtLeast(64))
        private var size = 0

        val length: Int get() = size

        fun write(b: Int) {
            ensure(1)
            buffer[size++] = b.toByte()
        }

        fun write(bytes: ByteArray, offset: Int, length: Int) {
            if (length <= 0) return
            ensure(length)
            System.arraycopy(bytes, offset, buffer, size, length)
            size += length
        }

        fun byteAt(index: Int): Int = buffer[index].toInt() and 0xFF

        fun toByteArray(): ByteArray = buffer.copyOf(size)

        private fun ensure(extra: Int) {
            if (size + extra <= buffer.size) return
            var capacity = buffer.size
            while (capacity < size + extra) capacity *= 2
            buffer = buffer.copyOf(capacity)
        }
    }

    companion object {
        const val DEFAULT_MAX_BYTES: Long = 64L * 1024 * 1024
        const val COMPRESSION_NONE = 1
        const val COMPRESSION_PALMDOC = 2
        const val COMPRESSION_HUFFCDIC = 17480
        const val EXTRA_FLAG_VARIABLE_WIDTH = 1
        const val PDB_HEADER_SIZE = 78
        const val PDB_NUM_RECORDS_OFFSET = 76
        const val MOBI_HEADER_START = 16
    }
}
