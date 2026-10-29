package eu.kanade.presentation.more.settings

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.structuralEqualityPolicy
import androidx.compose.ui.unit.dp

// CMP port: extracted from PreferenceItem.kt (settings framework lands with the settings
// screens port); merge back when PreferenceItem.kt is ported.
val LocalPreferenceMinHeight = compositionLocalOf(structuralEqualityPolicy()) { 56.dp }
