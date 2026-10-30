@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package app.miyomu.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.miyomu.ui.MiyomuGraph
import app.miyomu.ui.core.ReadingStatsRecorder
import app.miyomu.ui.navigation.LocalMiyomuNavigator
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * P1-8 阅读统计与回顾: daily reading time over the retention window plus the aggregate
 * summary (last 7/30 days, daily average, current streak). Sessions are recorded by both
 * readers on exit; everything here is derived from the one JSON preference.
 */
@Composable
fun ReadingStatsRoute(graph: MiyomuGraph) {
    val navigator = LocalMiyomuNavigator.current
    val recorder = graph.readingStats
    val today = remember { LocalDate.now() }
    val week = remember { recorder.dailySeconds(today, days = 7) }
    val month = remember { recorder.dailySeconds(today, days = 30) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("阅读统计") },
                navigationIcon = {
                    IconButton(onClick = { navigator.pop() }) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
            item {
                val total = week.sumOf { it.second }
                SummaryRow("近 7 天", formatDuration(total), "日均 ${formatDuration(total / 7)}")
            }
            item {
                val total = month.sumOf { it.second }
                SummaryRow("近 30 天", formatDuration(total), "日均 ${formatDuration(total / 30)}")
            }
            item {
                SummaryRow(
                    "连续阅读",
                    "${recorder.currentStreak(today)} 天",
                    "坚持每天翻开书页",
                )
            }
            item {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            }
            if (month.isEmpty()) {
                item {
                    Text(
                        text = "还没有阅读记录；打开一本书开始阅读后，这里会按天记录时长。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
            } else {
                items(month.size) { index ->
                    val (date, seconds) = month[month.lastIndex - index]
                    Text(
                        text = "${date.format(DateTimeFormatter.ofPattern("MM月dd日"))} · " +
                            formatDuration(seconds),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryRow(title: String, value: String, subtitle: String) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(text = title, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.headlineSmall)
        Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Seconds shown as 时/分, mirroring the recorder's retention granularity. */
private fun formatDuration(seconds: Long): String {
    val minutes = seconds / 60
    return when {
        minutes >= 60 -> "${minutes / 60} 小时 ${minutes % 60} 分钟"
        minutes > 0 -> "$minutes 分钟"
        else -> "不足 1 分钟"
    }
}
