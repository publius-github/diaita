package app.diaita.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class WeightTrendTest {
    private val start = LocalDate.of(2026, 7, 1)

    private fun steadyLoss(
        days: Int,
        startKg: Double,
        kgPerDay: Double,
    ): List<Pair<LocalDate, Double>> = (0 until days).map { start.plusDays(it.toLong()) to startKg + it * kgPerDay }

    @Test
    fun `steady loss yields the exact weekly rate`() {
        val rate = WeightTrend.ratePerWeek(steadyLoss(days = 15, startKg = 90.0, kgPerDay = -0.1))
        assertEquals(-0.7, rate!!, 1e-9)
    }

    @Test
    fun `fewer than two points or a short span yields no rate`() {
        assertNull(WeightTrend.ratePerWeek(listOf(start to 90.0)))
        assertNull(WeightTrend.ratePerWeek(listOf(start to 90.0, start.plusDays(3) to 89.5)))
    }

    @Test
    fun `projection lands where the trend crosses the target`() {
        val entries = steadyLoss(days = 15, startKg = 90.0, kgPerDay = -0.1)
        // Latest = 88.6 on day 14; target 87.2 is 1.4 kg away at 0.1 kg/day = 14 days.
        val projected = WeightTrend.projectedTargetDate(entries, targetKg = 87.2)
        assertEquals(start.plusDays(28), projected)
    }

    @Test
    fun `no projection when moving away from the target`() {
        val entries = steadyLoss(days = 15, startKg = 90.0, kgPerDay = 0.1)
        assertNull(WeightTrend.projectedTargetDate(entries, targetKg = 85.0))
    }

    @Test
    fun `no projection on a flat trend`() {
        val entries = (0 until 15).map { start.plusDays(it.toLong()) to 90.0 }
        assertNull(WeightTrend.projectedTargetDate(entries, targetKg = 85.0))
    }
}
