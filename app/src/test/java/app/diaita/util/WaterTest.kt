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
}
