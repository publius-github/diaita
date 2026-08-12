package app.diaita.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import androidx.room.Update
import app.diaita.data.db.entities.DiaryEntry
import app.diaita.data.db.entities.FoodItem
import kotlinx.coroutines.flow.Flow

data class DiaryEntryWithFood(
    @Embedded val entry: DiaryEntry,
    @Relation(parentColumn = "foodItemId", entityColumn = "id")
    val food: FoodItem,
)

data class DailyTotals(
    val totalCalories: Double,
    val totalProtein: Double,
    val totalFat: Double,
    val totalCarbs: Double,
    /**
     * Null when nothing logged that day carries the nutrient — the catalog columns are
     * optional, so "absent" must stay distinguishable from a summed 0.
     */
    val totalFiber: Double? = null,
    val totalSugar: Double? = null,
    val totalSalt: Double? = null,
)

data class DayTotal(
    val logDate: String,
    val totalCalories: Double,
)

data class DailyMacroTotals(
    val logDate: String,
    val totalCalories: Double,
    val totalProtein: Double,
    val totalFat: Double,
    val totalCarbs: Double,
)

@Dao
interface DiaryEntryDao {
    @Insert
    suspend fun insert(entry: DiaryEntry): Long

    /** Undo-restore path: re-inserting the same primary key twice must be a no-op, not a crash. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertOrIgnore(entry: DiaryEntry): Long

    @Transaction
    suspend fun insertAll(entries: List<DiaryEntry>) {
        entries.forEach { insert(it) }
    }

    @Delete
    suspend fun delete(entry: DiaryEntry)

    @Update
    suspend fun update(entry: DiaryEntry)

    @Query("SELECT * FROM diary_entries ORDER BY logDate ASC, loggedAt ASC")
    suspend fun getAllEntries(): List<DiaryEntry>

    @Query("SELECT EXISTS(SELECT 1 FROM diary_entries)")
    suspend fun hasAny(): Boolean

    @Transaction
    @Query("SELECT * FROM diary_entries WHERE logDate = :date ORDER BY loggedAt ASC")
    fun entriesForDate(date: String): Flow<List<DiaryEntryWithFood>>

    @Query("SELECT * FROM diary_entries WHERE logDate = :date ORDER BY loggedAt ASC")
    suspend fun entriesForDateOnce(date: String): List<DiaryEntry>

    @Query(
        """
        SELECT
            COALESCE(SUM(f.caloriesPer100g * d.quantityGrams / 100.0), 0) AS totalCalories,
            COALESCE(SUM(f.proteinPer100g * d.quantityGrams / 100.0), 0) AS totalProtein,
            COALESCE(SUM(f.fatPer100g * d.quantityGrams / 100.0), 0) AS totalFat,
            COALESCE(SUM(f.carbsPer100g * d.quantityGrams / 100.0), 0) AS totalCarbs,
            SUM(f.fiberPer100g * d.quantityGrams / 100.0) AS totalFiber,
            SUM(f.sugarPer100g * d.quantityGrams / 100.0) AS totalSugar,
            SUM(f.saltPer100g * d.quantityGrams / 100.0) AS totalSalt
        FROM diary_entries d
        JOIN food_items f ON f.id = d.foodItemId
        WHERE d.logDate = :date
        """,
    )
    fun dailyTotals(date: String): Flow<DailyTotals>

    @Query(
        """
        SELECT d.logDate AS logDate,
               COALESCE(SUM(f.caloriesPer100g * d.quantityGrams / 100.0), 0) AS totalCalories
        FROM diary_entries d
        JOIN food_items f ON f.id = d.foodItemId
        WHERE d.logDate BETWEEN :fromDate AND :toDate
        GROUP BY d.logDate
        ORDER BY d.logDate ASC
        """,
    )
    fun historyTotals(
        fromDate: String,
        toDate: String,
    ): Flow<List<DayTotal>>

    /** Most recently logged distinct foods, for the quick-access row on the Add Food screen. */
    @Query(
        """
        SELECT f.* FROM food_items f
        JOIN (SELECT foodItemId, MAX(loggedAt) AS lastLogged FROM diary_entries GROUP BY foodItemId) recent
            ON f.id = recent.foodItemId
        WHERE f.source != 'QUICK'
        ORDER BY recent.lastLogged DESC
        LIMIT :limit
        """,
    )
    fun recentFoods(limit: Int): Flow<List<FoodItem>>

    /** The grams this food was logged with most recently — the best guess for the next portion. */
    @Query("SELECT quantityGrams FROM diary_entries WHERE foodItemId = :foodItemId ORDER BY loggedAt DESC LIMIT 1")
    suspend fun lastLoggedQuantity(foodItemId: Long): Double?

    /** Per-day macro totals for all days strictly before [beforeDate] — used by the retention job. */
    @Query(
        """
        SELECT d.logDate AS logDate,
               COALESCE(SUM(f.caloriesPer100g * d.quantityGrams / 100.0), 0) AS totalCalories,
               COALESCE(SUM(f.proteinPer100g * d.quantityGrams / 100.0), 0) AS totalProtein,
               COALESCE(SUM(f.fatPer100g * d.quantityGrams / 100.0), 0) AS totalFat,
               COALESCE(SUM(f.carbsPer100g * d.quantityGrams / 100.0), 0) AS totalCarbs
        FROM diary_entries d
        JOIN food_items f ON f.id = d.foodItemId
        WHERE d.logDate < :beforeDate
        GROUP BY d.logDate
        """,
    )
    suspend fun dailyTotalsBefore(beforeDate: String): List<DailyMacroTotals>

    @Query("DELETE FROM diary_entries WHERE logDate < :beforeDate")
    suspend fun deleteBefore(beforeDate: String)
}
