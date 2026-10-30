package app.miyomu.novel

/**
 * Reads image dimensions from the file header, so pagination can reserve space for an
 * illustration before it is decoded. Supports PNG, JPEG, GIF and WebP (VP8/VP8L/VP8X);
 * anything else returns null and the reader falls back to a default aspect ratio.
 */
object ImageHeaderSniffer {

    data class Info(val format: String, val width: Int, val height: Int)

    fun sniff(bytes: ByteArray): Info? = when {
        hasSignature(bytes, byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)) -> png(bytes)
        bytes.size >= 2 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte() -> jpeg(bytes)
        bytes.size >= 6 && String(bytes, 0, 4) == "GIF8" -> Info(
            format = "gif",
            width = u16le(bytes, 6),
            height = u16le(bytes, 8),
        ).takeIf { it.width > 0 && it.height > 0 }
        bytes.size >= 12 && String(bytes, 0, 4) == "RIFF" && String(bytes, 8, 4) == "WEBP" -> webp(bytes)
        else -> null
    }

    private fun png(bytes: ByteArray): Info? {
        if (bytes.size < 24) return null
        val width = i32be(bytes, 16)
        val height = i32be(bytes, 20)
        return Info("png", width, height).takeIf { it.width > 0 && it.height > 0 }
    }

    private fun jpeg(bytes: ByteArray): Info? {
        var i = 2
        while (i + 9 < bytes.size) {
            if (bytes[i] != 0xFF.toByte()) {
                i++
                continue
            }
            val marker = bytes[i + 1].toInt() and 0xFF
            if (marker == 0xFF || marker == 0x00) {
                i++
                continue
            }
            if (marker in 0xC0..0xCF && marker != 0xC4 && marker != 0xC8 && marker != 0xCC) {
                // SOFn: length(2) precision(1) height(2) width(2)
                if (i + 9 >= bytes.size) return null
                val height = u16be(bytes, i + 5)
                val width = u16be(bytes, i + 7)
                return Info("jpeg", width, height).takeIf { it.width > 0 && it.height > 0 }
            }
            val length = u16be(bytes, i + 2)
            if (length <= 0) return null
            i += 2 + length
        }
        return null
    }

    private fun webp(bytes: ByteArray): Info? {
        if (bytes.size < 26) return null
        return when (val chunk = String(bytes, 12, 4)) {
            "VP8 " -> {
                // Lossy: dims at offset 26 (14 bits each after the 3-byte start code).
                val width = (u16le(bytes, 26) and 0x3FFF)
                val height = (u16le(bytes, 28) and 0x3FFF)
                Info("webp", width, height).takeIf { it.width > 0 && it.height > 0 }
            }
            "VP8L" -> {
                // Lossless: 14-bit dims packed at offset 21.
                val bits = u32le(bytes, 21).toInt() and 0x0FFFFFFF
                val width = (bits and 0x3FFF) + 1
                val height = ((bits shr 14) and 0x3FFF) + 1
                Info("webp", width, height)
            }
            "VP8X" -> {
                // Extended: 24-bit canvas size minus one at offset 24.
                val width = le24(bytes, 24) + 1
                val height = le24(bytes, 27) + 1
                Info("webp", width, height).takeIf { it.width > 0 && it.height > 0 }
            }
            else -> null.also { if (chunk !in setOf("VP8 ", "VP8L", "VP8X")) { /* unknown chunk */ } }
        }
    }

    private fun hasSignature(bytes: ByteArray, signature: ByteArray): Boolean =
        bytes.size >= signature.size && signature.indices.all { bytes[it] == signature[it] }

    private fun u16be(bytes: ByteArray, at: Int): Int =
        ((bytes[at].toInt() and 0xFF) shl 8) or (bytes[at + 1].toInt() and 0xFF)

    private fun u16le(bytes: ByteArray, at: Int): Int =
        (bytes[at].toInt() and 0xFF) or ((bytes[at + 1].toInt() and 0xFF) shl 8)

    private fun u32le(bytes: ByteArray, at: Int): Long =
        (bytes[at].toLong() and 0xFF) or
            ((bytes[at + 1].toLong() and 0xFF) shl 8) or
            ((bytes[at + 2].toLong() and 0xFF) shl 16) or
            ((bytes[at + 3].toLong() and 0xFF) shl 24)

    private fun le24(bytes: ByteArray, at: Int): Int =
        (bytes[at].toInt() and 0xFF) or
            ((bytes[at + 1].toInt() and 0xFF) shl 8) or
            ((bytes[at + 2].toInt() and 0xFF) shl 16)

    private fun i32be(bytes: ByteArray, at: Int): Int =
        ((bytes[at].toInt() and 0xFF) shl 24) or
            ((bytes[at + 1].toInt() and 0xFF) shl 16) or
            ((bytes[at + 2].toInt() and 0xFF) shl 8) or
            (bytes[at + 3].toInt() and 0xFF)
}
