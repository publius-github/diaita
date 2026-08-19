package app.diaita.util

import org.junit.Assert.assertEquals
import org.junit.Test

class WaterTest {
    @Test
    fun `target is 30 ml per kg rounded to 50`() {
        assertEquals(2400, waterTargetMl(80.0))
        assertEquals(2200, waterTargetMl(72.5))
        assertEquals(1950, waterTargetMl(65.4))
    }

    @Test
    fun `falls back to two liters without a wizard weight`() {
        assertEquals(DEFAULT_WATER_TARGET_ML, waterTargetMl(null))
        assertEquals(DEFAULT_WATER_TARGET_ML, waterTargetMl(0.0))
    }

    @Test
    fun `ml per kg is configurable`() {
        assertEquals(3200, waterTargetMl(80.0, mlPerKg = 40))
        assertEquals(1600, waterTargetMl(80.0, mlPerKg = 20))
    }

    @Test
    fun `the food share comes off the target`() {
        // The 30 ml per kg guideline is total daily water; the bar only counts what is drunk.
        assertEquals(2300, waterTargetMl(96.0, mlPerKg = 30, foodSharePercent = 20))
        assertEquals(2900, waterTargetMl(96.0, mlPerKg = 30, foodSharePercent = 0))
        assertEquals(1600, waterTargetMl(80.0, mlPerKg = 40, foodSharePercent = 50))
    }

    @Test
    fun `the food share applies to the no-weight fallback too`() {
        assertEquals(1600, waterTargetMl(null, foodSharePercent = 20))
    }

    @Test
    fun `target never drops below the floor`() {
        assertEquals(MIN_WATER_TARGET_ML, waterTargetMl(20.0, mlPerKg = 15, foodSharePercent = 50))
    }

    @Test
    fun `band stays within up to the amber slack inclusive`() {
        assertEquals(WaterBand.WITHIN, waterBand(0, 2000, 50, 100))
        assertEquals(WaterBand.WITHIN, waterBand(2000, 2000, 50, 100))
        assertEquals(WaterBand.WITHIN, waterBand(3000, 2000, 50, 100))
    }

    @Test
    fun `band escalates to amber past the amber slack and red past the red slack`() {
        assertEquals(WaterBand.OVER_AMBER, waterBand(3001, 2000, 50, 100))
        assertEquals(WaterBand.OVER_AMBER, waterBand(4000, 2000, 50, 100))
        assertEquals(WaterBand.OVER_RED, waterBand(4001, 2000, 50, 100))
    }

    @Test
    fun `band is within when the target is absent`() {
        assertEquals(WaterBand.WITHIN, waterBand(5000, 0, 50, 100))
    }
}
