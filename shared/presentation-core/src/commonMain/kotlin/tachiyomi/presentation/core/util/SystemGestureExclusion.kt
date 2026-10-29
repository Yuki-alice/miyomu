package tachiyomi.presentation.core.util

import androidx.compose.ui.Modifier

/**
 * Excludes the layout from Android's system gesture areas (e.g. the back-gesture edge);
 * a no-op where the concept does not exist.
 */
expect fun Modifier.systemGestureExclusion(): Modifier
