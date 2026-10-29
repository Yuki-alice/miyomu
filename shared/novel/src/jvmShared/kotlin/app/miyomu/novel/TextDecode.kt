package app.miyomu.novel

import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets

/** Text file decoding shared by the TXT, Markdown and XML-based novel formats. */
internal object TextDecode {

    /** BOM first, then strict UTF-8, then GB18030 (covers GBK/GB2312). */
    fun decode(bytes: ByteArray): String {
        when {
            bytes.size >= 3 &&
                bytes[0] == 0xEF.toByte() && bytes[1] == 0xBB.toByte() && bytes[2] == 0xBF.toByte() ->
                return String(bytes, 3, bytes.size - 3, StandardCharsets.UTF_8)

            bytes.size >= 2 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xFE.toByte() ->
                return String(bytes, 2, bytes.size - 2, StandardCharsets.UTF_16LE)

            bytes.size >= 2 && bytes[0] == 0xFE.toByte() && bytes[1] == 0xFF.toByte() ->
                return String(bytes, 2, bytes.size - 2, StandardCharsets.UTF_16BE)
        }

        try {
            return StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(bytes))
                .toString()
        } catch (_: CharacterCodingException) {
            // Not valid UTF-8; fall through to GB18030.
        }
        return String(bytes, charset("GB18030"))
    }
}
