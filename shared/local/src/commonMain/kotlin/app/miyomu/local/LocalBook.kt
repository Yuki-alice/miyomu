package app.miyomu.local

import app.miyomu.model.MediaType

enum class LocalBookFormat {
    DIRECTORY,
    ZIP,
    SEVEN_ZIP,
    PDF,
    EPUB,
    TXT,

    // Novel-only formats (docs/10); the comic scanner keeps ignoring them.
    FB2,
    HTML,
    MARKDOWN,
}

/**
 * A local book discovered under the library root.
 *
 * [id] and [location] are the same stable identifier in the first slice; keeping both
 * lets a future Android SAF implementation persist a document URI while still exposing a
 * stable library id.
 */
data class LocalBook(
    val id: String,
    val title: String,
    val format: LocalBookFormat,
    val mediaType: MediaType,
    val location: String,
    val lastModified: Long = 0L,
)

data class LocalChapter(
    val id: String,
    val title: String,
    val bookLocation: String,
    val location: String,
    val format: LocalBookFormat,
    val lastModified: Long = 0L,
)

data class LocalPage(
    val index: Int,
    val name: String,
    /** File path, or `archivePath!/entryName` for pages inside an archive. */
    val location: String,
)
