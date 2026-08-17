package app.diaita.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "meal_presets")
data class MealPreset(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
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
    val createdAt: Long = System.currentTimeMillis(),
)
