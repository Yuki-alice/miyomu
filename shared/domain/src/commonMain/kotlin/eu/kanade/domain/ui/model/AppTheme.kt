package eu.kanade.domain.ui.model

/** Aniyomi's AppTheme; MONET falls back to the default palette until platform dynamic color lands. */
enum class AppTheme {
    DEFAULT,
    MONET,
    CLOUDFLARE,
    COTTONCANDY,
    DOOM,
    GREEN_APPLE,
    LAVENDER,
    MATRIX,
    MIDNIGHT_DUSK,
    MOCHA,
    SAPPHIRE,
    NORD,
    STRAWBERRY_DAIQUIRI,
    TAKO,
    TEALTURQUOISE,
    TIDAL_WAVE,
    YINYANG,
    YOTSUBA,
    MONOCHROME,
    ;

    companion object {
        fun fromName(name: String?): AppTheme? = entries.firstOrNull { it.name == name }

        fun fromStored(stored: String, mode: ThemeMode): AppTheme {
            val fallback = if (mode == ThemeMode.LIGHT) MONET else DEFAULT
            return fromName(stored) ?: fallback
        }
    }
}

