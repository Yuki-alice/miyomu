package app.miyomu.local

/**
 * A platform-neutral handle to a local file or directory.
 *
 * Android and desktop both provide a JVM implementation; the interface keeps the
 * scanner and UI free of platform file APIs.
 */
interface LocalFileRef {
    val name: String

    /** Stable location used as the library key; a path on desktop, a URI on Android. */
    val location: String
    val isDirectory: Boolean
    val size: Long
    val lastModified: Long

    fun list(): List<LocalFileRef>

    fun readBytes(): ByteArray

    /** Permanently removes this file or directory tree. Callers confirm in the UI first. */
    fun delete(): Boolean
}
