package app.miyomu.local

internal fun naturalCompare(left: String, right: String): Int {
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

private val imageExtensions = setOf("jpg", "jpeg", "png", "webp", "gif", "bmp", "avif")

internal fun isImageName(name: String): Boolean =
    name.substringAfterLast('.', "").lowercase() in imageExtensions

internal fun extensionOf(name: String): String =
    name.substringAfterLast('.', "").lowercase()

internal fun LocalBookFormat.isComicFormat(): Boolean = when (this) {
    LocalBookFormat.DIRECTORY,
    LocalBookFormat.ZIP,
    LocalBookFormat.SEVEN_ZIP,
    LocalBookFormat.PDF,
    -> true
    LocalBookFormat.EPUB,
    LocalBookFormat.TXT,
    LocalBookFormat.VIDEO,
    LocalBookFormat.FB2,
    LocalBookFormat.HTML,
    LocalBookFormat.MARKDOWN,
    LocalBookFormat.MOBI,
    LocalBookFormat.AZW3,
    -> false
}

internal val videoExtensions = setOf("mp4", "m4v", "mkv", "webm", "avi", "mov", "ts", "m2ts")

internal val comicArchiveExtensions = setOf("zip", "cbz", "7z", "cb7")

internal fun isVideoName(name: String): Boolean = extensionOf(name) in videoExtensions
