/*
 * Dark color scheme adapted from Aniyomi's LavenderColorScheme
 * (https://github.com/aniyomiorg/aniyomi, Apache-2.0). See NOTICE.
 */
package app.miyomu.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color

val lavenderDarkColors = darkColorScheme(
        primary = Color(0xFFA177FF),
        onPrimary = Color(0xFF3D0090),
        primaryContainer = Color(0xFFA177FF),
        onPrimaryContainer = Color(0xFFFFFFFF),
        secondary = Color(0xFFA177FF), // Unread badge
        onSecondary = Color(0xFFFFFFFF), // Unread badge text
        secondaryContainer = Color(0xFF423271), // Navigation bar selector pill & progress indicator (remaining)
        onSecondaryContainer = Color(0xFFA177FF), // Navigation bar selected icon
        tertiary = Color(0xFFCDBDFF), // Downloaded badge
        onTertiary = Color(0xFF360096), // Downloaded badge text
        tertiaryContainer = Color(0xFF5512D8),
        onTertiaryContainer = Color(0xFFEFE6FF),
        error = Color(0xFFFFB4AB),
        onError = Color(0xFF690005),
        errorContainer = Color(0xFF93000A),
        onErrorContainer = Color(0xFFFFDAD6),
        background = Color(0xFF111129),
        onBackground = Color(0xFFE7E0EC),
        surface = Color(0xFF111129),
        onSurface = Color(0xFFE7E0EC),
        surfaceVariant = Color(0xFF3D2F6B), // Navigation bar background (ThemePrefWidget)
        onSurfaceVariant = Color(0xFFCBC3D6),
        outline = Color(0xFF958E9F),
        outlineVariant = Color(0xFF4A4453),
        scrim = Color(0xFF000000),
        inverseSurface = Color(0xFFE7E0EC),
        inverseOnSurface = Color(0xFF322F38),
        inversePrimary = Color(0xFF6D41C8),
        surfaceDim = Color(0xFF111129),
        surfaceBright = Color(0xFF3B3841),
        surfaceContainerLowest = Color(0xFF15132d),
        surfaceContainerLow = Color(0xFF171531),
        surfaceContainer = Color(0xFF1D193B), // Navigation bar background
        surfaceContainerHigh = Color(0xFF241f41),
        surfaceContainerHighest = Color(0xFF282446),
    )
