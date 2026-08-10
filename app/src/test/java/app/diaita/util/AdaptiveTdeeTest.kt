package app.diaita.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AdaptiveTdeeTest {
    @Test
    fun `maintenance equals intake when weight is stable`() {
        val tdee =
            AdaptiveTdee.estimate(
                dailyIntakes = List(28) { 2200.0 },
                startWeightKg = 80.0,
                endWeightKg = 80.0,
                spanDays = 28,
            )
        assertEquals(2200, tdee)
    }

    @Test
    fun `losing weight on a deficit raises the estimate above intake`() {
        // 1 kg lost over 28 days = 7700/28 = 275 kcal/day deficit.
        val tdee =
            AdaptiveTdee.estimate(
                dailyIntakes = List(28) { 2000.0 },
                startWeightKg = 80.0,
                endWeightKg = 79.0,
                spanDays = 28,
            )
        assertEquals(2275, tdee)
    }

    @Test
    fun `too short a window or too few logged days yields null`() {
        assertNull(AdaptiveTdee.estimate(List(10) { 2000.0 }, 80.0, 79.5, spanDays = 10))
        assertNull(AdaptiveTdee.estimate(List(5) { 2000.0 }, 80.0, 79.5, spanDays = 28))
    }

    @Test
    fun `implausible results are rejected`() {
        assertNull(AdaptiveTdee.estimate(List(14) { 500.0 }, 80.0, 85.0, spanDays = 14))
    }
}
