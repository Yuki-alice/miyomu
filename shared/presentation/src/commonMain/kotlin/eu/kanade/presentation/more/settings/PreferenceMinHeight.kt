package eu.kanade.presentation.more.settings

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.structuralEqualityPolicy
import androidx.compose.ui.unit.dp

// CMP port: extracted from PreferenceItem.kt (settings framework lands with the settings
// screens port); merge back when PreferenceItem.kt is ported.
val LocalPreferenceMinHeight = compositionLocalOf(structuralEqualityPolicy()) { 56.dp }

/**
 * CMP port: Aniyomi defines this in PreferenceItem.kt, which has not been ported yet. The
 * highlight flag only drives the scroll-to-setting blink, so a constant `false` is enough until
 * the settings framework lands.
 */
val LocalPreferenceHighlighted = compositionLocalOf(structuralEqualityPolicy()) { false }
