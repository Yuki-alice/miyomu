package app.miyomu.ui.reader

import androidx.compose.runtime.Composable

/** Desktop windows cannot be rotated, so the orientation lock is a no-op here. */
@Composable
actual fun ApplyReaderOrientation(flag: Int) = Unit
