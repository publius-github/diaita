package app.diaita.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import app.diaita.data.seed.SeedFoods

/**
 * All schema migrations, registered in [AppDatabase]. The migration baseline is v4 —
 * the last schema shipped while the app still used destructive migration; every schema
 * bump from here on must add a Migration here (enforced by the androidTest migration test).
 */
object Migrations {
    /**
     * Adds the stable seed-catalog identity column. Existing SEED rows are backfilled by
     * matching their (historically stable) names against the current catalog, so diary
     * entries keep pointing at the same row once upserts switch to seedKey. Retired
     * entries are included so the backfill keeps covering exactly the catalog this
     * migration originally shipped against.
     */
    val MIGRATION_4_5 =
        object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE food_items ADD COLUMN seedKey TEXT")
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_food_items_seedKey` ON `food_items` (`seedKey`)",
                )
                val backfill = SeedFoods.all.map { it.seedKey!! to it.name } + SeedFoods.retired.toList()
                backfill.forEach { (seedKey, name) ->
                    db.execSQL(
                        "UPDATE food_items SET seedKey = ? WHERE source = 'SEED' AND name = ? AND seedKey IS NULL",
                        arrayOf(seedKey, name),
                    )
                }
            }
        }

    /**
     * Adds the meal-slot column to presets so the Preset list can group by Breakfast/Lunch/
     * Dinner like the Diary screen. Existing presets predate the concept, so they default to
     * UNSPECIFIED and surface under the "Other" group until the user assigns one.
     */
    val MIGRATION_5_6 =
        object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE meal_presets ADD COLUMN mealSlot TEXT NOT NULL DEFAULT 'UNSPECIFIED'",
                )
            }
        }

    /**
     * Adds the wizard-estimated maintenance (TDEE) column to the goal row. Existing goals
     * stay NULL — maintenance only becomes known the next time the wizard is completed.
     */
    val MIGRATION_6_7 =
        object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE daily_goals ADD COLUMN maintenanceCalories INTEGER")
            }
        }

    /**
     * Adds the user-correction flag to foods. Existing rows default to not-edited; the flag
     * flips when the user fixes an entry from search, and seed syncs skip flagged rows so
     * catalog updates never clobber a deliberate correction.
     */
    val MIGRATION_7_8 =
        object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE food_items ADD COLUMN editedByUser INTEGER NOT NULL DEFAULT 0")
            }
        }

    /**
     * Catalog upgrade batch: deduplicates the OpenFoodFacts cache and makes `offId` unique
     * (fixes the concurrent-search double-insert race), then adds the extended-nutrient,
     * unit-conversion, favorite and cache-freshness columns. Diary entries and preset items
     * pointing at duplicate OFF rows are remapped onto the kept (earliest) row first, so no
     * foreign key ever dangles.
     */
    val MIGRATION_8_9 =
        object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TEMP TABLE off_remap AS
                    SELECT id, (SELECT MIN(f2.id) FROM food_items f2 WHERE f2.offId = f.offId) AS keepId
                    FROM food_items f WHERE f.offId IS NOT NULL
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    UPDATE diary_entries SET foodItemId =
                        (SELECT keepId FROM off_remap WHERE off_remap.id = diary_entries.foodItemId)
                    WHERE foodItemId IN (SELECT id FROM off_remap WHERE id != keepId)
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    UPDATE meal_preset_items SET foodItemId =
                        (SELECT keepId FROM off_remap WHERE off_remap.id = meal_preset_items.foodItemId)
                    WHERE foodItemId IN (SELECT id FROM off_remap WHERE id != keepId)
                    """.trimIndent(),
                )
                db.execSQL("DELETE FROM food_items WHERE id IN (SELECT id FROM off_remap WHERE id != keepId)")
                db.execSQL("DROP TABLE off_remap")
                db.execSQL("DROP INDEX IF EXISTS `index_food_items_offId`")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_food_items_offId` ON `food_items` (`offId`)")

                db.execSQL("ALTER TABLE food_items ADD COLUMN fiberPer100g REAL")
                db.execSQL("ALTER TABLE food_items ADD COLUMN sugarPer100g REAL")
                db.execSQL("ALTER TABLE food_items ADD COLUMN saltPer100g REAL")
                db.execSQL("ALTER TABLE food_items ADD COLUMN gramsPerMilliliter REAL")
                db.execSQL("ALTER TABLE food_items ADD COLUMN gramsPerPiece REAL")
                db.execSQL("ALTER TABLE food_items ADD COLUMN isFavorite INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE food_items ADD COLUMN lastRefreshedAt INTEGER")
            }
        }

    /**
     * Adds the cooked-dish weight to presets. A preset with a non-null value is a "dish":
     * its items are raw ingredient weights, and logging asks for the eaten cooked grams,
     * scaling every item by eaten/cooked. Null keeps plain one-tap preset semantics.
     */
    val MIGRATION_9_10 =
        object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE meal_presets ADD COLUMN cookedWeightGrams REAL")
            }
        }

    /**
     * Adds the seeded-dish identity and the user-edit flag to presets, mirroring the
     * food catalog's seedKey/editedByUser pattern so shipped dishes can be refreshed
     * without clobbering user-tuned proportions. Existing presets are all user-created:
     * seedKey stays NULL and they are never touched by the dish sync.
     */
    val MIGRATION_10_11 =
        object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE meal_presets ADD COLUMN seedKey TEXT")
                db.execSQL("ALTER TABLE meal_presets ADD COLUMN editedByUser INTEGER NOT NULL DEFAULT 0")
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_meal_presets_seedKey` ON `meal_presets` (`seedKey`)",
                )
            }
        }

    /**
     * Adds the settings mirror: a write-through copy of every DataStore preference, so the
     * single-file database backup carries the user's settings. Before this, a reinstall +
     * restore brought the diary back but silently reset coach tone, day boundaries and
     * every other preference (real user pain, 2026-08-17).
     */
    val MIGRATION_11_12 =
        object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `settings_mirror` " +
                        "(`key` TEXT NOT NULL, `type` TEXT NOT NULL, `value` TEXT NOT NULL, PRIMARY KEY(`key`))",
                )
            }
        }

    /** Water tracking (F2): one row per tap of the water button, summed per day for the bar. */
    val MIGRATION_12_13 =
        object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `water_entries` " +
                        "(`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `date` TEXT NOT NULL, " +
                        "`amountMl` INTEGER NOT NULL, `loggedAt` INTEGER NOT NULL)",
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_water_entries_date` ON `water_entries` (`date`)")
            }
        }

    val all: Array<Migration> =
        arrayOf(
            MIGRATION_4_5,
            MIGRATION_5_6,
            MIGRATION_6_7,
            MIGRATION_7_8,
            MIGRATION_8_9,
            MIGRATION_9_10,
            MIGRATION_10_11,
            MIGRATION_11_12,
            MIGRATION_12_13,
        )
}
