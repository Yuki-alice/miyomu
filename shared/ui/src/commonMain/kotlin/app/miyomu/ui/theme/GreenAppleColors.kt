/*
 * Dark color scheme adapted from Aniyomi's GreenAppleColorScheme
 * (https://github.com/aniyomiorg/aniyomi, Apache-2.0). See NOTICE.
 */
package app.miyomu.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color

val greenAppleDarkColors = darkColorScheme(
        primary = Color(0xFF7ADB8F),
        onPrimary = Color(0xFF003917),
        primaryContainer = Color(0xFF017737),
        onPrimaryContainer = Color(0xFFFFFFFF),
        secondary = Color(0xFF7ADB8F), // Unread badge
        onSecondary = Color(0xFF003917), // Unread badge text
        secondaryContainer = Color(0xFF017737), // Navigation bar selector pill & progress indicator (remaining)
        onSecondaryContainer = Color(0xFFFFFFFF), // Navigation bar selected icon
        tertiary = Color(0xFFFFB3AC), // Downloaded badge
        onTertiary = Color(0xFF680008), // Downloaded badge text
        tertiaryContainer = Color(0xFFC7282A),
        onTertiaryContainer = Color(0xFFFFFFFF),
        error = Color(0xFFFFB4AB),
        onError = Color(0xFF690005),
        errorContainer = Color(0xFF93000A),
        onErrorContainer = Color(0xFFFFDAD6),
        background = Color(0xFF0F1510),
        onBackground = Color(0xFFDFE4DB),
        surface = Color(0xFF0F1510),
        onSurface = Color(0xFFDFE4DB),
        surfaceVariant = Color(0xFF3F493F), // Navigation bar background (ThemePrefWidget)
        onSurfaceVariant = Color(0xFFBECABC),
        outline = Color(0xFF889487),
        outlineVariant = Color(0xFF3F493F),
        scrim = Color(0xFF000000),
        inverseSurface = Color(0xFFDFE4DB),
        inverseOnSurface = Color(0xFF2C322C),
        inversePrimary = Color(0xFF006D32),
        surfaceDim = Color(0xFF0F1510),
        surfaceBright = Color(0xFF353B35),
        surfaceContainerLowest = Color(0xFF0A0F0B),
        surfaceContainerLow = Color(0xFF181D18),
        surfaceContainer = Color(0xFF1C211C), // Navigation bar background
        surfaceContainerHigh = Color(0xFF262B26),
        surfaceContainerHighest = Color(0xFF313630),
    )
