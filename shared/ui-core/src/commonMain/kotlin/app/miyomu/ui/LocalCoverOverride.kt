package app.miyomu.ui

import app.miyomu.local.LocalBook
import app.miyomu.local.LocalBookFormat
import java.io.File
import tachiyomi.core.common.preference.PreferenceStore

/**
 * P1-1 封面候选: user-chosen cover for directory comics. The selection is stored as a
 * book-relative file path in the shared preference store; [localCoverOverrideBytes] resolves
 * it ahead of the default first-page cover.
 */
fun coverOverrideKey(bookId: String): String = "local_cover_override_${bookId.hashCode()}"

/** Cover bytes from the user's picked candidate, or null when unset/missing. */
fun localCoverOverrideBytes(
    preferenceStore: PreferenceStore,
    book: LocalBook,
): ByteArray? {
    val relative = preferenceStore.getString(coverOverrideKey(book.id), "").get()
    if (relative.isBlank()) return null
    val file = File(book.location).parentFile?.resolve(relative) ?: return null
    return file.takeIf { it.isFile }?.readBytes()
}

/** Saves [candidate] (a file inside the book directory) as the book's cover. */
fun saveCoverOverride(
    preferenceStore: PreferenceStore,
    book: LocalBook,
    candidate: File,
) {
    val bookDir = File(book.location).parentFile
    val relative = when {
        bookDir == null -> candidate.absolutePath
        else -> candidate.relativeToOrSelf(bookDir).path
    }
    preferenceStore.getString(coverOverrideKey(book.id), "").set(relative)
}

/**
 * Candidate cover images for a book: for directory comics, the top-level images in the book
 * directory (single-file books have no candidates — their cover is the first page).
 */
fun localCoverCandidates(book: LocalBook): List<File> {
    if (book.format != LocalBookFormat.DIRECTORY) return emptyList()
    val dir = File(book.location)
    if (!dir.isDirectory) return emptyList()
    return dir.listFiles().orEmpty()
        .filter { it.isFile && isCoverImageName(it.name) }
        .sortedBy { it.name.lowercase() }
}

private fun isCoverImageName(name: String): Boolean =
    name.substringAfterLast('.', "").lowercase() in setOf("jpg", "jpeg", "png", "webp", "gif", "bmp", "avif")
