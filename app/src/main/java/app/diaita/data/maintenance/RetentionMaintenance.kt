package app.diaita.data.maintenance

import app.diaita.data.db.dao.DiaryEntryDao
import app.diaita.data.db.dao.FoodItemDao
import app.diaita.data.db.dao.MonthlyAggregateDao
import app.diaita.data.db.entities.MonthlyAggregate
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Retention policy, run as a cheap idempotent check on every app start:
 * - raw [app.diaita.data.db.entities.DiaryEntry] rows are kept for
 *   [retentionDays] (user-configurable; a non-positive value —
 *   [app.diaita.data.settings.SettingsRepository.RETENTION_FOREVER], the default —
 *   disables folding entirely so full history is kept);
 * - anything older gets folded into per-month averages ([MonthlyAggregate]) and the raw
 *   rows deleted. Aggregates are kept indefinitely — they are a handful of rows per year.
 * - orphaned catalog rows are pruned: QUICK one-offs nothing references any more, and
 *   stale never-logged OpenFoodFacts cache rows (kept for [OFF_CACHE_ORPHAN_DAYS]).
 *
 * Merging uses the stored daysCount so folding more days of an already-aggregated month
 * produces a correct weighted average.
 *
 * The fold (aggregate upsert + raw-row delete) runs inside [transactionRunner] so process
 * death between the two can never re-fold the same days into inflated averages. Production
 * passes Room's `withTransaction`; unit tests may keep the pass-through default.
 */
class RetentionMaintenance(
    private val diaryEntryDao: DiaryEntryDao,
    private val aggregateDao: MonthlyAggregateDao,
    private val today: LocalDate = LocalDate.now(),
    private val retentionDays: Int = RAW_RETENTION_DAYS,
    private val foodItemDao: FoodItemDao? = null,
    private val transactionRunner: suspend (suspend () -> Unit) -> Unit = { it() },
) {
    suspend fun run() {
        if (retentionDays > 0) {
            val cutoffDate =
                today
                    .minusDays(retentionDays - 1L)
                    .format(DateTimeFormatter.ISO_LOCAL_DATE)

            val oldDays = diaryEntryDao.dailyTotalsBefore(cutoffDate)
            if (oldDays.isNotEmpty()) {
                transactionRunner {
                    foldIntoAggregates(oldDays.groupBy { it.logDate.substring(0, 7) })
                    diaryEntryDao.deleteBefore(cutoffDate)
                }
            }
        }

        foodItemDao?.let { dao ->
            dao.pruneOrphanQuickFoods()
            val offCutoff =
                today
                    .minusDays(OFF_CACHE_ORPHAN_DAYS.toLong())
                    .atStartOfDay(java.time.ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli()
            dao.pruneStaleOffFoods(offCutoff)
        }
    }

    private suspend fun foldIntoAggregates(daysByMonth: Map<String, List<app.diaita.data.db.dao.DailyMacroTotals>>) {
        daysByMonth.forEach { (yearMonth, days) ->
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
    }

    companion object {
        const val RAW_RETENTION_DAYS = 365

        /** Never-logged, never-touched OFF cache rows older than this get pruned. */
        const val OFF_CACHE_ORPHAN_DAYS = 90
    }
}
