package app.miyomu.local

import java.io.File

/**
 * JVM/Android file-backed implementation. Android can use app-private paths now;
 * SAF document URIs will get a sibling implementation without changing the scanner.
 */
class FileLocalFileRef(
    private val file: File,
) : LocalFileRef {
    override val name: String = file.name.ifEmpty { file.absolutePath }
    override val location: String = file.absolutePath
    override val isDirectory: Boolean = file.isDirectory
    override val size: Long = if (file.isFile) file.length() else 0L
    override val lastModified: Long = file.lastModified()

    override fun list(): List<LocalFileRef> =
        file.listFiles()?.map(::FileLocalFileRef).orEmpty()

    override fun readBytes(): ByteArray = file.readBytes()
}
