package app.diaita.data.db.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

enum class MealSlot { BREAKFAST, LUNCH, DINNER, SNACK, UNSPECIFIED }

@Entity(
    tableName = "diary_entries",
    foreignKeys = [
        ForeignKey(
            entity = FoodItem::class,
            parentColumns = ["id"],
            childColumns = ["foodItemId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index("foodItemId"), Index("logDate")],
)
data class DiaryEntry(
    @androidx.room.PrimaryKey(autoGenerate = true) val id: Long = 0,
    val foodItemId: Long,
    val quantityGrams: Double,
    /** ISO-8601 date, e.g. "2026-07-10" — enables cheap equality/range queries per day. */
    val logDate: String,
    val loggedAt: Long,
    val mealSlot: MealSlot = MealSlot.UNSPECIFIED,
    /**
     * Set when this entry was inserted by [app.diaita.data.repository.PresetRepository.logPreset]
     * rather than logged individually. [sourcePresetName] is a snapshot of the preset's name at logging time
     * (so history stays correct even if the preset is later renamed or deleted). Entries sharing the same
     * (sourcePresetId, loggedAt) pair came from the same one-tap preset log and are grouped as one Diary row.
     */
    val sourcePresetId: Long? = null,
    val sourcePresetName: String? = null,
)
