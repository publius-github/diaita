package app.diaita.util

import app.diaita.data.db.entities.FoodItem

object NutrientMath {
    fun caloriesFor(food: FoodItem, quantityGrams: Double): Double =
        food.caloriesPer100g * quantityGrams / 100.0

    fun proteinFor(food: FoodItem, quantityGrams: Double): Double =
        food.proteinPer100g * quantityGrams / 100.0

    fun fatFor(food: FoodItem, quantityGrams: Double): Double =
        food.fatPer100g * quantityGrams / 100.0

    fun carbsFor(food: FoodItem, quantityGrams: Double): Double =
        food.carbsPer100g * quantityGrams / 100.0
}
