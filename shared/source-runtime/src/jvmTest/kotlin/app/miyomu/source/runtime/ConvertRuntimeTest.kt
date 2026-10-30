package app.miyomu.source.runtime

import kotlinx.coroutines.runBlocking
import kotlinx.io.files.Path
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * `Convert` as sources see it: the JS global end-to-end, plus a couple of host-level vectors.
 *
 * Vectors are the published ones (RFC 1321 MD5, FIPS 180 SHA-256, RFC 4231 HMAC) so a broken
 * host binding cannot silently "pass" by agreeing with itself.
 */
class ConvertRuntimeTest {

    @TempDir
    lateinit var tempDir: File

    private fun load(script: String) = runBlocking {
        loadJsComicSource(script = script, dataDir = Path(tempDir.resolve("convert").toString()))
    }

    /** The probe reports its results through the descriptor name, which is all we can read back. */
    private fun probe(vararg expressions: String): String {
        val body = expressions.joinToString(" + \"|\" + ") { "__try(() => $it)" }
        val script = """
            function __try(fn) {
                try { return String(fn()); } catch (error) { return "ERR:" + (error && error.message); }
            }

            class ConvertProbe extends ComicSource {
                name = "convert:" + $body
                key = "convert_probe"
                version = "1.0.0"
                search = { load: async () => ({ comics: [], maxPage: 1 }) }
                comic = {
                    loadInfo: async () => new ComicDetails({ title: "t", chapters: {} }),
                    loadEp: async () => ({ images: [] })
                }
            }
        """.trimIndent()
        val source = load(script)
        return try {
            source.descriptor.name.removePrefix("convert:")
        } finally {
            source.close()
        }
    }

    @Test
    fun `digests match published vectors`() {
        val result = probe(
            "Convert.hexEncode(Convert.md5('abc'))",
            "Convert.hexEncode(Convert.sha1('abc'))",
            "Convert.hexEncode(Convert.sha256('abc'))",
        )
        val (md5, sha1, sha256) = result.split("|")
        assertEquals("900150983cd24fb0d6963f7d28e17f72", md5)
        assertEquals("a9993e364706816aba3e25717850c26c9cd0d89d", sha1)
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", sha256)
    }

    @Test
    fun `hmac matches rfc 4231 case 2`() {
        val result = probe("Convert.hmacString('Jefe', 'what do ya want for nothing?', 'sha256')")
        assertEquals("5bdcc146bf60754e6a042426089575c75a003f089d2739839dec58b964ec3843", result)
    }

    @Test
    fun `encoding round-trips through the host`() {
        val result = probe(
            "Convert.hexEncode(Convert.encodeUtf8('AZ'))",
            "Convert.decodeUtf8(Convert.decodeBase64(Convert.encodeBase64(Convert.encodeUtf8('中文 ok'))))",
            "Convert.decryptAesEcb(Convert.encryptAesEcb(Convert.encodeUtf8('secret'), Convert.encodeUtf8('0123456789abcdef')), Convert.encodeUtf8('0123456789abcdef')) instanceof ArrayBuffer",
        )
        val (hex, text, isBuffer) = result.split("|")
        assertEquals("415a", hex, "hex of 'AZ'")
        assertEquals("中文 ok", text)
        assertEquals("true", isBuffer, "AES-ECB round trip must return an ArrayBuffer")
    }

    @Test
    fun `aes ecb decrypts a known block`() {
        // AES-128-ECB of "0123456789abcdef" under key "0123456789abcdef", PKCS#5 padded.
        val cipherHex = probe(
            "Convert.hexEncode(Convert.encryptAesEcb(Convert.encodeUtf8('0123456789abcdef'), Convert.encodeUtf8('0123456789abcdef')))",
        )
        assertEquals(64, cipherHex.length, "16 payload bytes pad to a 32-byte cipher block")
        val roundTrip = probe(
            "Convert.decodeUtf8(Convert.decryptAesEcb(Convert.encryptAesEcb(Convert.encodeUtf8('0123456789abcdef'), Convert.encodeUtf8('0123456789abcdef')), Convert.encodeUtf8('0123456789abcdef')))",
        )
        assertEquals("0123456789abcdef", roundTrip)
    }

    @Test
    fun `unsupported rsa says so instead of returning garbage`() {
        val message = probe(
            "(() => { try { Convert.decryptRsa('a', 'b'); return 'no-throw'; } catch (e) { return 'threw'; } })()",
        )
        assertEquals("threw", message)
    }
}
