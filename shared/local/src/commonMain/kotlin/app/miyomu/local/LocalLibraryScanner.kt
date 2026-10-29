package app.miyomu.local

import app.miyomu.model.MediaType

/**
 * Discovers readable books under a library root.
 *
 * A directory is one comic book; each child directory or archive inside it is a chapter.
 * A directory holding video files is a multi-episode video book instead, and a directory
 * whose children are folders of video files is a video series with seasons. A standalone
 * archive is a comic book with one chapter, and a standalone video file is a single-entry
 * video book. Unsupported formats are skipped until their loaders land.
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
        if (ref.isDirectory) {
            val video = isVideoDirectory(ref)
            return LocalBook(
                id = ref.location,
                title = ref.name,
                format = if (video) LocalBookFormat.VIDEO else LocalBookFormat.DIRECTORY,
                mediaType = if (video) MediaType.VIDEO else MediaType.COMIC,
                location = ref.location,
                lastModified = ref.lastModified,
            )
        }

        val extension = extensionOf(ref.name)

        if (extension in videoExtensions) {
            return LocalBook(
                id = ref.location,
                title = ref.name.substringBeforeLast('.'),
                format = LocalBookFormat.VIDEO,
                mediaType = MediaType.VIDEO,
                location = ref.location,
                lastModified = ref.lastModified,
            )
        }

        val format = when (extension) {
            "zip", "cbz" -> LocalBookFormat.ZIP
            "7z", "cb7" -> LocalBookFormat.SEVEN_ZIP
            "pdf" -> LocalBookFormat.PDF
            "epub" -> LocalBookFormat.EPUB
            "txt" -> LocalBookFormat.TXT
            else -> null
        } ?: return null

        if (!format.isComicFormat()) return null

        return LocalBook(
            id = ref.location,
            title = ref.name.substringBeforeLast('.'),
            format = format,
            mediaType = MediaType.COMIC,
            location = ref.location,
            lastModified = ref.lastModified,
        )
    }

    /**
     * Comics win over videos when both layouts are present: chapter folders with images or
     * comic archives mark the directory as a comic. Cover images next to video files do not.
     */
    private fun isVideoDirectory(ref: LocalFileRef): Boolean {
        val children = ref.list().filterNot { it.name.startsWith('.') }
        if (children.any { !it.isDirectory && isVideoName(it.name) }) return true

        val childDirs = children.filter { it.isDirectory }
        val hasVideoSeasons = childDirs.any { dir ->
            dir.list().any { !it.isDirectory && isVideoName(it.name) }
        }
        if (!hasVideoSeasons) return false

        val hasComicChapters = childDirs.any { dir ->
            dir.list().any { !it.isDirectory && isImageName(it.name) }
        } || children.any { !it.isDirectory && extensionOf(it.name) in comicArchiveExtensions }
        return !hasComicChapters
    }
}
