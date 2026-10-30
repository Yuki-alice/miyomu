package app.miyomu.ui.theme

import androidx.compose.material3.ColorScheme
import eu.kanade.domain.ui.model.AppTheme

/** Maps an [AppTheme] to its generated palettes; MONET falls back to default until dynamic color lands. */
fun AppTheme.darkColors(): ColorScheme = when (this) {
    AppTheme.CLOUDFLARE -> cloudflareColorsDark
    AppTheme.COTTONCANDY -> cottoncandyColorsDark
    AppTheme.DOOM -> doomColorsDark
    AppTheme.GREEN_APPLE -> greenAppleColorsDark
    AppTheme.LAVENDER -> lavenderColorsDark
    AppTheme.MATRIX -> matrixColorsDark
    AppTheme.MIDNIGHT_DUSK -> midnightDuskColorsDark
    AppTheme.MOCHA -> mochaColorsDark
    AppTheme.MONOCHROME -> monochromeColorsDark
    AppTheme.NORD -> nordColorsDark
    AppTheme.SAPPHIRE -> sapphireColorsDark
    AppTheme.STRAWBERRY_DAIQUIRI -> strawberryDaiquiriColorsDark
    AppTheme.TAKO -> takoColorsDark
    AppTheme.TEALTURQUOISE -> tealturquoiseColorsDark
    AppTheme.TIDAL_WAVE -> tidalWaveColorsDark
    AppTheme.YINYANG -> yinyangColorsDark
    AppTheme.YOTSUBA -> yotsubaColorsDark
    AppTheme.DEFAULT -> defaultColorsDark
    AppTheme.MONET -> defaultColorsDark
}

fun AppTheme.lightColors(): ColorScheme = when (this) {
    AppTheme.CLOUDFLARE -> cloudflareColorsLight
    AppTheme.COTTONCANDY -> cottoncandyColorsLight
    AppTheme.DOOM -> doomColorsLight
    AppTheme.GREEN_APPLE -> greenAppleColorsLight
    AppTheme.LAVENDER -> lavenderColorsLight
    AppTheme.MATRIX -> matrixColorsLight
    AppTheme.MIDNIGHT_DUSK -> midnightDuskColorsLight
    AppTheme.MOCHA -> mochaColorsLight
    AppTheme.MONOCHROME -> monochromeColorsLight
    AppTheme.NORD -> nordColorsLight
    AppTheme.SAPPHIRE -> sapphireColorsLight
    AppTheme.STRAWBERRY_DAIQUIRI -> strawberryDaiquiriColorsLight
    AppTheme.TAKO -> takoColorsLight
    AppTheme.TEALTURQUOISE -> tealturquoiseColorsLight
    AppTheme.TIDAL_WAVE -> tidalWaveColorsLight
    AppTheme.YINYANG -> yinyangColorsLight
    AppTheme.YOTSUBA -> yotsubaColorsLight
    AppTheme.DEFAULT -> defaultColorsLight
    AppTheme.MONET -> defaultColorsLight
}
