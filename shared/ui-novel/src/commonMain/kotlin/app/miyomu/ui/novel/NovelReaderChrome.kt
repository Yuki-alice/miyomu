package app.miyomu.ui.novel

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.FormatListBulleted
import androidx.compose.material.icons.outlined.NavigateBefore
import androidx.compose.material.icons.outlined.NavigateNext
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.VolumeOff
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import app.miyomu.reader.novel.NovelReadingMode
import app.miyomu.reader.novel.NovelReaderPreferences
import app.miyomu.reader.novel.NovelReaderTheme
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

/**
 * Novel reader chrome: translucent top bar (book / chapter), bottom bar (position slider,
 * chapter navigation, settings). Colors follow the reader theme rather than the app theme,
 * like dedicated reading apps do.
 */
@Composable
fun NovelReaderChrome(
    visible: Boolean,
    bookTitle: String,
    chapterTitle: String,
    positionLabel: String,
    positionFraction: Float,
    hasPreviousChapter: Boolean,
    hasNextChapter: Boolean,
    bookmarked: Boolean,
    palette: NovelPalette,
    onBack: () -> Unit,
    onPreviousChapter: () -> Unit,
    onNextChapter: () -> Unit,
    onPositionChange: (Float) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenCatalog: () -> Unit,
    onToggleBookmark: () -> Unit,
    /** P1-7 朗读: non-null enables the TTS affordance (platform-dependent). */
    readingAloud: Boolean = false,
    onToggleTts: (() -> Unit)? = null,
) {
    if (!visible) return
    val chromeSurface = palette.background.copy(alpha = 0.96f)

    Column(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(chromeSurface)
                .statusBarsPadding()
                .padding(horizontal = 4.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = palette.text,
                )
            }
            Column(Modifier.weight(1f)) {
                Text(
                    text = bookTitle,
                    style = MaterialTheme.typography.titleMedium,
                    color = palette.text,
                    maxLines = 1,
                )
                Text(
                    text = chapterTitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.text.copy(alpha = 0.7f),
                    maxLines = 1,
                )
            }
            IconButton(onClick = onToggleBookmark) {
                Icon(
                    if (bookmarked) Icons.Outlined.Bookmark else Icons.Outlined.BookmarkBorder,
                    contentDescription = stringResource(
                        if (bookmarked) MR.strings.novel_remove_bookmark else MR.strings.novel_add_bookmark,
                    ),
                    tint = palette.text,
                )
            }
            if (onToggleTts != null) {
                IconButton(onClick = onToggleTts) {
                    Icon(
                        if (readingAloud) Icons.Outlined.VolumeOff else Icons.Outlined.VolumeUp,
                        contentDescription = if (readingAloud) "停止朗读" else "朗读本章",
                        tint = palette.text,
                    )
                }
            }
        }

        Spacer(Modifier.weight(1f))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(chromeSurface)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onPreviousChapter, enabled = hasPreviousChapter) {
                Icon(
                    Icons.Outlined.NavigateBefore,
                    contentDescription = stringResource(MR.strings.novel_previous_chapter),
                    tint = palette.text,
                )
            }
            Text(
                text = positionLabel,
                style = MaterialTheme.typography.labelMedium,
                color = palette.text,
                modifier = Modifier
                    .testTag("novel-position")
                    .padding(horizontal = 4.dp),
            )
            Slider(
                value = positionFraction.coerceIn(0f, 1f),
                onValueChange = onPositionChange,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
                    .testTag("novel-slider"),
            )
            IconButton(onClick = onNextChapter, enabled = hasNextChapter) {
                Icon(
                    Icons.Outlined.NavigateNext,
                    contentDescription = stringResource(MR.strings.novel_next_chapter),
                    tint = palette.text,
                )
            }
            IconButton(onClick = onOpenCatalog) {
                Icon(
                    Icons.Outlined.FormatListBulleted,
                    contentDescription = stringResource(MR.strings.novel_catalog),
                    tint = palette.text,
                )
            }
            IconButton(onClick = onOpenSettings) {
                Icon(
                    Icons.Outlined.Tune,
                    contentDescription = stringResource(MR.strings.novel_reader_settings),
                    tint = palette.text,
                )
            }
        }
    }
}
