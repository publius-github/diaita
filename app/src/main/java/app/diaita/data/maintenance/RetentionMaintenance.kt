package app.diaita.data.maintenance

import app.diaita.data.db.dao.DiaryEntryDao
import app.diaita.data.db.dao.MonthlyAggregateDao
import app.diaita.data.db.entities.MonthlyAggregate
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Retention policy, run as a cheap idempotent check on every app start:
 * - raw [app.diaita.data.db.entities.DiaryEntry] rows are kept for
 *   [RAW_RETENTION_DAYS] (a full year, so "what did I eat back then" keeps working);
 * - anything older gets folded into per-month averages ([MonthlyAggregate]) and the raw
 *   rows deleted. Aggregates are kept indefinitely — they are a handful of rows per year.
 *
 * Merging uses the stored daysCount so folding more days of an already-aggregated month
 * produces a correct weighted average.
 */
class RetentionMaintenance(
    private val diaryEntryDao: DiaryEntryDao,
    private val aggregateDao: MonthlyAggregateDao,
    private val today: LocalDate = LocalDate.now(),
) {
    suspend fun run() {
        val cutoffDate =
            today
                .minusDays(RAW_RETENTION_DAYS - 1L)
                .format(DateTimeFormatter.ISO_LOCAL_DATE)

        val oldDays = diaryEntryDao.dailyTotalsBefore(cutoffDate)
        if (oldDays.isEmpty()) return

        oldDays.groupBy { it.logDate.substring(0, 7) }.forEach { (yearMonth, days) ->
            val existing = aggregateDao.get(yearMonth)
            val merged =
                if (existing == null) {
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
                        avgCalories =
                            (existing.avgCalories * existing.daysCount + days.sumOf { it.totalCalories }) / totalDays,
                        avgProtein =
                            (
                                existing.avgProtein * existing.daysCount +
                                    days.sumOf {
                                        it.totalProtein
                                    }
                            ) / totalDays,
                        avgFat = (existing.avgFat * existing.daysCount + days.sumOf { it.totalFat }) / totalDays,
                        avgCarbs = (existing.avgCarbs * existing.daysCount + days.sumOf { it.totalCarbs }) / totalDays,
                        daysCount = totalDays,
                    )
                }
            aggregateDao.upsert(merged)
        }
        diaryEntryDao.deleteBefore(cutoffDate)
    }

    companion object {
        const val RAW_RETENTION_DAYS = 365
    }
}
