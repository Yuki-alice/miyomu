package eu.kanade.tachiyomi.ui.reader.setting

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ScreenLockLandscape
import androidx.compose.material.icons.filled.ScreenLockPortrait
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.StayCurrentLandscape
import androidx.compose.material.icons.filled.StayCurrentPortrait
import androidx.compose.ui.graphics.vector.ImageVector
import dev.icerock.moko.resources.StringResource
import tachiyomi.i18n.MR

/**
 * CMP port: ActivityInfo screen-orientation constants inlined as literals; orientation
 * locking itself is not applied on desktop.
 */
enum class ReaderOrientation(
    val flag: Int,
    val stringRes: StringResource,
    val icon: ImageVector,
    val flagValue: Int,
) {
    DEFAULT(
        -1, // ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        MR.strings.label_default,
        Icons.Default.ScreenRotation,
        0x00000000,
    ),
    FREE(
        -1,
        MR.strings.rotation_free,
        Icons.Default.ScreenRotation,
        0x00000008,
    ),
    PORTRAIT(
        7, // ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
        MR.strings.rotation_portrait,
        Icons.Default.StayCurrentPortrait,
        0x00000010,
    ),
    LANDSCAPE(
        8, // ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        MR.strings.rotation_landscape,
        Icons.Default.StayCurrentLandscape,
        0x00000018,
    ),
    LOCKED_PORTRAIT(
        1, // ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        MR.strings.rotation_force_portrait,
        Icons.Default.ScreenLockPortrait,
        0x00000020,
    ),
    LOCKED_LANDSCAPE(
        0, // ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        MR.strings.rotation_force_landscape,
        Icons.Default.ScreenLockLandscape,
        0x00000028,
    ),
    REVERSE_PORTRAIT(
        9, // ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT
        MR.strings.rotation_reverse_portrait,
        Icons.Default.StayCurrentPortrait,
        0x00000030,
    ),
    ;

    companion object {
        const val MASK = 0x00000038

        fun fromPreference(preference: Int?): ReaderOrientation = entries.find { it.flagValue == preference } ?: DEFAULT
    }
}
