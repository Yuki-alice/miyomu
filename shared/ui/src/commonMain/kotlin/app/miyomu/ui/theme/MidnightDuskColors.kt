/*
 * Dark color scheme adapted from Aniyomi's MidnightDuskColorScheme
 * (https://github.com/aniyomiorg/aniyomi, Apache-2.0). See NOTICE.
 */
package app.miyomu.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color

val midnightDuskDarkColors = darkColorScheme(
        primary = Color(0xFFF02475),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFBD1C5C),
        onPrimaryContainer = Color(0xFFFFFFFF),
        inversePrimary = Color(0xFFF02475),
        secondary = Color(0xFFF02475), // Unread badge
        onSecondary = Color(0xFF16151D), // Unread badge text
        secondaryContainer = Color(0xFF66183C), // Navigation bar selector pill & progress indicator (remaining)
        onSecondaryContainer = Color(0xFFF02475), // Navigation bar selector icon
        tertiary = Color(0xFF55971C), // Downloaded badge
        onTertiary = Color(0xFF16151D), // Downloaded badge text
        tertiaryContainer = Color(0xFF386412),
        onTertiaryContainer = Color(0xFFE5E1E5),
        background = Color(0xFF16151D),
        onBackground = Color(0xFFE5E1E5),
        surface = Color(0xFF16151D),
        onSurface = Color(0xFFE5E1E5),
        surfaceVariant = Color(0xFF281624), // Navigation bar background (ThemePrefWidget)
        onSurfaceVariant = Color(0xFFD6C1C4),
        surfaceTint = Color(0xFFF02475),
        inverseSurface = Color(0xFF333043),
        inverseOnSurface = Color(0xFFFFFFFF),
        outline = Color(0xFF9F8C8F),
        surfaceContainerLowest = Color(0xFF221320),
        surfaceContainerLow = Color(0xFF251522),
        surfaceContainer = Color(0xFF281624), // Navigation bar background
        surfaceContainerHigh = Color(0xFF2D1C2A),
        surfaceContainerHighest = Color(0xFF2F1F2C),
    )
