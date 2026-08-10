package app.diaita.data.db.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** QUICK marks one-off calorie-only logs ("Quick entry"); they are hidden from search and recents. */
enum class FoodSource { SEED, MANUAL, OPENFOODFACTS, QUICK }

@Entity(
    tableName = "food_items",
    indices = [
        Index(value = ["barcode"]),
        Index(value = ["offId"], unique = true),
        Index(value = ["seedKey"], unique = true),
    ],
)
data class FoodItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** Stable identity of a seed-catalog entry (null for MANUAL/OPENFOODFACTS rows). */
    val seedKey: String? = null,
    val name: String,
    val brand: String? = null,
    val barcode: String? = null,
    val caloriesPer100g: Double,
    val proteinPer100g: Double,
    val fatPer100g: Double,
    val carbsPer100g: Double,
    val fiberPer100g: Double? = null,
    val sugarPer100g: Double? = null,
    val saltPer100g: Double? = null,
    val defaultServingGrams: Double? = null,
    /** Density for foods logged by volume (drinks, oils); null = grams-only. */
    val gramsPerMilliliter: Double? = null,
    /** Weight of one piece for foods logged by count (eggs, bread slices); null = grams-only. */
    val gramsPerPiece: Double? = null,
    val source: FoodSource,
    val offId: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    /** Set once the user corrects this entry; seed-catalog syncs then leave it alone. */
    val editedByUser: Boolean = false,
    /** Pinned by the user; favorites rank first in search and get their own quick row. */
    val isFavorite: Boolean = false,
    /** When an OPENFOODFACTS row last matched fresh upstream data; drives cache refresh. */
    val lastRefreshedAt: Long? = null,
)
