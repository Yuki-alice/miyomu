package eu.kanade.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import eu.kanade.domain.ui.UiPreferences
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.absoluteValue

/**
 * CMP port: Aniyomi's Context-based DateExtensions.toRelativeString is inlined here with
 * moko compose resources; UiPreferences is supplied through [MiyomuDatePreferences].
 */

/** Relative-date preferences resolved once per composition by the app wiring. */
data class MiyomuDatePreferences(
    val relativeTime: Boolean,
    val dateFormatPattern: String,
) {
    val dateFormat: DateTimeFormatter
        get() = UiPreferences.dateFormat(dateFormatPattern)
}

/** Overridable provider so tests / the app graph can supply preferences without Injekt. */
var datePreferencesProvider: () -> MiyomuDatePreferences = {
    MiyomuDatePreferences(relativeTime = true, dateFormatPattern = "")
}

@Composable
fun relativeDateText(
    dateEpochMillis: Long,
): String {
    return relativeDateText(
        localDate = LocalDate.ofInstant(
            Instant.ofEpochMilli(dateEpochMillis),
            ZoneId.systemDefault(),
        )
            .takeIf { dateEpochMillis > 0L },
    )
}

// For use in chapter/episode release time
@Composable
fun relativeDateTimeText(
    dateEpochMillis: Long,
): String {
    return relativeDateTimeText(
        localDateTime = LocalDateTime.ofInstant(
            Instant.ofEpochMilli(dateEpochMillis),
            ZoneId.systemDefault(),
        )
            .takeIf { dateEpochMillis > 0L },
    )
}

@Composable
fun relativeDateText(
    localDate: LocalDate?,
): String {
    val preferences = remember { datePreferencesProvider() }
    return localDate?.toRelativeString(
        relative = preferences.relativeTime,
        dateFormat = preferences.dateFormat,
    )
        ?: stringResource(MR.strings.not_applicable)
}

// For use in chapter/episode release time
@Composable
fun relativeDateTimeText(
    localDateTime: LocalDateTime?,
): String {
    val preferences = remember { datePreferencesProvider() }
    return localDateTime?.toDateRelativeString(
        relative = preferences.relativeTime,
        dateFormat = preferences.dateFormat,
    )
        ?: stringResource(MR.strings.not_applicable)
}

@Composable
private fun LocalDate.toRelativeString(
    relative: Boolean,
    dateFormat: DateTimeFormatter,
): String {
    if (!relative) {
        return dateFormat.format(this)
    }
    val now = LocalDate.now()
    val difference = ChronoUnit.DAYS.between(this, now)
    return when {
        difference < -7 -> dateFormat.format(this)
        difference < 0 -> pluralStringResource(
            MR.plurals.upcoming_relative_time,
            difference.toInt().absoluteValue,
            difference.toInt().absoluteValue,
        )
        difference < 1 -> stringResource(MR.strings.relative_time_today)
        difference < 7 -> pluralStringResource(
            MR.plurals.relative_time,
            difference.toInt(),
            difference.toInt(),
        )
        else -> dateFormat.format(this)
    }
}

@Composable
private fun LocalDateTime.toDateRelativeString(
    relative: Boolean,
    dateFormat: DateTimeFormatter,
): String {
    if (!relative) {
        val date = dateFormat.format(this)
        val time = DateTimeFormatter.ofLocalizedTime(java.time.format.FormatStyle.SHORT).format(this)
        return "$date $time"
    }
    return toLocalDate(ZoneId.systemDefault()).toRelativeString(relative, dateFormat)
}

private fun LocalDateTime.toLocalDate(zoneId: ZoneId): LocalDate =
    atZone(zoneId).toLocalDate()
