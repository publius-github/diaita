package app.diaita.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DishMathTest {
    @Test
    fun `portion scales linearly against cooked weight`() {
        assertEquals(0.15, dishPortionFactor(cookedWeightGrams = 3000.0, eatenGrams = 450.0)!!, 1e-9)
    }

    @Test
    fun `whole dish gives factor one`() {
        assertEquals(1.0, dishPortionFactor(cookedWeightGrams = 3000.0, eatenGrams = 3000.0)!!, 1e-9)
    }

    @Test
    fun `eating more than one batch is allowed`() {
        assertEquals(1.5, dishPortionFactor(cookedWeightGrams = 1000.0, eatenGrams = 1500.0)!!, 1e-9)
    }

    @Test
    fun `plain preset without cooked weight never logs a portion`() {
        assertNull(dishPortionFactor(cookedWeightGrams = null, eatenGrams = 450.0))
    }

    @Test
    fun `non-positive weights never log a portion`() {
        assertNull(dishPortionFactor(cookedWeightGrams = 0.0, eatenGrams = 450.0))
        assertNull(dishPortionFactor(cookedWeightGrams = -100.0, eatenGrams = 450.0))
        assertNull(dishPortionFactor(cookedWeightGrams = 3000.0, eatenGrams = 0.0))
        assertNull(dishPortionFactor(cookedWeightGrams = 3000.0, eatenGrams = -450.0))
    }
}
