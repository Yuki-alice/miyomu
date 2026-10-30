package app.miyomu.ui.core

import java.time.LocalDate
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import tachiyomi.core.common.preference.PreferenceStore

/**
 * Daily reading-time tracking (P1-8), stored as one JSON preference (`epochDay -> seconds`)
 * so no Room migration is needed. Entries older than [RETENTION_DAYS] are pruned on write;
 * both readers record their session length through [recordSession].
 */
class ReadingStatsRecorder(private val preferenceStore: PreferenceStore) {

    /** Accumulates [seconds] into [day]'s bucket (second-granular, sub-second reads ignored). */
    fun recordSession(day: LocalDate, seconds: Long) {
        if (seconds <= 0) return
        val current = readAll().toMutableMap()
        val key = day.toEpochDay().toString()
        current[key] = (current[key] ?: 0L) + seconds
        val cutoff = day.toEpochDay() - RETENTION_DAYS
        val pruned = current.filterKeys { it.toLongOrNull() != null && it.toLong() >= cutoff }
        preferenceStore.getString(KEY, "{}").set(Json.encodeToString(buildJsonObject {
            pruned.forEach { (epochDay, value) -> put(epochDay, value) }
        }))
    }

    /** Seconds read per local date over the last [days] days, oldest first, zero days omitted. */
    fun dailySeconds(today: LocalDate, days: Int): List<Pair<LocalDate, Long>> {
        val all = readAll()
        return (days - 1 downTo 0).mapNotNull { back ->
            val date = today.minusDays(back.toLong())
            all[date.toEpochDay().toString()]?.takeIf { it > 0 }?.let { date to it }
        }
    }

    fun totalSeconds(today: LocalDate, days: Int): Long =
        dailySeconds(today, days).sumOf { it.second }

    /** Longest daily streak ending today or yesterday, in days. */
    fun currentStreak(today: LocalDate): Int {
        val present = readAll().keys.mapNotNull { it.toLongOrNull() }.toSet()
        var cursor = today
        if (cursor.toEpochDay() !in present) cursor = cursor.minusDays(1)
        var streak = 0
        while (cursor.toEpochDay() in present) {
            streak++
            cursor = cursor.minusDays(1)
        }
        return streak
    }

    private fun readAll(): Map<String, Long> {
        val raw = preferenceStore.getString(KEY, "{}").get()
        val parsed = runCatching { Json.parseToJsonElement(raw) }.getOrNull() as? JsonObject ?: return emptyMap()
        return parsed.entries.mapNotNull { (key, value) ->
            value.jsonPrimitive.longOrNull?.let { key to it }
        }.toMap()
    }

    companion object {
        const val KEY = "reading_stats_daily_seconds"

        /** Upper bound for one recorded session (a composition left open overnight etc.). */
        const val MAX_SESSION_SECONDS = 7200L
        const val RETENTION_DAYS = 90L
    }
}
