package app.miyomu.local

import app.miyomu.model.MediaType

/**
 * Discovers readable comic books under a library root.
 *
 * A directory is one book; each child directory or archive inside it is a chapter.
 * A standalone archive is a book with one chapter. Unsupported formats are skipped
 * until their loaders land.
 */
class LocalLibraryScanner {

    fun scan(root: LocalFileRef): List<LocalBook> {
        val entries = if (root.isDirectory) root.list() else listOf(root)
        return entries
            .asSequence()
            .filterNot { it.name.startsWith('.') }
            .mapNotNull(::bookOf)
            .sortedWith { left, right -> naturalCompare(left.title, right.title) }
            .toList()
    }

    private fun bookOf(ref: LocalFileRef): LocalBook? {
        val format = when {
            ref.isDirectory -> LocalBookFormat.DIRECTORY
            else -> when (extensionOf(ref.name)) {
                "zip", "cbz" -> LocalBookFormat.ZIP
                "7z", "cb7" -> LocalBookFormat.SEVEN_ZIP
                "pdf" -> LocalBookFormat.PDF
                "epub" -> LocalBookFormat.EPUB
                "txt" -> LocalBookFormat.TXT
                else -> null
            }
        } ?: return null

        if (!format.isComicFormat()) return null

        val title = if (ref.isDirectory) ref.name else ref.name.substringBeforeLast('.')
        return LocalBook(
            id = ref.location,
            title = title,
            format = format,
            mediaType = MediaType.COMIC,
            location = ref.location,
            lastModified = ref.lastModified,
        )
    }
}
