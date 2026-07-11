package app.diaita.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Singleton row (id is always 1) holding the user's current daily targets. */
@Entity(tableName = "daily_goals")
data class DailyGoal(
    @PrimaryKey val id: Int = 1,
    val calorieTarget: Int,
    val proteinTargetG: Int,
    val fatTargetG: Int,
    val carbsTargetG: Int,
    val weightTargetKg: Double? = null,
    val updatedAt: Long = System.currentTimeMillis(),
)
