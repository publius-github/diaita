package app.diaita.ui.history

import org.junit.Assert.assertEquals
import org.junit.Test

class WeeklyBucketingTest {
    private fun day(
        date: String,
        calories: Double,
    ) = HistoryDayUi(date = date, label = date.substring(8), calories = calories, zone = CalorieZone.ON_TRACK)

    @Test
    fun `buckets keep the first day's date and average only logged days`() {
        val week1 = (1..7).map { day("2026-01-0$it", if (it <= 2) 2000.0 else 0.0) }
        val week2 = (8..14).map { day("2026-01-${"%02d".format(it)}", 1000.0) }

        val buckets = bucketWeekly(week1 + week2, goalCalories = null, maintenanceCalories = null)

        assertEquals(2, buckets.size)
        assertEquals("2026-01-01", buckets[0].date)
        // Two logged days at 2000 kcal; the five empty days must not drag the average down.
        assertEquals(2000.0, buckets[0].calories, 1e-9)
        assertEquals("2026-01-08", buckets[1].date)
        assertEquals(1000.0, buckets[1].calories, 1e-9)
    }

    @Test
    fun `a week with nothing logged stays zero`() {
        val days = (1..7).map { day("2026-02-0$it", 0.0) }

        val buckets = bucketWeekly(days, goalCalories = 2000, maintenanceCalories = null)

        assertEquals(1, buckets.size)
        assertEquals(0.0, buckets[0].calories, 1e-9)
    }

    @Test
    fun `bucket zone is classified from the weekly average`() {
        val days = (1..7).map { day("2026-03-0$it", 2500.0) }

        val buckets = bucketWeekly(days, goalCalories = 2000, maintenanceCalories = null)

        assertEquals(CalorieZone.OFF_TRACK, buckets[0].zone)
    }

    @Test
    fun `a trailing partial week forms its own bucket`() {
        val days = (1..9).map { day("2026-04-0$it", 1500.0) }

        val buckets = bucketWeekly(days, goalCalories = null, maintenanceCalories = null)

        assertEquals(2, buckets.size)
        assertEquals("2026-04-08", buckets[1].date)
        assertEquals(1500.0, buckets[1].calories, 1e-9)
    }
}
