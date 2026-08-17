package app.diaita.data.seed

import androidx.room.RoomDatabase
import androidx.room.withTransaction
import androidx.sqlite.db.SupportSQLiteDatabase
import app.diaita.data.db.AppDatabase
import app.diaita.data.db.entities.FoodSource
import app.diaita.data.db.entities.MealPreset
import app.diaita.data.db.entities.MealPresetItem
import app.diaita.data.db.entities.MealSlot
import app.diaita.data.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Keeps the seed food catalog and the seeded dish presets in sync on database open (which
 * also covers first creation). Both syncs are versioned independently.
 *
 * Foods: when the stored catalog version is behind [SeedFoods.CATALOG_VERSION], every entry
 * is upserted by [SeedFoods]' stable seedKey — existing rows get their name/macros/fiber/
 * sugar/salt/serving refreshed (so corrections reach existing installs), missing rows are
 * inserted. Rows keep their primary key, so diary entries referencing them are unaffected.
 * Rows the user has corrected ([FoodItem.editedByUser]) are never refreshed — a deliberate
 * local fix outranks the shipped catalog. Retired rows ([SeedFoods.retired]) are deleted,
 * but only when unedited and unreferenced — history always wins over catalog hygiene.
 *
 * Dishes: when the stored dish version is behind [SeedDishes.DISH_CATALOG_VERSION], each
 * [SeedDishes.SeedDish] is inserted only while the stored version predates its
 * sinceVersion (so a dish the user deleted never comes back on a later sync), refreshed in
 * place when present and unedited, and left alone once [MealPreset.editedByUser] is set.
 *
 * [databaseProvider] is invoked lazily (Room only opens the underlying SQLite file, and
 * fires callbacks, on first real query) so it's safe for it to reference a database
 * instance that is still being constructed by the caller at registration time.
 */
class SeedDatabaseCallback(
    private val appScope: CoroutineScope,
    private val settingsRepository: SettingsRepository,
    private val databaseProvider: () -> AppDatabase,
) : RoomDatabase.Callback() {
    override fun onOpen(db: SupportSQLiteDatabase) {
        super.onOpen(db)
        appScope.launch {
            syncFoods()
            syncDishes()
        }
    }

    private suspend fun syncFoods() {
        if (settingsRepository.seedCatalogVersion.first() >= SeedFoods.CATALOG_VERSION) return
        val foodItemDao = databaseProvider().foodItemDao()
        val existingByKey =
            foodItemDao
                .getBySource(FoodSource.SEED)
                .filter { it.seedKey != null }
                .associateBy { it.seedKey }
        SeedFoods.all.forEach { seed ->
            val existing = existingByKey[seed.seedKey]
            if (existing == null) {
                foodItemDao.insert(seed)
            } else if (existing.editedByUser) {
                // A deliberate user correction outranks the shipped catalog — leave it alone.
            } else {
                foodItemDao.update(
                    existing.copy(
                        name = seed.name,
                        caloriesPer100g = seed.caloriesPer100g,
                        proteinPer100g = seed.proteinPer100g,
                        fatPer100g = seed.fatPer100g,
                        carbsPer100g = seed.carbsPer100g,
                        fiberPer100g = seed.fiberPer100g,
                        sugarPer100g = seed.sugarPer100g,
                        saltPer100g = seed.saltPer100g,
                        defaultServingGrams = seed.defaultServingGrams,
                    ),
                )
            }
        }
        foodItemDao.deleteRetiredSeeds(SeedFoods.retired.keys.toList())
        settingsRepository.setSeedCatalogVersion(SeedFoods.CATALOG_VERSION)
    }

    private suspend fun syncDishes() {
        val storedVersion = settingsRepository.seedDishVersion.first()
        if (storedVersion >= SeedDishes.DISH_CATALOG_VERSION) return
        val database = databaseProvider()
        val presetDao = database.mealPresetDao()
        val foodIdByKey =
            database
                .foodItemDao()
                .getBySource(FoodSource.SEED)
                .mapNotNull { food -> food.seedKey?.let { it to food.id } }
                .toMap()
        val existingByKey = presetDao.getSeededWithItems().associateBy { it.preset.seedKey }
        database.withTransaction {
            SeedDishes.all.forEach { dish ->
                // A dish whose ingredient is missing can't produce correct numbers — skip
                // it whole rather than seed a silently incomplete composition (guarded
                // against in unit tests, so this is a belt-and-braces check).
                val items = dish.items.map { (key, grams) -> (foodIdByKey[key] ?: return@forEach) to grams }
                val existing = existingByKey[dish.seedKey]
                if (existing == null) {
                    if (storedVersion < dish.sinceVersion) {
                        val presetId =
                            presetDao.insertPreset(
                                MealPreset(
                                    seedKey = dish.seedKey,
                                    name = dish.name,
                                    mealSlot = MealSlot.UNSPECIFIED,
                                    cookedWeightGrams = dish.cookedWeightGrams,
                                ),
                            )
                        items.forEach { (foodId, grams) ->
                            presetDao.insertItem(
                                MealPresetItem(presetId = presetId, foodItemId = foodId, quantityGrams = grams),
                            )
                        }
                    }
                } else if (!existing.preset.editedByUser) {
                    presetDao.updateSeededDish(existing.preset.id, dish.name, dish.cookedWeightGrams)
                    presetDao.deleteItemsForPreset(existing.preset.id)
                    items.forEach { (foodId, grams) ->
                        presetDao.insertItem(
                            MealPresetItem(presetId = existing.preset.id, foodItemId = foodId, quantityGrams = grams),
                        )
                    }
                }
            }
        }
        settingsRepository.setSeedDishVersion(SeedDishes.DISH_CATALOG_VERSION)
    }
}
