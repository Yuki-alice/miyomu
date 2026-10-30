package app.miyomu.ui

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable

/**
 * Platform dynamic color (Material You). Returns null when the platform cannot provide it;
 * the theme falls back to the default palette.
 */
@Composable
expect fun monetColorScheme(dark: Boolean): ColorScheme?
