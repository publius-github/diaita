package app.diaita.data.db.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "meal_presets",
    indices = [Index(value = ["seedKey"], unique = true)],
)
data class MealPreset(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** Stable identity of a seeded dish ([app.diaita.data.seed.SeedDishes]); null for user-created presets. */
    val seedKey: String? = null,
    val name: String,
    /** Which meal this preset belongs to, so the list can group it like the Diary screen. */
    val mealSlot: MealSlot = MealSlot.UNSPECIFIED,
    /**
     * Weight of the whole cooked dish. Non-null turns the preset into a "dish": items stay
     * raw ingredient weights, and logging scales them by eatenGrams / cookedWeightGrams
     * instead of inserting them one-tap as-is. Cooked weight may legitimately be below the
     * raw total (roasting) or above it (soup), so it is never validated against the items.
     */
    val cookedWeightGrams: Double? = null,
    /** Set once the user edits this preset; seeded-dish syncs then leave it alone (only meaningful with [seedKey]). */
    val editedByUser: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
)
