package app.miyomu.source.runtime

import java.security.MessageDigest
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

internal actual fun createConvertBridge(): ConvertBridge = JvmConvertBridge()

/**
 * `Convert` on the JVM: java.security for digests, javax.crypto for HMAC and AES.
 *
 * Deliberately narrow — only the primitives Venera's global exposes. Unknown algorithm or
 * mode names throw instead of silently degrading, so a source sees a real error rather than
 * a wrong signature.
 */
internal class JvmConvertBridge : ConvertBridge {

    override fun utf8Encode(text: String): ByteArray = text.encodeToByteArray()

    override fun utf8Decode(bytes: ByteArray): String = bytes.decodeToString()

    override fun gbkEncode(text: String): ByteArray = text.toByteArray(charset("GBK"))

    override fun gbkDecode(bytes: ByteArray): String = bytes.toString(charset("GBK"))

    override fun base64Encode(bytes: ByteArray): String = Base64.getEncoder().encodeToString(bytes)

    override fun base64Decode(text: String): ByteArray =
        Base64.getMimeDecoder().decode(text.filterNot { it.isWhitespace() })

    override fun hexEncode(bytes: ByteArray): String =
        bytes.joinToString("") { byte -> "%02x".format(byte.toInt() and 0xFF) }

    override fun digest(algorithm: String, bytes: ByteArray): ByteArray =
        MessageDigest.getInstance(messageDigestName(algorithm)).digest(bytes)

    override fun hmac(algorithm: String, key: ByteArray, bytes: ByteArray): ByteArray {
        val mac = Mac.getInstance(hmacName(algorithm))
        mac.init(SecretKeySpec(key, mac.algorithm))
        return mac.doFinal(bytes)
    }

    override fun aes(
        mode: String,
        encrypt: Boolean,
        data: ByteArray,
        key: ByteArray,
        iv: ByteArray?,
    ): ByteArray {
        val transformation = when (mode.lowercase()) {
            "ecb" -> "AES/ECB/PKCS5Padding"
            "cbc" -> "AES/CBC/PKCS5Padding"
            "cfb" -> "AES/CFB/NoPadding"
            "ofb" -> "AES/OFB/NoPadding"
            else -> throw IllegalArgumentException("unsupported AES mode: $mode")
        }
        val cipher = Cipher.getInstance(transformation)
        val keySpec = SecretKeySpec(key, "AES")
        val needsIv = transformation != "AES/ECB/PKCS5Padding"
        if (needsIv) {
            val spec = IvParameterSpec(iv ?: throw IllegalArgumentException("AES/$mode needs an iv"))
            cipher.init(if (encrypt) Cipher.ENCRYPT_MODE else Cipher.DECRYPT_MODE, keySpec, spec)
        } else {
            cipher.init(if (encrypt) Cipher.ENCRYPT_MODE else Cipher.DECRYPT_MODE, keySpec)
        }
        return cipher.doFinal(data)
    }

    private fun messageDigestName(algorithm: String): String = when (algorithm.lowercase()) {
        "md5" -> "MD5"
        "sha1" -> "SHA-1"
        "sha256" -> "SHA-256"
        "sha512" -> "SHA-512"
        else -> throw IllegalArgumentException("unsupported digest: $algorithm")
    }

    private fun hmacName(algorithm: String): String = when (algorithm.lowercase()) {
        "md5" -> "HmacMD5"
        "sha1" -> "HmacSHA1"
        "sha256" -> "HmacSHA256"
        "sha512" -> "HmacSHA512"
        else -> throw IllegalArgumentException("unsupported hmac hash: $algorithm")
    }
}
