package app.diaita.data.maintenance

import app.diaita.data.db.AppDatabase
import app.diaita.data.db.entities.MonthlyAggregate
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

/**
 * Retention policy, run as a cheap idempotent check on every app start:
 * - raw [app.diaita.data.db.entities.DiaryEntry] rows are kept for [RAW_RETENTION_DAYS];
 * - anything older gets folded into per-month averages ([MonthlyAggregate]) and the raw rows deleted;
 * - monthly aggregates older than [AGGREGATE_RETENTION_MONTHS] are dropped.
 *
 * Merging uses the stored daysCount so folding more days of an already-aggregated month
 * produces a correct weighted average.
 */
class RetentionMaintenance(private val database: AppDatabase) {

    suspend fun run() {
        val diaryEntryDao = database.diaryEntryDao()
        val aggregateDao = database.monthlyAggregateDao()

        val cutoffDate = LocalDate.now()
            .minusDays(RAW_RETENTION_DAYS - 1L)
            .format(DateTimeFormatter.ISO_LOCAL_DATE)

        val oldDays = diaryEntryDao.dailyTotalsBefore(cutoffDate)
        if (oldDays.isNotEmpty()) {
            oldDays.groupBy { it.logDate.substring(0, 7) }.forEach { (yearMonth, days) ->
                val existing = aggregateDao.get(yearMonth)
                val merged = if (existing == null) {
                    MonthlyAggregate(
                        yearMonth = yearMonth,
                        avgCalories = days.map { it.totalCalories }.average(),
                        avgProtein = days.map { it.totalProtein }.average(),
                        avgFat = days.map { it.totalFat }.average(),
                        avgCarbs = days.map { it.totalCarbs }.average(),
                        daysCount = days.size,
                    )
                } else {
                    val totalDays = existing.daysCount + days.size
                    MonthlyAggregate(
                        yearMonth = yearMonth,
                        avgCalories = (existing.avgCalories * existing.daysCount + days.sumOf { it.totalCalories }) / totalDays,
                        avgProtein = (existing.avgProtein * existing.daysCount + days.sumOf { it.totalProtein }) / totalDays,
                        avgFat = (existing.avgFat * existing.daysCount + days.sumOf { it.totalFat }) / totalDays,
                        avgCarbs = (existing.avgCarbs * existing.daysCount + days.sumOf { it.totalCarbs }) / totalDays,
                        daysCount = totalDays,
                    )
                }
                aggregateDao.upsert(merged)
            }
            diaryEntryDao.deleteBefore(cutoffDate)
        }

        aggregateDao.deleteBefore(YearMonth.now().minusMonths(AGGREGATE_RETENTION_MONTHS.toLong()).toString())
    }

    companion object {
        const val RAW_RETENTION_DAYS = 30
        const val AGGREGATE_RETENTION_MONTHS = 12
    }
}
