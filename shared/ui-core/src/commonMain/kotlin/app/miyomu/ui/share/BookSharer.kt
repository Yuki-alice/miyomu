package app.miyomu.ui.share

import androidx.compose.runtime.Composable
import app.miyomu.local.LocalBookFormat

/**
 * P1-6 系统集成（分享动作）: hands the detail screens a platform share callback for a book
 * file, or null when the platform has no share sheet (desktop). The callback receives the
 * book's on-disk location and MIME type; whether the file is actually shareable (e.g. inside
 * the provider-scoped directory) is the platform's call.
 */
@Composable
expect fun rememberBookSharer(): ((filePath: String, mimeType: String) -> Unit)?

fun bookMimeType(format: LocalBookFormat): String = when (format) {
    LocalBookFormat.ZIP -> "application/zip"
    LocalBookFormat.SEVEN_ZIP -> "application/x-7z-compressed"
    LocalBookFormat.PDF -> "application/pdf"
    LocalBookFormat.EPUB -> "application/epub+zip"
    LocalBookFormat.TXT, LocalBookFormat.MARKDOWN -> "text/plain"
    LocalBookFormat.HTML -> "text/html"
    LocalBookFormat.MOBI -> "application/x-mobipocket-ebook"
    else -> "application/octet-stream"
}
