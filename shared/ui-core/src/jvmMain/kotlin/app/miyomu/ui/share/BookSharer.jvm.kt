package app.miyomu.ui.share

import androidx.compose.runtime.Composable

/**
 * Desktop has no system share sheet; the share affordance stays hidden. Exporting a copy
 * through the folder-open hook is the desktop alternative.
 */
@Composable
actual fun rememberBookSharer(): ((filePath: String, mimeType: String) -> Unit)? = null
