package eu.kanade.presentation.theme.colorscheme

import androidx.compose.material3.ColorScheme

/**
 * CMP port: dynamic (wallpaper-derived) color is a platform-specific Android feature;
 * until an expect/actual dynamic scheme exists, Monet falls back to the Tachiyomi scheme.
 */
internal class MonetColorScheme : BaseColorScheme() {

    override val darkScheme: ColorScheme
        get() = TachiyomiColorScheme.darkScheme

    override val lightScheme: ColorScheme
        get() = TachiyomiColorScheme.lightScheme
}
