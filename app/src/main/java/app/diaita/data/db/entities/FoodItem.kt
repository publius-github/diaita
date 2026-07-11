package app.diaita.data.db.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class FoodSource { SEED, MANUAL, OPENFOODFACTS }

@Entity(
    tableName = "food_items",
    indices = [Index(value = ["barcode"]), Index(value = ["offId"])],
)
data class FoodItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val brand: String? = null,
    val barcode: String? = null,
    val caloriesPer100g: Double,
    val proteinPer100g: Double,
    val fatPer100g: Double,
    val carbsPer100g: Double,
    val defaultServingGrams: Double? = null,
    val source: FoodSource,
    val offId: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
)
