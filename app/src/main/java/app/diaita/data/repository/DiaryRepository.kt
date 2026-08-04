package app.diaita.data.repository

import app.diaita.data.db.dao.DailyTotals
import app.diaita.data.db.dao.DayTotal
import app.diaita.data.db.dao.DiaryEntryDao
import app.diaita.data.db.dao.DiaryEntryWithFood
import app.diaita.data.db.entities.DiaryEntry
import app.diaita.data.db.entities.FoodItem
import app.diaita.data.db.entities.MealSlot
import app.diaita.util.DateUtils
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

private const val MILLIS_PER_DAY = 24L * 60 * 60 * 1000

class DiaryRepository(
    private val diaryEntryDao: DiaryEntryDao,
) {
    fun entriesForDate(date: String): Flow<List<DiaryEntryWithFood>> = diaryEntryDao.entriesForDate(date)

    fun dailyTotals(date: String): Flow<DailyTotals> = diaryEntryDao.dailyTotals(date)

    fun historyTotals(
        fromDate: String,
        toDate: String,
    ): Flow<List<DayTotal>> = diaryEntryDao.historyTotals(fromDate, toDate)

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

    /** Re-inserts a previously deleted entry as-is (same id) — used to support delete-undo. */
    suspend fun restoreEntry(entry: DiaryEntry) = diaryEntryDao.insert(entry)

    /**
     * Copies every entry of [fromDate] onto [toDate], preserving meal slots and preset
     * grouping snapshots. Each copy keeps its original time-of-day (shifted whole days),
     * so meal ordering and snack day-part classification survive the copy.
     * Returns how many entries were copied (0 = source day empty).
     */
    suspend fun copyDay(
        fromDate: String,
        toDate: String,
    ): Int {
        val entries = diaryEntryDao.entriesForDateOnce(fromDate)
        val dayShiftMillis =
            (LocalDate.parse(toDate).toEpochDay() - LocalDate.parse(fromDate).toEpochDay()) * MILLIS_PER_DAY
        diaryEntryDao.insertAll(
            entries.map { it.copy(id = 0, logDate = toDate, loggedAt = it.loggedAt + dayShiftMillis) },
        )
        return entries.size
    }

    suspend fun getAllEntries(): List<DiaryEntry> = diaryEntryDao.getAllEntries()
}
