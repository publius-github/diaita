package app.diaita.data.db

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Replays every migration on real schema snapshots from app/schemas. Add a case here for
 * each new schema version — this is what keeps "no destructive migration" honest.
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {
    private val testDbName = "migration-test.db"

    @get:Rule
    val helper =
        MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(),
            AppDatabase::class.java,
            emptyList(),
            FrameworkSQLiteOpenHelperFactory(),
        )

    @Test
    fun migrate4To5_backfillsSeedKeysByName() {
        helper.createDatabase(testDbName, 4).use { db ->
            db.execSQL(
                """
                INSERT INTO food_items
                    (name, caloriesPer100g, proteinPer100g, fatPer100g, carbsPer100g, source, createdAt)
                VALUES
                    ('White rice, cooked', 130.0, 2.7, 0.3, 28.0, 'SEED', 0),
                    ('My custom food', 100.0, 1.0, 1.0, 1.0, 'MANUAL', 0)
                """.trimIndent(),
            )
        }

        helper.runMigrationsAndValidate(testDbName, 5, true, Migrations.MIGRATION_4_5).use { db ->
            db.query("SELECT seedKey FROM food_items WHERE name = 'White rice, cooked'").use { cursor ->
                cursor.moveToFirst()
                assertEquals("white_rice_cooked", cursor.getString(0))
            }
            db.query("SELECT seedKey FROM food_items WHERE name = 'My custom food'").use { cursor ->
                cursor.moveToFirst()
                assertEquals(null, cursor.getString(0))
            }
        }
    }

    @Test
    fun migrate5To6_defaultsExistingPresetsToUnspecifiedSlot() {
        helper.createDatabase(testDbName, 5).use { db ->
            db.execSQL(
                "INSERT INTO meal_presets (name, createdAt) VALUES ('My usual breakfast', 0)",
            )
        }

        helper.runMigrationsAndValidate(testDbName, 6, true, Migrations.MIGRATION_5_6).use { db ->
            db.query("SELECT mealSlot FROM meal_presets WHERE name = 'My usual breakfast'").use { cursor ->
                cursor.moveToFirst()
                assertEquals("UNSPECIFIED", cursor.getString(0))
            }
        }
    }

    @Test
    fun migrate6To7_existingGoalHasNullMaintenance() {
        helper.createDatabase(testDbName, 6).use { db ->
            db.execSQL(
                """
                INSERT INTO daily_goals
                    (id, calorieTarget, proteinTargetG, fatTargetG, carbsTargetG, weightTargetKg, updatedAt)
                VALUES
                    (1, 2000, 150, 55, 225, NULL, 0)
                """.trimIndent(),
            )
        }

        helper.runMigrationsAndValidate(testDbName, 7, true, Migrations.MIGRATION_6_7).use { db ->
            db.query("SELECT calorieTarget, maintenanceCalories FROM daily_goals WHERE id = 1").use { cursor ->
                cursor.moveToFirst()
                assertEquals(2000, cursor.getInt(0))
                assertEquals(true, cursor.isNull(1))
            }
        }
    }

    @Test
    fun migrate8To9_dedupesOffRowsAndRemapsReferences() {
        helper.createDatabase(testDbName, 8).use { db ->
            db.execSQL(
                """
                INSERT INTO food_items
                    (id, name, caloriesPer100g, proteinPer100g, fatPer100g, carbsPer100g,
                     source, offId, createdAt, editedByUser)
                VALUES
                    (1, 'Nutella', 539.0, 6.3, 30.9, 57.5, 'OPENFOODFACTS', '3017620422003', 0, 0),
                    (2, 'Nutella', 539.0, 6.3, 30.9, 57.5, 'OPENFOODFACTS', '3017620422003', 0, 0),
                    (3, 'Oats', 379.0, 13.2, 6.5, 67.7, 'SEED', NULL, 0, 0)
                """.trimIndent(),
            )
            db.execSQL(
                """
                INSERT INTO diary_entries (foodItemId, quantityGrams, logDate, loggedAt, mealSlot)
                VALUES (2, 30.0, '2026-08-01', 0, 'UNSPECIFIED')
                """.trimIndent(),
            )
        }

        helper.runMigrationsAndValidate(testDbName, 9, true, Migrations.MIGRATION_8_9).use { db ->
            db.query("SELECT COUNT(*) FROM food_items WHERE offId = '3017620422003'").use { cursor ->
                cursor.moveToFirst()
                assertEquals(1, cursor.getInt(0))
            }
            db.query("SELECT foodItemId FROM diary_entries").use { cursor ->
                cursor.moveToFirst()
                assertEquals(1, cursor.getInt(0))
            }
            db.query("SELECT isFavorite, fiberPer100g FROM food_items WHERE id = 3").use { cursor ->
                cursor.moveToFirst()
                assertEquals(0, cursor.getInt(0))
                assertEquals(true, cursor.isNull(1))
            }
        }
    }

    @Test
    fun migrate9To10_existingPresetsHaveNullCookedWeight() {
        helper.createDatabase(testDbName, 9).use { db ->
            db.execSQL(
                "INSERT INTO meal_presets (name, mealSlot, createdAt) VALUES ('My usual breakfast', 'BREAKFAST', 0)",
            )
        }

        helper.runMigrationsAndValidate(testDbName, 10, true, Migrations.MIGRATION_9_10).use { db ->
            db.query("SELECT cookedWeightGrams FROM meal_presets WHERE name = 'My usual breakfast'").use { cursor ->
                cursor.moveToFirst()
                assertEquals(true, cursor.isNull(0))
            }
        }
    }

    @Test
    fun migrate10To11_existingPresetsStayUserOwned() {
        helper.createDatabase(testDbName, 10).use { db ->
            db.execSQL(
                "INSERT INTO meal_presets (name, mealSlot, createdAt) VALUES ('My usual breakfast', 'BREAKFAST', 0)",
            )
        }

        helper.runMigrationsAndValidate(testDbName, 11, true, Migrations.MIGRATION_10_11).use { db ->
            db.query("SELECT seedKey, editedByUser FROM meal_presets WHERE name = 'My usual breakfast'").use { cursor ->
                cursor.moveToFirst()
                assertEquals(true, cursor.isNull(0))
                assertEquals(0, cursor.getInt(1))
            }
        }
    }

    @Test
    fun migrate11To12_createsEmptySettingsMirror() {
        helper.createDatabase(testDbName, 11).close()

        helper.runMigrationsAndValidate(testDbName, 12, true, Migrations.MIGRATION_11_12).use { db ->
            db.query("SELECT COUNT(*) FROM settings_mirror").use { cursor ->
                cursor.moveToFirst()
                assertEquals(0, cursor.getInt(0))
            }
        }
    }

    @Test
    fun migrate12To13_createsEmptyWaterTable() {
        helper.createDatabase(testDbName, 12).close()

        helper.runMigrationsAndValidate(testDbName, 13, true, Migrations.MIGRATION_12_13).use { db ->
            db.query("SELECT COUNT(*) FROM water_entries").use { cursor ->
                cursor.moveToFirst()
                assertEquals(0, cursor.getInt(0))
            }
        }
    }

    @Test
    fun migrate7To8_existingFoodsDefaultToNotEditedByUser() {
        helper.createDatabase(testDbName, 7).use { db ->
            db.execSQL(
                """
                INSERT INTO food_items
                    (name, caloriesPer100g, proteinPer100g, fatPer100g, carbsPer100g, source, createdAt)
                VALUES
                    ('White rice, cooked', 130.0, 2.7, 0.3, 28.0, 'SEED', 0)
                """.trimIndent(),
            )
        }

        helper.runMigrationsAndValidate(testDbName, 8, true, Migrations.MIGRATION_7_8).use { db ->
            db.query("SELECT editedByUser FROM food_items WHERE name = 'White rice, cooked'").use { cursor ->
                cursor.moveToFirst()
                assertEquals(0, cursor.getInt(0))
            }
        }
    }
}
