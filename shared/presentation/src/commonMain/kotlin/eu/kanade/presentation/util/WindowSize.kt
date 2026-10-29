package eu.kanade.presentation.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp

/** Width threshold matching Android's `sw600dp` tablet qualifier. */
private val tabletWidthThreshold = 600.dp

/**
 * Aniyomi reads `Configuration.smallestScreenWidthDp` here. Compose Multiplatform has no
 * `LocalConfiguration` on desktop, so the container size from `LocalWindowInfo` is used instead.
 */
@Composable
@ReadOnlyComposable
fun isTabletUi(): Boolean {
    val containerSize = LocalWindowInfo.current.containerSize
    val density = LocalDensity.current
    return with(density) { containerSize.width.toDp() >= tabletWidthThreshold }
}
