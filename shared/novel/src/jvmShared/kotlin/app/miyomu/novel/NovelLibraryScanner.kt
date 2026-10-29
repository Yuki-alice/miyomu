package app.miyomu.novel

import app.miyomu.local.LocalBook
import app.miyomu.local.LocalBookFormat
import app.miyomu.local.LocalFileRef
import app.miyomu.model.MediaType

/**
 * Discovers novel books (TXT/EPUB) under a library root. Mirrors the comic scanner's
 * discovery rules (dotfiles skipped, natural order) but only claims novel formats —
 * the comic scanner keeps ignoring them, so both can run over the same root.
 */
class NovelLibraryScanner {

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
        if (ref.isDirectory) return null
        val format = when (extensionOf(ref.name)) {
            "txt" -> LocalBookFormat.TXT
            "epub" -> LocalBookFormat.EPUB
            "fb2" -> LocalBookFormat.FB2
            "html", "htm" -> LocalBookFormat.HTML
            "md", "markdown" -> LocalBookFormat.MARKDOWN
            else -> return null
        }
        return LocalBook(
            id = ref.location,
            title = ref.name.substringBeforeLast('.'),
            format = format,
            mediaType = MediaType.NOVEL,
            location = ref.location,
            lastModified = ref.lastModified,
        )
    }
}

private fun extensionOf(name: String): String =
    name.substringAfterLast('.', "").lowercase()

// Copied from shared/local (internal there, not visible across modules).
private fun naturalCompare(left: String, right: String): Int {
    var i = 0
    var j = 0
    while (i < left.length && j < right.length) {
        val a = left[i]
        val b = right[j]
        if (a.isDigit() && b.isDigit()) {
            var endA = i
            while (endA < left.length && left[endA].isDigit()) endA++
            var endB = j
            while (endB < right.length && right[endB].isDigit()) endB++

            val numberA = left.substring(i, endA).trimStart('0').ifEmpty { "0" }
            val numberB = right.substring(j, endB).trimStart('0').ifEmpty { "0" }
            val byLength = numberA.length.compareTo(numberB.length)
            if (byLength != 0) return byLength
            val byDigits = numberA.compareTo(numberB)
            if (byDigits != 0) return byDigits

            i = endA
            j = endB
            continue
        }

        val foldedA = a.lowercaseChar()
        val foldedB = b.lowercaseChar()
        if (foldedA != foldedB) return foldedA.compareTo(foldedB)
        i++
        j++
    }
    return (left.length - i).compareTo(right.length - j)
}
