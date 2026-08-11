package app.diaita.data.repository

import androidx.room.withTransaction
import app.diaita.data.db.AppDatabase
import app.diaita.data.db.dao.DailyTotals
import app.diaita.data.db.dao.DayTotal
import app.diaita.data.db.dao.DiaryEntryDao
import app.diaita.data.db.dao.DiaryEntryWithFood
import app.diaita.data.db.entities.DiaryEntry
import app.diaita.data.db.entities.FoodItem
import app.diaita.data.db.entities.MealSlot
import app.diaita.data.db.entities.MonthlyAggregate
import app.diaita.util.DateUtils
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.format.DateTimeParseException

private const val MILLIS_PER_DAY = 24L * 60 * 60 * 1000

class DiaryRepository(
    private val database: AppDatabase,
    private val diaryEntryDao: DiaryEntryDao,
) {
    fun entriesForDate(date: String): Flow<List<DiaryEntryWithFood>> = diaryEntryDao.entriesForDate(date)

    suspend fun hasAnyEntries(): Boolean = diaryEntryDao.hasAny()

    suspend fun entriesForDateOnce(date: String): List<DiaryEntry> = diaryEntryDao.entriesForDateOnce(date)

    fun dailyTotals(date: String): Flow<DailyTotals> = diaryEntryDao.dailyTotals(date)

    fun historyTotals(
        fromDate: String,
        toDate: String,
    ): Flow<List<DayTotal>> = diaryEntryDao.historyTotals(fromDate, toDate)

    fun monthlyAverages(): Flow<List<MonthlyAggregate>> = database.monthlyAggregateDao().getAll()

    fun recentFoods(limit: Int): Flow<List<FoodItem>> = diaryEntryDao.recentFoods(limit)

    suspend fun lastLoggedQuantity(foodItemId: Long): Double? = diaryEntryDao.lastLoggedQuantity(foodItemId)

    suspend fun logFood(
        foodItemId: Long,
        quantityGrams: Double,
        date: String,
        mealSlot: MealSlot = MealSlot.UNSPECIFIED,
    ) {
        diaryEntryDao.insert(
            DiaryEntry(
                foodItemId = foodItemId,
                quantityGrams = quantityGrams,
                logDate = date,
                loggedAt = DateUtils.loggedAtFor(date),
                mealSlot = mealSlot,
            ),
        )
    }

    suspend fun deleteEntry(entry: DiaryEntry) = diaryEntryDao.delete(entry)

    suspend fun updateEntry(entry: DiaryEntry) = diaryEntryDao.update(entry)

    /**
     * Re-inserts a previously deleted entry as-is (same id) — used to support delete-undo.
     * Insert-or-ignore keeps a double "Undo" tap from throwing on the reused primary key.
     */
    suspend fun restoreEntry(entry: DiaryEntry) = diaryEntryDao.insertOrIgnore(entry)

    /**
     * Copies every entry of [fromDate] onto [toDate], preserving meal slots and preset
     * grouping snapshots. Each copy keeps its original time-of-day (shifted whole days),
     * so meal ordering and snack day-part classification survive the copy.
     *
     * Runs read + insert in one transaction and skips entries that already exist
     * identically on the target day, so a double-tap cannot silently duplicate the day.
     * Returns how many entries were copied (0 = source empty, same-day copy, or bad date).
     */
    suspend fun copyDay(
        fromDate: String,
        toDate: String,
    ): Int = copyEntries(fromDate, toDate, slot = null)

    /** Same as [copyDay] but restricted to one meal [slot] of the source day. */
    suspend fun copyMeal(
        fromDate: String,
        toDate: String,
        slot: MealSlot,
    ): Int = copyEntries(fromDate, toDate, slot)

    private suspend fun copyEntries(
        fromDate: String,
        toDate: String,
        slot: MealSlot?,
    ): Int {
        if (fromDate == toDate) return 0
        val dayShiftMillis =
            try {
                (LocalDate.parse(toDate).toEpochDay() - LocalDate.parse(fromDate).toEpochDay()) * MILLIS_PER_DAY
            } catch (e: DateTimeParseException) {
                return 0
            }
        return database.withTransaction {
            val source =
                diaryEntryDao
                    .entriesForDateOnce(fromDate)
                    .filter { slot == null || it.mealSlot == slot }
            val existing = diaryEntryDao.entriesForDateOnce(toDate)
            val copies =
                source
                    .map { it.copy(id = 0, logDate = toDate, loggedAt = it.loggedAt + dayShiftMillis) }
                    .filterNot { copy ->
                        existing.any { it.matchesCopy(copy) }
                    }
            diaryEntryDao.insertAll(copies)
            copies.size
        }
    }

    private fun DiaryEntry.matchesCopy(copy: DiaryEntry): Boolean =
        foodItemId == copy.foodItemId &&
            quantityGrams == copy.quantityGrams &&
            loggedAt == copy.loggedAt &&
            mealSlot == copy.mealSlot &&
            sourcePresetId == copy.sourcePresetId

    suspend fun getAllEntries(): List<DiaryEntry> = diaryEntryDao.getAllEntries()
}
