package app.miyomu.source.runtime

/**
 * Host-side codecs behind Venera's `Convert` global.
 *
 * Sources lean on these for request signing (md5/sha256/hmac), payload decoding (base64,
 * gbk) and encrypted API responses (AES). Doing them on the host keeps binary data off the
 * JS/QuickJS boundary, and both shipping targets are JVM, so one implementation serves
 * Android and desktop.
 */
internal interface ConvertBridge {

    fun utf8Encode(text: String): ByteArray

    fun utf8Decode(bytes: ByteArray): String

    fun gbkEncode(text: String): ByteArray

    fun gbkDecode(bytes: ByteArray): String

    fun base64Encode(bytes: ByteArray): String

    fun base64Decode(text: String): ByteArray

    fun hexEncode(bytes: ByteArray): String

    /** [algorithm] is one of `md5`, `sha1`, `sha256`, `sha512`. */
    fun digest(algorithm: String, bytes: ByteArray): ByteArray

    /** HMAC over [bytes] with [key]; [algorithm] as in [digest]. */
    fun hmac(algorithm: String, key: ByteArray, bytes: ByteArray): ByteArray

    /**
     * AES with PKCS#5 padding. [mode] is `ecb`, `cbc`, `cfb` or `ofb`; [iv] is required for
     * every mode except ECB.
     */
    fun aes(mode: String, encrypt: Boolean, data: ByteArray, key: ByteArray, iv: ByteArray?): ByteArray
}

internal expect fun createConvertBridge(): ConvertBridge

/** Coerces whatever quickjs-kt hands over for a binary argument into bytes. */
internal fun Any?.convertBytes(): ByteArray = when (this) {
    null -> ByteArray(0)
    is ByteArray -> this
    is UByteArray -> this.toByteArray()
    is String -> this.encodeToByteArray()
    is List<*> -> ByteArray(size) { index -> (this[index] as Number).toByte() }
    else -> throw IllegalArgumentException("unsupported binary argument: ${this::class.simpleName}")
}

internal fun Any?.convertText(): String = when (this) {
    null -> ""
    is String -> this
    is ByteArray -> this.decodeToString()
    is UByteArray -> this.toByteArray().decodeToString()
    else -> this.toString()
}
