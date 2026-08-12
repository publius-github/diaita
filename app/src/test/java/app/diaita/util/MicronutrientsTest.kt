package app.diaita.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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
    fun `fiber is a target, sugar and salt are limits`() {
        val progress = micronutrientProgress(micronutrientTotals(10.0, 10.0, 1.0))

        assertEquals(listOf(false, true, true), progress.map { it.isLimit })
        assertEquals(
            listOf(FIBER_TARGET_GRAMS, SUGAR_LIMIT_GRAMS, SALT_LIMIT_GRAMS),
            progress.map { it.referenceGrams },
        )
    }

    @Test
    fun `bar fills proportionally and clamps at the reference`() {
        val half = micronutrientProgress(micronutrientTotals(15.0, null, null)).single()
        val over = micronutrientProgress(micronutrientTotals(45.0, null, null)).single()

        assertEquals(0.5f, half.fraction, 0.001f)
        assertEquals(1f, over.fraction, 0.001f)
    }

    @Test
    fun `passing a limit is exceeded, never reached`() {
        val salt = micronutrientProgress(micronutrientTotals(null, null, SALT_LIMIT_GRAMS + 0.5)).single()

        assertTrue(salt.exceeded)
        assertFalse(salt.reached)
    }

    @Test
    fun `meeting the fiber target is reached, never exceeded`() {
        val fiber = micronutrientProgress(micronutrientTotals(FIBER_TARGET_GRAMS, null, null)).single()

        assertTrue(fiber.reached)
        assertFalse(fiber.exceeded)
    }

    @Test
    fun `staying under a limit is neither`() {
        val sugar = micronutrientProgress(micronutrientTotals(null, SUGAR_LIMIT_GRAMS - 1, null)).single()

        assertFalse(sugar.exceeded)
        assertFalse(sugar.reached)
    }
}
