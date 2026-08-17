package app.diaita.data.seed

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SeedDishesTest {
    private val foodsByKey = SeedFoods.all.associateBy { it.seedKey!! }

    @Test
    fun `ships exactly forty dishes with unique keys`() {
        assertEquals(40, SeedDishes.all.size)
        assertEquals(
            SeedDishes.all.size,
            SeedDishes.all
                .map { it.seedKey }
                .toSet()
                .size,
        )
    }

    @Test
    fun `every ingredient resolves to a seed catalog entry`() {
        SeedDishes.all.forEach { dish ->
            assertTrue("${dish.seedKey} has no ingredients", dish.items.isNotEmpty())
            dish.items.forEach { (key, grams) ->
                assertTrue("${dish.seedKey}: unknown ingredient $key", key in foodsByKey)
                assertTrue("${dish.seedKey}: non-positive grams for $key", grams > 0)
            }
        }
    }

    @Test
    fun `cooked weight is positive and dish density is plausible`() {
        SeedDishes.all.forEach { dish ->
            assertTrue("${dish.seedKey}: cooked weight must be positive", dish.cookedWeightGrams > 0)
            val totalKcal =
                dish.items.sumOf { (key, grams) -> foodsByKey.getValue(key).caloriesPer100g * grams / 100.0 }
            val kcalPer100g = totalKcal / dish.cookedWeightGrams * 100
            assertTrue(
                "${dish.seedKey}: implausible ${kcalPer100g.toInt()} kcal/100g",
                kcalPer100g in 20.0..350.0,
            )
        }
    }

    @Test
    fun `since versions never exceed the current dish catalog version`() {
        SeedDishes.all.forEach { dish ->
            assertTrue(dish.sinceVersion in 1..SeedDishes.DISH_CATALOG_VERSION)
        }
    }

    @Test
    fun `retired flat dishes are gone from the food catalog but stay backfillable`() {
        SeedFoods.retired.keys.forEach { key ->
            assertTrue("$key still in SeedFoods.all", key !in foodsByKey)
        }
        // Retired keys must never be reused for a new food — the cleanup would delete it.
        assertTrue(SeedFoods.retired.keys.none { it in foodsByKey })
    }
}
