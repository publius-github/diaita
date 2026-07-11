package app.diaita.data.db.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "meal_preset_items",
    foreignKeys = [
        ForeignKey(
            entity = MealPreset::class,
            parentColumns = ["id"],
            childColumns = ["presetId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = FoodItem::class,
            parentColumns = ["id"],
            childColumns = ["foodItemId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index("presetId"), Index("foodItemId")],
)
data class MealPresetItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val presetId: Long,
    val foodItemId: Long,
    val quantityGrams: Double,
)
