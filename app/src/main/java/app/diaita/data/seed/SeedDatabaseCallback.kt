package app.diaita.data.seed

import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import app.diaita.data.db.AppDatabase
import app.diaita.data.db.entities.FoodSource
import app.diaita.data.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Keeps the seed food catalog in sync on database open (which also covers first
 * creation). Sync is versioned: when the stored catalog version is behind
 * [SeedFoods.CATALOG_VERSION], every entry is upserted by [SeedFoods]' stable seedKey —
 * existing rows get their name/macros/serving refreshed (so corrections reach existing
 * installs), missing rows are inserted. Rows keep their primary key, so diary entries
 * referencing them are unaffected. Rows the user has corrected ([FoodItem.editedByUser])
 * are never refreshed — a deliberate local fix outranks the shipped catalog.
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
            if (settingsRepository.seedCatalogVersion.first() >= SeedFoods.CATALOG_VERSION) {
                return@launch
            }
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
                            defaultServingGrams = seed.defaultServingGrams,
                        ),
                    )
                }
            }
            settingsRepository.setSeedCatalogVersion(SeedFoods.CATALOG_VERSION)
        }
    }
}
