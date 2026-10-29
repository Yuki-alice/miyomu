package eu.kanade.presentation.util

import androidx.compose.runtime.Composable
import eu.kanade.presentation.components.relativeDateText
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import java.time.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

/**
 * CMP port: Aniyomi's version takes a `Context` to read strings. moko resolves them from the
 * composition instead, so the receiver moves to a `@Composable` and the `Context` parameter is
 * dropped. Call sites pass only the fallback text.
 */
@Composable
fun Duration.toDurationString(fallback: String): String {
    return toComponents { days, hours, minutes, seconds, _ ->
        buildList(4) {
            if (days != 0L) add(stringResource(MR.strings.day_short, days))
            if (hours != 0) add(stringResource(MR.strings.hour_short, hours))
            if (minutes != 0 && (days == 0L || hours == 0)) {
                add(stringResource(MR.strings.minute_short, minutes))
            }
            if (seconds != 0 && days == 0L && hours == 0) {
                add(stringResource(MR.strings.seconds_short, seconds))
            }
        }.joinToString(" ").ifBlank { fallback }
    }
}

/**
 * CMP port: `android.text.format.DateUtils.getRelativeTimeSpanString` has no KMP equivalent.
 * Under a day the elapsed time is rendered with the short unit strings; beyond that it defers to
 * [relativeDateText], which already handles "today" / "N days ago" / absolute dates.
 */
@Composable
fun relativeTimeSpanString(epochMillis: Long): String {
    if (epochMillis <= 0L) {
        return stringResource(MR.strings.relative_time_span_never)
    }
    val now = Instant.now().toEpochMilli()
    val elapsed = now - epochMillis
    return when {
        elapsed < 1.minutes.inWholeMilliseconds -> stringResource(
            MR.strings.updates_last_update_info_just_now,
        )
        elapsed < 1.days.inWholeMilliseconds -> elapsed.milliseconds
            .toDurationString(fallback = relativeDateText(epochMillis))
        else -> relativeDateText(epochMillis)
    }
}
