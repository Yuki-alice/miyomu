package app.miyomu.core.design

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape

private val Indigo = Color(0xFF4B4BD6)
private val IndigoLight = Color(0xFFC0C1FF)
private val Coral = Color(0xFFB23B54)
private val CoralLight = Color(0xFFFFB1C1)
private val Mint = Color(0xFF006B4F)
private val MintLight = Color(0xFF5BD6A8)

private val LightColors: ColorScheme = lightColorScheme(
    primary = Indigo,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE1E0FF),
    onPrimaryContainer = Color(0xFF0F0B52),
    secondary = Coral,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFD9DF),
    onSecondaryContainer = Color(0xFF3F0016),
    tertiary = Mint,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFF8EF8C9),
    onTertiaryContainer = Color(0xFF002117),
    background = Color(0xFFFBF8FF),
    onBackground = Color(0xFF1B1B21),
    surface = Color(0xFFFBF8FF),
    onSurface = Color(0xFF1B1B21),
    surfaceVariant = Color(0xFFE3DFEA),
    onSurfaceVariant = Color(0xFF46464F),
    outline = Color(0xFF777680),
    outlineVariant = Color(0xFFC7C5D0),
    error = Color(0xFFBA1A1A),
)

private val DarkColors: ColorScheme = darkColorScheme(
    primary = IndigoLight,
    onPrimary = Color(0xFF1B1B5E),
    primaryContainer = Color(0xFF3333A8),
    onPrimaryContainer = Color(0xFFE1E0FF),
    secondary = CoralLight,
    onSecondary = Color(0xFF66002A),
    secondaryContainer = Color(0xFF8E1635),
    onSecondaryContainer = Color(0xFFFFD9DF),
    tertiary = MintLight,
    onTertiary = Color(0xFF003825),
    tertiaryContainer = Color(0xFF005138),
    onTertiaryContainer = Color(0xFF8EF8C9),
    background = Color(0xFF131318),
    onBackground = Color(0xFFE5E1E9),
    surface = Color(0xFF131318),
    onSurface = Color(0xFFE5E1E9),
    surfaceVariant = Color(0xFF46464F),
    onSurfaceVariant = Color(0xFFC7C5D0),
    outline = Color(0xFF918F9A),
    outlineVariant = Color(0xFF46464F),
    error = Color(0xFFFFB4AB),
)

private val MiyomuShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun MiyomuTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialExpressiveTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        motionScheme = MotionScheme.expressive(),
        shapes = MiyomuShapes,
        typography = Typography(),
        content = content,
    )
}
