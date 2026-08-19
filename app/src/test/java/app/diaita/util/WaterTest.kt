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
