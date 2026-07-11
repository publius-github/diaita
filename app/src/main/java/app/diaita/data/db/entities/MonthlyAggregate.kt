package app.diaita.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Per-month averages of days whose raw diary entries have been folded away by the retention job
 * (raw entries are kept for 30 days, monthly aggregates for 12 months). [daysCount] is how many
 * logged days contributed — needed to merge correctly when more days of the same month get folded later.
 */
@Entity(tableName = "monthly_aggregates")
data class MonthlyAggregate(
    /** e.g. "2026-06" */
    @PrimaryKey val yearMonth: String,
    val avgCalories: Double,
    val avgProtein: Double,
    val avgFat: Double,
    val avgCarbs: Double,
    val daysCount: Int,
    val computedAt: Long = System.currentTimeMillis(),
)
