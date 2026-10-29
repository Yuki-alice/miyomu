package app.miyomu.player

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.openani.mediamp.metadata.AudioTrack
import org.openani.mediamp.metadata.SubtitleTrack

private val ChromeBackground = Color.Black.copy(alpha = 0.6f)

@Composable
internal fun PlayerTopBar(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = ChromeBackground,
    ) {
        Row(
            modifier = Modifier.systemBarsPadding().padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "返回",
                    tint = Color.White,
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
internal fun PlayerBottomBar(
    positionMillis: Long,
    durationMillis: Long,
    playWhenReady: Boolean,
    buffering: Boolean,
    settingsEnabled: Boolean,
    onTogglePlay: () -> Unit,
    onOpenSettings: () -> Unit,
    onScrub: (Float) -> Unit,
    onScrubFinished: () -> Unit,
    onNextEpisode: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = ChromeBackground,
        shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
    ) {
        Column(modifier = Modifier.systemBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onTogglePlay) {
                    if (buffering) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(20.dp),
                        )
                    } else {
                        Icon(
                            imageVector = if (playWhenReady) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (playWhenReady) "暂停" else "播放",
                            tint = Color.White,
                        )
                    }
                }

                Text(
                    text = formatTime(positionMillis),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                )

                Slider(
                    value = positionMillis.coerceIn(0L, durationMillis.coerceAtLeast(1L)).toFloat(),
                    onValueChange = onScrub,
                    onValueChangeFinished = onScrubFinished,
                    valueRange = 0f..(durationMillis.coerceAtLeast(1L)).toFloat(),
                    modifier = Modifier.weight(1f),
                )

                Text(
                    text = formatTime(durationMillis),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                )

                if (onNextEpisode != null) {
                    TextButton(onClick = onNextEpisode) {
                        Text("下一集", color = Color.White)
                    }
                }

                IconButton(onClick = onOpenSettings, enabled = settingsEnabled) {
                    Icon(
                        imageVector = Icons.Outlined.Settings,
                        contentDescription = "播放设置",
                        tint = if (settingsEnabled) Color.White else Color.White.copy(alpha = 0.4f),
                    )
                }
            }
        }
    }
}

@Composable
internal fun PlayerSettingsSheet(
    danmakuEnabled: Boolean? = null,
    onDanmakuToggled: ((Boolean) -> Unit)? = null,
    speed: Float?,
    onSpeedSelected: (Float) -> Unit,
    audioCandidates: List<AudioTrack>,
    selectedAudio: AudioTrack?,
    onAudioSelected: (AudioTrack?) -> Unit,
    subtitleCandidates: List<SubtitleTrack>,
    selectedSubtitle: SubtitleTrack?,
    onSubtitleSelected: (SubtitleTrack?) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        PlayerSettingsContent(
            danmakuEnabled = danmakuEnabled,
            onDanmakuToggled = onDanmakuToggled,
            speed = speed,
            onSpeedSelected = onSpeedSelected,
            audioCandidates = audioCandidates,
            selectedAudio = selectedAudio,
            onAudioSelected = onAudioSelected,
            subtitleCandidates = subtitleCandidates,
            selectedSubtitle = selectedSubtitle,
            onSubtitleSelected = onSubtitleSelected,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun PlayerSettingsContent(
    danmakuEnabled: Boolean? = null,
    onDanmakuToggled: ((Boolean) -> Unit)? = null,
    speed: Float?,
    onSpeedSelected: (Float) -> Unit,
    audioCandidates: List<AudioTrack>,
    selectedAudio: AudioTrack?,
    onAudioSelected: (AudioTrack?) -> Unit,
    subtitleCandidates: List<SubtitleTrack>,
    selectedSubtitle: SubtitleTrack?,
    onSubtitleSelected: (SubtitleTrack?) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp),
    ) {
        Text(
            text = "播放设置",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
        )

        if (danmakuEnabled != null && onDanmakuToggled != null) {
            SectionTitle("弹幕")
            TrackOptionRow(
                label = if (danmakuEnabled) "已开启（点击关闭）" else "已关闭（点击开启）",
                selected = danmakuEnabled,
                onClick = { onDanmakuToggled(!danmakuEnabled) },
            )
        }

        if (speed != null) {
            SectionTitle("播放速度")
            FlowRow(
                modifier = Modifier.padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                playerSpeedOptions.forEach { option ->
                    FilterChip(
                        selected = speed == option,
                        onClick = { onSpeedSelected(option) },
                        label = { Text(formatSpeed(option)) },
                    )
                }
            }
        }

        if (audioCandidates.size > 1) {
            SectionTitle("音轨")
            TrackOptionRow(
                label = "自动",
                selected = selectedAudio == null,
                onClick = { onAudioSelected(null) },
            )
            audioCandidates.forEach { track ->
                TrackOptionRow(
                    label = track.displayName(),
                    selected = track == selectedAudio,
                    onClick = { onAudioSelected(track) },
                )
            }
        }

        if (subtitleCandidates.isNotEmpty()) {
            SectionTitle("字幕")
            TrackOptionRow(
                label = "关闭",
                selected = selectedSubtitle == null,
                onClick = { onSubtitleSelected(null) },
            )
            subtitleCandidates.forEach { track ->
                TrackOptionRow(
                    label = track.displayName(),
                    selected = track == selectedSubtitle,
                    onClick = { onSubtitleSelected(track) },
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
    )
}

@Composable
private fun TrackOptionRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}
