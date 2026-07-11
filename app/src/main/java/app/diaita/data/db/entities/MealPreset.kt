package app.diaita.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "meal_presets")
data class MealPreset(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
)
