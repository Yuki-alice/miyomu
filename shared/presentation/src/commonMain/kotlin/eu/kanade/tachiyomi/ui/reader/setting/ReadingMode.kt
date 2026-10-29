package eu.kanade.tachiyomi.ui.reader.setting

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material.icons.outlined.UnfoldMore
import androidx.compose.material.icons.outlined.ViewStream
import androidx.compose.ui.graphics.vector.ImageVector
import dev.icerock.moko.resources.StringResource
import tachiyomi.i18n.MR

/**
 * CMP port: Android drawable icons replaced with Compose ImageVectors, and toViewer()
 * removed — viewer construction stays in Miyomu's ComicReaderScreen.
 */
enum class ReadingMode(
    val stringRes: StringResource,
    val icon: ImageVector,
    val flagValue: Int,
    val direction: Direction? = null,
    val type: ViewerType? = null,
) {
    DEFAULT(MR.strings.label_default, Icons.AutoMirrored.Outlined.MenuBook, 0x00000000),
    LEFT_TO_RIGHT(
        MR.strings.left_to_right_viewer,
        Icons.AutoMirrored.Outlined.ArrowForward,
        0x00000001,
        Direction.Horizontal,
        ViewerType.Pager,
    ),
    RIGHT_TO_LEFT(
        MR.strings.right_to_left_viewer,
        Icons.AutoMirrored.Outlined.ArrowBack,
        0x00000002,
        Direction.Horizontal,
        ViewerType.Pager,
    ),
    VERTICAL(
        MR.strings.vertical_viewer,
        Icons.Outlined.SwapVert,
        0x00000003,
        Direction.Vertical,
        ViewerType.Pager,
    ),
    WEBTOON(
        MR.strings.webtoon_viewer,
        Icons.Outlined.ViewStream,
        0x00000004,
        Direction.Vertical,
        ViewerType.Webtoon,
    ),
    CONTINUOUS_VERTICAL(
        MR.strings.vertical_plus_viewer,
        Icons.Outlined.UnfoldMore,
        0x00000005,
        Direction.Vertical,
        ViewerType.Webtoon,
    ),
    ;

    companion object {
        const val MASK = 0x00000007

        fun fromPreference(preference: Int?): ReadingMode = entries.find { it.flagValue == preference } ?: DEFAULT

        fun isPagerType(preference: Int): Boolean {
            val mode = fromPreference(preference)
            return mode.type is ViewerType.Pager
        }
    }

    sealed interface Direction {
        data object Horizontal : Direction
        data object Vertical : Direction
    }

    sealed interface ViewerType {
        data object Pager : ViewerType
        data object Webtoon : ViewerType
    }
}
