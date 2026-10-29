package app.miyomu.ui.novel

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.miyomu.reader.novel.NovelReadingMode
import app.miyomu.reader.novel.NovelReaderPreferences
import app.miyomu.reader.novel.NovelReaderTheme
import eu.kanade.core.preference.asState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

/**
 * Typography and presentation settings for the novel reader. Values are global
 * [NovelReaderPreferences]; changes apply live while the sheet is open.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NovelReaderSettingsSheet(
    preferences: NovelReaderPreferences,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        NovelReaderSettingsContent(
            preferences = preferences,
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
        )
    }
}

@Composable
internal fun NovelReaderSettingsContent(
    preferences: NovelReaderPreferences,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val readingMode by remember(preferences) { preferences.readingMode() }.asState(scope)
    val theme by remember(preferences) { preferences.theme() }.asState(scope)
    val fontSize by remember(preferences) { preferences.fontSize() }.asState(scope)
    val lineHeight by remember(preferences) { preferences.lineHeight() }.asState(scope)
    val pagePadding by remember(preferences) { preferences.pagePadding() }.asState(scope)
    val paragraphSpacing by remember(preferences) { preferences.paragraphSpacing() }.asState(scope)
    val warmFilter by remember(preferences) { preferences.warmFilter() }.asState(scope)
    val volumeKeyPaging by remember(preferences) { preferences.volumeKeyPaging() }.asState(scope)

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(
            text = stringResource(MR.strings.novel_reader_settings),
            style = MaterialTheme.typography.titleMedium,
        )

        SettingsLabel(stringResource(MR.strings.novel_reading_mode))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = readingMode == NovelReadingMode.PAGED,
                onClick = { scope.launchSet(preferences.readingMode(), NovelReadingMode.PAGED) },
                label = { Text(stringResource(MR.strings.novel_reading_mode_paged)) },
            )
            FilterChip(
                selected = readingMode == NovelReadingMode.SCROLL,
                onClick = { scope.launchSet(preferences.readingMode(), NovelReadingMode.SCROLL) },
                label = { Text(stringResource(MR.strings.novel_reading_mode_scroll)) },
            )
        }

        SettingsLabel(stringResource(MR.strings.novel_theme))
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            NovelReaderTheme.entries.forEach { candidate ->
                val swatch = candidate.palette()
                val selected = theme == candidate
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(swatch.background)
                            .border(
                                width = if (selected) 3.dp else 1.dp,
                                color = if (selected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.outline
                                },
                                shape = CircleShape,
                            )
                            .clickable { scope.launchSet(preferences.theme(), candidate) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("文", color = swatch.text, style = MaterialTheme.typography.titleSmall)
                    }
                    Text(
                        text = when (candidate) {
                            NovelReaderTheme.LIGHT -> stringResource(MR.strings.novel_theme_light)
                            NovelReaderTheme.SEPIA -> stringResource(MR.strings.novel_theme_sepia)
                            NovelReaderTheme.DARK -> stringResource(MR.strings.novel_theme_dark)
                            NovelReaderTheme.BLACK -> stringResource(MR.strings.novel_theme_black)
                        },
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
        }

        SettingsSlider(
            label = stringResource(MR.strings.novel_font_size),
            value = fontSize.toFloat(),
            valueText = "${fontSize}sp",
            range = NovelReaderPreferences.MIN_FONT_SIZE.toFloat()..NovelReaderPreferences.MAX_FONT_SIZE.toFloat(),
            steps = NovelReaderPreferences.MAX_FONT_SIZE - NovelReaderPreferences.MIN_FONT_SIZE - 1,
            onChange = { value ->
                scope.launchSet(preferences.fontSize(), NovelReaderPreferences.clampFontSize(value.toInt()))
            },
        )

        SettingsSlider(
            label = stringResource(MR.strings.novel_line_height),
            value = lineHeight,
            valueText = "%.1f".format(lineHeight),
            range = NovelReaderPreferences.MIN_LINE_HEIGHT..NovelReaderPreferences.MAX_LINE_HEIGHT,
            steps = 9,
            onChange = { value ->
                scope.launchSet(
                    preferences.lineHeight(),
                    NovelReaderPreferences.clampLineHeight((value * 10).toInt() / 10f),
                )
            },
        )

        SettingsSlider(
            label = stringResource(MR.strings.novel_page_padding),
            value = pagePadding.toFloat(),
            valueText = "${pagePadding}dp",
            range = NovelReaderPreferences.MIN_PAGE_PADDING.toFloat()..NovelReaderPreferences.MAX_PAGE_PADDING.toFloat(),
            steps = NovelReaderPreferences.MAX_PAGE_PADDING - 1,
            onChange = { value ->
                scope.launchSet(preferences.pagePadding(), NovelReaderPreferences.clampPagePadding(value.toInt()))
            },
        )

        SettingsSlider(
            label = stringResource(MR.strings.novel_paragraph_spacing),
            value = paragraphSpacing.toFloat(),
            valueText = "${paragraphSpacing}dp",
            range = NovelReaderPreferences.MIN_PARAGRAPH_SPACING.toFloat()..NovelReaderPreferences.MAX_PARAGRAPH_SPACING.toFloat(),
            steps = NovelReaderPreferences.MAX_PARAGRAPH_SPACING - 1,
            onChange = { value ->
                scope.launchSet(
                    preferences.paragraphSpacing(),
                    NovelReaderPreferences.clampParagraphSpacing(value.toInt()),
                )
            },
        )

        SettingsLabel(stringResource(MR.strings.novel_warm_filter))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = warmFilter == NovelReaderPreferences.FILTER_OFF,
                onClick = { scope.launchSet(preferences.warmFilter(), NovelReaderPreferences.FILTER_OFF) },
                label = { Text(stringResource(MR.strings.off)) },
            )
            FilterChip(
                selected = warmFilter == 1,
                onClick = { scope.launchSet(preferences.warmFilter(), 1) },
                label = { Text(stringResource(MR.strings.novel_filter_low)) },
            )
            FilterChip(
                selected = warmFilter == 2,
                onClick = { scope.launchSet(preferences.warmFilter(), 2) },
                label = { Text(stringResource(MR.strings.novel_filter_mid)) },
            )
            FilterChip(
                selected = warmFilter == 3,
                onClick = { scope.launchSet(preferences.warmFilter(), 3) },
                label = { Text(stringResource(MR.strings.novel_filter_high)) },
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SettingsLabel(stringResource(MR.strings.novel_volume_key_paging))
            Switch(
                checked = volumeKeyPaging,
                onCheckedChange = { checked ->
                    scope.launchSet(preferences.volumeKeyPaging(), checked)
                },
            )
        }
    }
}

@Composable
private fun SettingsLabel(text: String) {
    Text(text = text, style = MaterialTheme.typography.labelLarge)
}

@Composable
private fun SettingsSlider(
    label: String,
    value: Float,
    valueText: String,
    range: ClosedFloatingPointRange<Float>,
    steps: Int,
    onChange: (Float) -> Unit,
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SettingsLabel(label)
            Text(valueText, style = MaterialTheme.typography.labelMedium)
        }
        Slider(
            value = value.coerceIn(range.start, range.endInclusive),
            onValueChange = onChange,
            valueRange = range,
            steps = steps,
        )
    }
}

private fun <T> CoroutineScope.launchSet(preference: tachiyomi.core.common.preference.Preference<T>, value: T) {
    launch { preference.set(value) }
}
