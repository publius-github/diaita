package app.diaita.ui.common

import org.junit.Assert.assertEquals
import org.junit.Test

class CalorieOverBandTest {
    @Test
    fun `within the goal is green`() {
        assertEquals(CalorieOverBand.WITHIN, calorieOverBand(1800.0, 2000, 2600))
        assertEquals(CalorieOverBand.WITHIN, calorieOverBand(2000.0, 2000, 2600))
    }

    @Test
    fun `over goal but under maintenance is amber`() {
        assertEquals(CalorieOverBand.OVER_GOAL, calorieOverBand(2300.0, 2000, 2600))
        assertEquals(CalorieOverBand.OVER_GOAL, calorieOverBand(2600.0, 2000, 2600))
    }

    @Test
    fun `past maintenance is red`() {
        assertEquals(CalorieOverBand.OVER_MAINTENANCE, calorieOverBand(2601.0, 2000, 2600))
    }

    @Test
    fun `without a maintenance above the goal any overshoot is red`() {
        assertEquals(CalorieOverBand.OVER_MAINTENANCE, calorieOverBand(2100.0, 2000, null))
        // Surplus plan: maintenance at or below the goal gives the amber zone no room.
        assertEquals(CalorieOverBand.OVER_MAINTENANCE, calorieOverBand(2900.0, 2800, 2400))
    }
}
