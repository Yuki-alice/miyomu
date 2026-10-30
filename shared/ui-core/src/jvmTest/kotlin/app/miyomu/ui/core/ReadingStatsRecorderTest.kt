package app.miyomu.ui.core

import java.time.LocalDate
import tachiyomi.core.common.preference.InMemoryPreferenceStore
import kotlin.test.Test
import kotlin.test.assertEquals

/** P1-8: daily bucketing, aggregation, streak and retention pruning. */
class ReadingStatsRecorderTest {

    private val today: LocalDate = LocalDate.of(2026, 10, 31)

    private fun recorder() = ReadingStatsRecorder(InMemoryPreferenceStore())

    @Test
    fun `accumulates sessions per day and aggregates`() {
        val recorder = recorder()
        recorder.recordSession(today, 600)
        recorder.recordSession(today, 300)
        recorder.recordSession(today.minusDays(1), 1200)

        assertEquals(900, recorder.totalSeconds(today, days = 1))
        assertEquals(2100, recorder.totalSeconds(today, days = 7))
        assertEquals(1200, recorder.totalSeconds(today.minusDays(1), days = 1))
        assertEquals(0, recorder.totalSeconds(today, days = 0))
    }

    @Test
    fun `zero and negative sessions are ignored`() {
        val recorder = recorder()
        recorder.recordSession(today, 0)
        recorder.recordSession(today, -5)

        assertEquals(0, recorder.totalSeconds(today, days = 1))
    }

    @Test
    fun `daily list is oldest first and skips empty days`() {
        val recorder = recorder()
        recorder.recordSession(today, 600)
        recorder.recordSession(today.minusDays(2), 300)

        val daily = recorder.dailySeconds(today, days = 7)

        assertEquals(
            listOf(today.minusDays(2), today),
            daily.map { it.first },
        )
        assertEquals(listOf(300L, 600L), daily.map { it.second })
    }

    @Test
    fun `streak counts consecutive days ending today or yesterday`() {
        val recorder = recorder()
        recorder.recordSession(today, 60)
        recorder.recordSession(today.minusDays(1), 60)
        recorder.recordSession(today.minusDays(2), 60)
        // A gap three days ago must not extend the streak.
        recorder.recordSession(today.minusDays(4), 60)

        assertEquals(3, recorder.currentStreak(today))
    }

    @Test
    fun `streak survives when today has no session yet`() {
        val recorder = recorder()
        recorder.recordSession(today.minusDays(1), 60)
        recorder.recordSession(today.minusDays(2), 60)

        assertEquals(2, recorder.currentStreak(today))
    }

    @Test
    fun `entries beyond the retention window are pruned`() {
        val recorder = recorder()
        recorder.recordSession(today.minusDays(ReadingStatsRecorder.RETENTION_DAYS + 1), 999)
        recorder.recordSession(today, 60)

        assertEquals(60, recorder.totalSeconds(today, days = 400))
    }
}
