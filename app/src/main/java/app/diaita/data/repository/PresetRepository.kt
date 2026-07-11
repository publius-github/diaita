package app.diaita.data.repository

import androidx.room.withTransaction
import app.diaita.data.db.AppDatabase
import app.diaita.data.db.dao.MealPresetDao
import app.diaita.data.db.dao.PresetWithItems
import app.diaita.data.db.entities.DiaryEntry
import app.diaita.data.db.entities.MealPreset
import app.diaita.data.db.entities.MealPresetItem
import app.diaita.util.DateUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/** A preset with its items plus the total calories of one serving, for list display. */
data class PresetSummary(
    val preset: MealPreset,
    val items: List<MealPresetItem>,
    val totalCalories: Double,
)

class PresetRepository(
    private val database: AppDatabase,
    private val mealPresetDao: MealPresetDao,
) {
    fun getAllWithItems(): Flow<List<PresetWithItems>> = mealPresetDao.getAllWithItems()

    fun getAllSummaries(): Flow<List<PresetSummary>> =
        combine(mealPresetDao.getAllWithItems(), mealPresetDao.presetCalories()) { presets, calories ->
            val caloriesByPresetId = calories.associate { it.presetId to it.calories }
            presets.map { withItems ->
                PresetSummary(
                    preset = withItems.preset,
                    items = withItems.items,
                    totalCalories = caloriesByPresetId[withItems.preset.id] ?: 0.0,
                )
            }
        }

    suspend fun getWithItems(presetId: Long): PresetWithItems? = mealPresetDao.getWithItems(presetId)

    suspend fun createPreset(name: String, items: List<Pair<Long, Double>>): Long =
        database.withTransaction {
            val presetId = mealPresetDao.insertPreset(MealPreset(name = name))
            items.forEach { (foodItemId, quantityGrams) ->
                mealPresetDao.insertItem(
                    MealPresetItem(presetId = presetId, foodItemId = foodItemId, quantityGrams = quantityGrams),
                )
            }
            presetId
        }

    /** Replaces the preset's name and full item list in one transaction (simplest correct update path). */
    suspend fun updatePreset(presetId: Long, name: String, items: List<Pair<Long, Double>>) {
        database.withTransaction {
            mealPresetDao.renamePreset(presetId, name)
            mealPresetDao.deleteItemsForPreset(presetId)
            items.forEach { (foodItemId, quantityGrams) ->
                mealPresetDao.insertItem(
                    MealPresetItem(presetId = presetId, foodItemId = foodItemId, quantityGrams = quantityGrams),
                )
            }
        }
    }

    suspend fun deletePreset(preset: MealPreset) = mealPresetDao.deletePreset(preset)

    /** Re-inserts a previously deleted preset and all its items as-is (same ids) — used to support delete-undo. */
    suspend fun restorePreset(presetWithItems: PresetWithItems) {
        database.withTransaction {
            mealPresetDao.insertPreset(presetWithItems.preset)
            presetWithItems.items.forEach { mealPresetDao.insertItem(it) }
        }
    }

    /**
     * One-tap logging: inserts one [DiaryEntry] per constituent item, all in a single transaction,
     * tagged with [DiaryEntry.sourcePresetId]/[DiaryEntry.sourcePresetName] and a shared [loggedAt]
     * timestamp so the Diary screen can display them as one grouped row instead of N separate foods.
     */
    suspend fun logPreset(presetId: Long, logDate: String) {
        database.withTransaction {
            val presetWithItems = mealPresetDao.getWithItems(presetId) ?: return@withTransaction
            val diaryEntryDao = database.diaryEntryDao()
            val loggedAt = System.currentTimeMillis()
            // Presets are one-tap logs with no slot picker, so infer the meal from the clock —
            // this is what places the log under the right Diary section and day-part segment.
            val mealSlot = DateUtils.defaultMealSlotNow()
            presetWithItems.items.forEach { item ->
                diaryEntryDao.insert(
                    DiaryEntry(
                        foodItemId = item.foodItemId,
                        quantityGrams = item.quantityGrams,
                        logDate = logDate,
                        loggedAt = loggedAt,
                        mealSlot = mealSlot,
                        sourcePresetId = presetWithItems.preset.id,
                        sourcePresetName = presetWithItems.preset.name,
                    ),
                )
            }
        }
    }
}
