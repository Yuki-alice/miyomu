package eu.kanade.domain.ui.model

/**
 * CMP port: the AppCompatDelegate application function is Android-only; theme mode
 * application is handled by the platform entry points.
 */
enum class ThemeMode {
    LIGHT,
    DARK,
    SYSTEM,
    ;

    companion object {
        /** Accepts the current enum-name encoding and the legacy 0/1/2 int encoding. */
        fun fromStored(stored: String): ThemeMode = when (stored) {
            "SYSTEM" -> SYSTEM
            "LIGHT" -> LIGHT
            "DARK" -> DARK
            "0" -> SYSTEM
            "1" -> LIGHT
            "2" -> DARK
            else -> SYSTEM
        }
    }
}
