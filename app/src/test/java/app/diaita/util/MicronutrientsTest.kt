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

    @Test
    fun `within the limit is green regardless of tolerance`() {
        assertEquals(MicronutrientBand.WITHIN, micronutrientBand(Micronutrient.SUGAR, 50.0, 0))
        assertEquals(MicronutrientBand.WITHIN, micronutrientBand(Micronutrient.SALT, 4.9, 0))
        assertEquals(MicronutrientBand.WITHIN, micronutrientBand(Micronutrient.FIBER, 45.0, 0))
    }

    @Test
    fun `over the limit but inside the tolerance is amber`() {
        assertEquals(MicronutrientBand.OVER, micronutrientBand(Micronutrient.SUGAR, 74.9, 50))
        assertEquals(MicronutrientBand.OVER, micronutrientBand(Micronutrient.SALT, 6.0, 20))
        assertEquals(MicronutrientBand.OVER, micronutrientBand(Micronutrient.FIBER, 80.0, 15))
    }

    @Test
    fun `past the tolerance is red`() {
        assertEquals(MicronutrientBand.FAR_OVER, micronutrientBand(Micronutrient.SUGAR, 75.1, 50))
        assertEquals(MicronutrientBand.FAR_OVER, micronutrientBand(Micronutrient.SALT, 6.1, 20))
        assertEquals(MicronutrientBand.FAR_OVER, micronutrientBand(Micronutrient.FIBER, 81.0, 15))
    }

    @Test
    fun `zero tolerance turns red immediately past the limit`() {
        assertEquals(MicronutrientBand.FAR_OVER, micronutrientBand(Micronutrient.SALT, 5.01, 0))
    }
}
