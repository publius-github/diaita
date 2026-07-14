package app.diaita.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "meal_presets")
data class MealPreset(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    /** Which meal this preset belongs to, so the list can group it like the Diary screen. */
    val mealSlot: MealSlot = MealSlot.UNSPECIFIED,
    val createdAt: Long = System.currentTimeMillis(),
)
