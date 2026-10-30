/*
 * Dark color scheme adapted from Aniyomi's StrawberryColorScheme
 * (https://github.com/aniyomiorg/aniyomi, Apache-2.0). See NOTICE.
 */
package app.miyomu.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color

val strawberryDarkColors = darkColorScheme(
        primary = Color(0xFFFFB2B8),
        onPrimary = Color(0xFF67001D),
        primaryContainer = Color(0xFFD53855),
        onPrimaryContainer = Color(0xFFFFFFFF),
        secondary = Color(0xFFED4A65), // Unread badge
        onSecondary = Color(0xFF201A1A), // Unread badge text
        secondaryContainer = Color(0xFF91002A), // Navigation bar selector pill & progress indicator (remaining)
        onSecondaryContainer = Color(0xFFFFFFFF), // Navigation bar selector icon
        tertiary = Color(0xFFE8C08E), // Downloaded badge
        onTertiary = Color(0xFF201A1A), // Downloaded badge text
        tertiaryContainer = Color(0xFF775930),
        onTertiaryContainer = Color(0xFFFFF7F1),
        error = Color(0xFFFFB4AB),
        onError = Color(0xFF690005),
        errorContainer = Color(0xFF93000A),
        onErrorContainer = Color(0xFFFFDAD6),
        background = Color(0xFF201A1A),
        onBackground = Color(0xFFF7DCDD),
        surface = Color(0xFF201A1A),
        onSurface = Color(0xFFF7DCDD),
        surfaceVariant = Color(0xFF322727), // Navigation bar background (ThemePrefWidget)
        onSurfaceVariant = Color(0xFFE1BEC0),
        outline = Color(0xFFA9898B),
        outlineVariant = Color(0xFF594042),
        scrim = Color(0xFF000000),
        inverseSurface = Color(0xFFF7DCDD),
        inverseOnSurface = Color(0xFF3D2C2D),
        inversePrimary = Color(0xFFB61F40),
        surfaceDim = Color(0xFF1D1011),
        surfaceBright = Color(0xFF463536),
        surfaceContainerLowest = Color(0xFF2C2222),
        surfaceContainerLow = Color(0xFF302525),
        surfaceContainer = Color(0xFF322727), // Navigation bar background
        surfaceContainerHigh = Color(0xFF3C2F2F),
        surfaceContainerHighest = Color(0xFF463737),
    )
