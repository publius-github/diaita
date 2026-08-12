package app.diaita.util

import org.junit.Assert.assertEquals
import org.junit.Test

class MicronutrientsTest {
    @Test
    fun `keeps display order and drops absent nutrients`() {
        val totals = micronutrientTotals(fiberGrams = 12.4, sugarGrams = null, saltGrams = 5.1)

        assertEquals(listOf(Micronutrient.FIBER, Micronutrient.SALT), totals.map { it.nutrient })
    }

    @Test
    fun `no data at all yields nothing to show`() {
        assertEquals(emptyList<MicronutrientTotal>(), micronutrientTotals(null, null, null))
    }

    @Test
    fun `a real zero is kept, unlike absent data`() {
        val totals = micronutrientTotals(fiberGrams = 0.0, sugarGrams = null, saltGrams = null)

        assertEquals(1, totals.size)
        assertEquals("0", totals.single().displayGrams)
    }

    @Test
    fun `fiber and sugar round to whole grams`() {
        val totals = micronutrientTotals(fiberGrams = 12.4, sugarGrams = 30.6, saltGrams = null)

        assertEquals(listOf("12", "31"), totals.map { it.displayGrams })
    }

    @Test
    fun `salt keeps one decimal`() {
        val salt = micronutrientTotals(null, null, saltGrams = 1.24).single()

        assertEquals(formatWeightForDisplay(1.24), salt.displayGrams)
    }
}
