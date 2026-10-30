package app.miyomu.ui.share

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import java.io.File

@Composable
actual fun rememberBookSharer(): ((filePath: String, mimeType: String) -> Unit)? {
    val context = LocalContext.current
    return remember(context) {
        { filePath: String, mimeType: String -> context.shareBookFile(filePath, mimeType) }
    }
}

/**
 * Shares the book file through ACTION_SEND. Only books under `filesDir/library` are
 * shareable — they sit inside the FileProvider scope; SAF-root books cannot be exposed
 * this way and are silently ignored.
 */
private fun Context.shareBookFile(filePath: String, mimeType: String) {
    val file = File(filePath)
    val libraryRoot = File(filesDir, "library").canonicalFile
    if (!file.canonicalFile.path.startsWith(libraryRoot.path)) return

    val uri = FileProvider.getUriForFile(this, "$packageName.files", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = mimeType
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    runCatching {
        startActivity(Intent.createChooser(intent, file.name))
    }
}
