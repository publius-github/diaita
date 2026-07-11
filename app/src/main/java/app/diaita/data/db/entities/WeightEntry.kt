package app.diaita.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/** One weigh-in per day — the ISO date is the primary key, re-logging the same day replaces it. */
@Entity(tableName = "weight_entries")
data class WeightEntry(
    @PrimaryKey val date: String,
    val weightKg: Double,
    val loggedAt: Long = System.currentTimeMillis(),
)
