package app.diaita.ui.history

import org.junit.Assert.assertEquals
import org.junit.Test

class CalorieZoneTest {
    @Test
    fun `no goal is always on track`() {
        assertEquals(CalorieZone.ON_TRACK, calorieZone(5000.0, null, 2500))
        assertEquals(CalorieZone.ON_TRACK, calorieZone(5000.0, 0, 2500))
    }

    @Test
    fun `no maintenance falls back to two states`() {
        assertEquals(CalorieZone.ON_TRACK, calorieZone(1800.0, 2000, null))
        assertEquals(CalorieZone.ON_TRACK, calorieZone(2000.0, 2000, null))
        assertEquals(CalorieZone.OFF_TRACK, calorieZone(2001.0, 2000, null))
    }

    @Test
    fun `maintenance equal to goal falls back to two states`() {
        assertEquals(CalorieZone.ON_TRACK, calorieZone(2000.0, 2000, 2000))
        assertEquals(CalorieZone.OFF_TRACK, calorieZone(2100.0, 2000, 2000))
    }

    @Test
    fun `deficit plan zones`() {
        assertEquals(CalorieZone.ON_TRACK, calorieZone(1900.0, 2000, 2500))
        assertEquals(CalorieZone.ON_TRACK, calorieZone(2000.0, 2000, 2500))
        assertEquals(CalorieZone.BETWEEN, calorieZone(2200.0, 2000, 2500))
        assertEquals(CalorieZone.BETWEEN, calorieZone(2500.0, 2000, 2500))
        assertEquals(CalorieZone.OFF_TRACK, calorieZone(2600.0, 2000, 2500))
    }

    @Test
    fun `surplus plan zones mirror`() {
        assertEquals(CalorieZone.ON_TRACK, calorieZone(3100.0, 3000, 2500))
        assertEquals(CalorieZone.ON_TRACK, calorieZone(3000.0, 3000, 2500))
        assertEquals(CalorieZone.BETWEEN, calorieZone(2700.0, 3000, 2500))
        assertEquals(CalorieZone.BETWEEN, calorieZone(2500.0, 3000, 2500))
        assertEquals(CalorieZone.OFF_TRACK, calorieZone(2400.0, 3000, 2500))
    }
}
