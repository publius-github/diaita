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
     * entries keep pointing at the same row once upserts switch to seedKey.
     */
    val MIGRATION_4_5 =
        object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE food_items ADD COLUMN seedKey TEXT")
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_food_items_seedKey` ON `food_items` (`seedKey`)",
                )
                SeedFoods.all.forEach { seed ->
                    db.execSQL(
                        "UPDATE food_items SET seedKey = ? WHERE source = 'SEED' AND name = ? AND seedKey IS NULL",
                        arrayOf(seed.seedKey, seed.name),
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

    val all: Array<Migration> = arrayOf(MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8)
}
