/*
 * Material 3 color tokens adapted from Aniyomi's color schemes
 * (https://github.com/aniyomiorg/aniyomi, Apache-2.0). See NOTICE.
 */
package app.miyomu.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import app.miyomu.ui.theme.darkColors
import app.miyomu.ui.theme.defaultColorsDark
import app.miyomu.ui.theme.defaultColorsLight
import app.miyomu.ui.theme.lightColors
import eu.kanade.domain.ui.model.AppTheme
import eu.kanade.domain.ui.model.ThemeMode

/** Aniyomi's `pref_theme_dark_amoled_key`: pure-black surfaces in dark mode. */
private fun androidx.compose.material3.ColorScheme.amoled(): androidx.compose.material3.ColorScheme = copy(
    background = Color.Black,
    onBackground = Color.White,
    surface = Color.Black,
    onSurface = Color.White,
    surfaceContainerLowest = Color.Black,
    surfaceContainerLow = Color.Black,
    surfaceContainer = Color.Black,
    surfaceContainerHigh = Color.Black,
    surfaceContainerHighest = Color.Black,
)

@Composable
fun resolveDarkTheme(themeMode: ThemeMode): Boolean = when (themeMode) {
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
}

@Composable
fun MiyomuTheme(
    themeMode: ThemeMode,
    appTheme: AppTheme,
    amoled: Boolean = false,
    content: @Composable () -> Unit,
) {
    val dark = resolveDarkTheme(themeMode)
    var colors = if (appTheme == AppTheme.MONET) {
        monetColorScheme(dark) ?: if (dark) defaultColorsDark else defaultColorsLight
    } else {
        if (dark) appTheme.darkColors() else appTheme.lightColors()
    }
    if (dark && amoled) colors = colors.amoled()
    MaterialTheme(
        colorScheme = colors,
        typography = Typography(),
        content = content,
    )
}

/**
 * Preview wrapper for @Preview composables: follows the system dark mode with the default
 * palette. Replaces the deleted `TachiyomiPreviewTheme`, which carried a second copy of every
 * color scheme.
 */
@Composable
fun MiyomuPreviewTheme(
    appTheme: AppTheme = AppTheme.DEFAULT,
    amoled: Boolean = false,
    content: @Composable () -> Unit,
) {
    MiyomuTheme(
        themeMode = ThemeMode.SYSTEM,
        appTheme = appTheme,
        amoled = amoled,
        content = content,
    )
}
