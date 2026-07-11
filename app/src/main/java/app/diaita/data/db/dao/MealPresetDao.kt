package app.diaita.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import app.diaita.data.db.entities.MealPreset
import app.diaita.data.db.entities.MealPresetItem
import kotlinx.coroutines.flow.Flow

data class PresetWithItems(
    @Embedded val preset: MealPreset,
    @Relation(parentColumn = "id", entityColumn = "presetId")
    val items: List<MealPresetItem>,
)

@Dao
interface MealPresetDao {
    @Insert
    suspend fun insertPreset(preset: MealPreset): Long

    @Insert
    suspend fun insertItem(item: MealPresetItem): Long

    @Delete
    suspend fun deletePreset(preset: MealPreset)

    @Delete
    suspend fun deleteItem(item: MealPresetItem)

    @Query("UPDATE meal_presets SET name = :name WHERE id = :presetId")
    suspend fun renamePreset(presetId: Long, name: String)

    @Query("DELETE FROM meal_preset_items WHERE presetId = :presetId")
    suspend fun deleteItemsForPreset(presetId: Long)

    @Transaction
    @Query("SELECT * FROM meal_presets ORDER BY name ASC")
    fun getAllWithItems(): Flow<List<PresetWithItems>>

    @Transaction
    @Query("SELECT * FROM meal_presets WHERE id = :id")
    suspend fun getWithItems(id: Long): PresetWithItems?
}
