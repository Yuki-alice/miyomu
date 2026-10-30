package app.miyomu.ui.reader

import androidx.compose.runtime.Composable

/**
 * Applies the reader's orientation lock, expressed with the `ActivityInfo` constants that
 * `ReaderOrientation.flag` already carries (-1 = unspecified).
 *
 * Desktop windows have no orientation, so the JVM actual is a no-op; Android sets
 * `Activity.requestedOrientation` for as long as the value is in composition.
 */
@Composable
expect fun ApplyReaderOrientation(flag: Int)
