package app.diaita.data.repository

import androidx.room.withTransaction
import app.diaita.data.db.AppDatabase
import app.diaita.data.db.dao.MealPresetDao
import app.diaita.data.db.dao.PresetWithItems
import app.diaita.data.db.entities.DiaryEntry
import app.diaita.data.db.entities.MealPreset
import app.diaita.data.db.entities.MealPresetItem
import app.diaita.data.db.entities.MealSlot
import app.diaita.util.DateUtils
import app.diaita.util.dishPortionFactor
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

    suspend fun createPreset(
        name: String,
        mealSlot: MealSlot,
        items: List<Pair<Long, Double>>,
        cookedWeightGrams: Double? = null,
    ): Long =
        database.withTransaction {
            val presetId =
                mealPresetDao.insertPreset(
                    MealPreset(name = name, mealSlot = mealSlot, cookedWeightGrams = cookedWeightGrams),
                )
            items.forEach { (foodItemId, quantityGrams) ->
                mealPresetDao.insertItem(
                    MealPresetItem(presetId = presetId, foodItemId = foodItemId, quantityGrams = quantityGrams),
                )
            }
            presetId
        }

    /** Replaces the preset's header (name + slot + cooked weight) and full item list in one transaction. */
    suspend fun updatePreset(
        presetId: Long,
        name: String,
        mealSlot: MealSlot,
        items: List<Pair<Long, Double>>,
        cookedWeightGrams: Double? = null,
    ) {
        database.withTransaction {
            mealPresetDao.updatePresetHeader(presetId, name, mealSlot, cookedWeightGrams)
            mealPresetDao.deleteItemsForPreset(presetId)
            items.forEach { (foodItemId, quantityGrams) ->
                mealPresetDao.insertItem(
                    MealPresetItem(presetId = presetId, foodItemId = foodItemId, quantityGrams = quantityGrams),
                )
            }
        }
    }

    suspend fun deletePreset(preset: MealPreset) = mealPresetDao.deletePreset(preset)

    /**
     * Re-inserts a previously deleted preset and all its items as-is (same ids) — used to
     * support delete-undo. Insert-or-ignore keeps a double "Undo" from throwing on reused ids.
     */
    suspend fun restorePreset(presetWithItems: PresetWithItems) {
        database.withTransaction {
            mealPresetDao.insertPresetOrIgnore(presetWithItems.preset)
            presetWithItems.items.forEach { mealPresetDao.insertItemOrIgnore(it) }
        }
    }

    /**
     * One-tap logging: inserts one [DiaryEntry] per constituent item, all in a single transaction,
     * tagged with [DiaryEntry.sourcePresetId]/[DiaryEntry.sourcePresetName] and a shared [loggedAt]
     * timestamp so the Diary screen can display them as one grouped row instead of N separate foods.
     */
    suspend fun logPreset(
        presetId: Long,
        logDate: String,
        fallbackSlot: MealSlot,
    ) = logScaled(presetId, logDate, fallbackSlot, factor = 1.0)

    /**
     * Logs an eaten portion of a dish (a preset with [MealPreset.cookedWeightGrams]): every raw
     * ingredient weight is scaled by eatenGrams / cookedWeightGrams, so nutrition comes from the
     * raw per-100g values and water loss/gain during cooking cancels out. Inserted entries are
     * snapshots — editing the dish later leaves history untouched. No-op for plain presets.
     */
    suspend fun logDishPortion(
        presetId: Long,
        eatenGrams: Double,
        logDate: String,
        fallbackSlot: MealSlot,
    ) {
        database.withTransaction {
            val cookedWeight = mealPresetDao.getWithItems(presetId)?.preset?.cookedWeightGrams
            val factor = dishPortionFactor(cookedWeight, eatenGrams) ?: return@withTransaction
            logScaled(presetId, logDate, fallbackSlot, factor)
        }
    }

    private suspend fun logScaled(
        presetId: Long,
        logDate: String,
        fallbackSlot: MealSlot,
        factor: Double,
    ) {
        database.withTransaction {
            val presetWithItems = mealPresetDao.getWithItems(presetId) ?: return@withTransaction
            val diaryEntryDao = database.diaryEntryDao()
            val loggedAt = DateUtils.loggedAtFor(logDate)
            // A preset that declares its own meal logs under that meal; an unassigned (legacy)
            // preset has no opinion, so use the caller-provided clock-based fallback. Either way
            // this is what places the log under the right Diary section and day-part segment.
            val mealSlot =
                presetWithItems.preset.mealSlot.takeUnless { it == MealSlot.UNSPECIFIED }
                    ?: fallbackSlot
            presetWithItems.items.forEach { item ->
                diaryEntryDao.insert(
                    DiaryEntry(
                        foodItemId = item.foodItemId,
                        quantityGrams = item.quantityGrams * factor,
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
