package app.diaita.util

import app.diaita.data.db.entities.FoodItem
import app.diaita.data.db.entities.FoodSource
import org.junit.Assert.assertEquals
import org.junit.Test

class NutrientMathTest {
    private val food =
        FoodItem(
            name = "Test food",
            caloriesPer100g = 250.0,
            proteinPer100g = 10.0,
            fatPer100g = 5.0,
            carbsPer100g = 40.0,
            source = FoodSource.MANUAL,
        )

    @Test
    fun `scales linearly with quantity`() {
        assertEquals(250.0, NutrientMath.caloriesFor(food, 100.0), 1e-9)
        assertEquals(125.0, NutrientMath.caloriesFor(food, 50.0), 1e-9)
        assertEquals(375.0, NutrientMath.caloriesFor(food, 150.0), 1e-9)
    }

    @Test
    fun `zero quantity yields zero for every macro`() {
        assertEquals(0.0, NutrientMath.caloriesFor(food, 0.0), 1e-9)
        assertEquals(0.0, NutrientMath.proteinFor(food, 0.0), 1e-9)
        assertEquals(0.0, NutrientMath.fatFor(food, 0.0), 1e-9)
        assertEquals(0.0, NutrientMath.carbsFor(food, 0.0), 1e-9)
    }

    @Test
    fun `fractional grams are supported`() {
        assertEquals(2.5 * 10.0 / 100.0, NutrientMath.proteinFor(food, 2.5), 1e-9)
        assertEquals(150.5 * 5.0 / 100.0, NutrientMath.fatFor(food, 150.5), 1e-9)
        assertEquals(33.3 * 40.0 / 100.0, NutrientMath.carbsFor(food, 33.3), 1e-9)
    }
}
