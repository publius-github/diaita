package app.diaita.data.maintenance

import app.diaita.data.db.dao.DailyMacroTotals
import app.diaita.data.db.dao.DailyTotals
import app.diaita.data.db.dao.DayTotal
import app.diaita.data.db.dao.DiaryEntryDao
import app.diaita.data.db.dao.DiaryEntryWithFood
import app.diaita.data.db.dao.MonthlyAggregateDao
import app.diaita.data.db.entities.DiaryEntry
import app.diaita.data.db.entities.FoodItem
import app.diaita.data.db.entities.MonthlyAggregate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

private class FakeDiaryEntryDao(
    private val oldDayTotals: List<DailyMacroTotals>,
) : DiaryEntryDao {
    val deleteBeforeCalls = mutableListOf<String>()

    override suspend fun dailyTotalsBefore(beforeDate: String): List<DailyMacroTotals> = oldDayTotals

    override suspend fun deleteBefore(beforeDate: String) {
        deleteBeforeCalls += beforeDate
    }

    override suspend fun insert(entry: DiaryEntry): Long = 0

    override suspend fun insertOrIgnore(entry: DiaryEntry): Long = 0

    override suspend fun delete(entry: DiaryEntry) = Unit

    override suspend fun update(entry: DiaryEntry) = Unit

    override suspend fun getAllEntries(): List<DiaryEntry> = emptyList()

    override suspend fun hasAny(): Boolean = false

    override fun entriesForDate(date: String): Flow<List<DiaryEntryWithFood>> = flowOf(emptyList())

    override suspend fun entriesForDateOnce(date: String): List<DiaryEntry> = emptyList()

    override fun dailyTotals(date: String): Flow<DailyTotals> = flowOf(DailyTotals(0.0, 0.0, 0.0, 0.0))

    override fun historyTotals(
        fromDate: String,
        toDate: String,
    ): Flow<List<DayTotal>> = flowOf(emptyList())

    override fun recentFoods(limit: Int): Flow<List<FoodItem>> = flowOf(emptyList())

    override suspend fun lastLoggedQuantity(foodItemId: Long): Double? = null
}

private class FakeMonthlyAggregateDao(
    initial: List<MonthlyAggregate> = emptyList(),
) : MonthlyAggregateDao {
    val stored = initial.associateBy { it.yearMonth }.toMutableMap()

    override suspend fun upsert(aggregate: MonthlyAggregate) {
        stored[aggregate.yearMonth] = aggregate
    }

    override suspend fun get(yearMonth: String): MonthlyAggregate? = stored[yearMonth]

    override fun getAll(): Flow<List<MonthlyAggregate>> = flowOf(stored.values.toList())
}

class RetentionMaintenanceTest {
    private val today = LocalDate.of(2026, 7, 11)

    private fun day(
        date: String,
        calories: Double,
    ) = DailyMacroTotals(date, calories, calories / 20, calories / 30, calories / 10)

    @Test
    fun `no old days means no writes and no deletes`() =
        runTest {
            val diaryDao = FakeDiaryEntryDao(emptyList())
            val aggregateDao = FakeMonthlyAggregateDao()

            RetentionMaintenance(diaryDao, aggregateDao, today).run()

            assertTrue(aggregateDao.stored.isEmpty())
            assertTrue(diaryDao.deleteBeforeCalls.isEmpty())
        }

    @Test
    fun `old days fold into a fresh monthly average and raws get deleted`() =
        runTest {
            val diaryDao =
                FakeDiaryEntryDao(
                    listOf(day("2025-05-01", 1800.0), day("2025-05-02", 2200.0)),
                )
            val aggregateDao = FakeMonthlyAggregateDao()

            RetentionMaintenance(diaryDao, aggregateDao, today).run()

            val aggregate = aggregateDao.stored.getValue("2025-05")
            assertEquals(2000.0, aggregate.avgCalories, 1e-9)
            assertEquals(2, aggregate.daysCount)
            // cutoff = today - (365 - 1) days
            assertEquals(listOf("2025-07-12"), diaryDao.deleteBeforeCalls)
        }

    @Test
    fun `merging into an existing aggregate is weighted by days count`() =
        runTest {
            val diaryDao = FakeDiaryEntryDao(listOf(day("2025-05-10", 1400.0)))
            val aggregateDao =
                FakeMonthlyAggregateDao(
                    listOf(
                        MonthlyAggregate(
                            yearMonth = "2025-05",
                            avgCalories = 2000.0,
                            avgProtein = 100.0,
                            avgFat = 66.0,
                            avgCarbs = 200.0,
                            daysCount = 2,
                        ),
                    ),
                )

            RetentionMaintenance(diaryDao, aggregateDao, today).run()

            val merged = aggregateDao.stored.getValue("2025-05")
            // (2000*2 + 1400) / 3 = 1800
            assertEquals(1800.0, merged.avgCalories, 1e-9)
            assertEquals(3, merged.daysCount)
        }

    @Test
    fun `fold and raw delete run inside the provided transaction`() =
        runTest {
            val diaryDao = FakeDiaryEntryDao(listOf(day("2025-05-01", 2000.0)))
            val aggregateDao = FakeMonthlyAggregateDao()
            var transactions = 0

            RetentionMaintenance(
                diaryDao,
                aggregateDao,
                today,
                transactionRunner = { block ->
                    transactions++
                    block()
                },
            ).run()

            assertEquals(1, transactions)
            assertEquals(1, aggregateDao.stored.size)
            assertEquals(1, diaryDao.deleteBeforeCalls.size)
        }

    @Test
    fun `forever retention folds and deletes nothing even with old days present`() =
        runTest {
            val diaryDao = FakeDiaryEntryDao(listOf(day("2020-01-01", 2000.0)))
            val aggregateDao = FakeMonthlyAggregateDao()

            RetentionMaintenance(diaryDao, aggregateDao, today, retentionDays = 0).run()

            assertTrue(aggregateDao.stored.isEmpty())
            assertTrue(diaryDao.deleteBeforeCalls.isEmpty())
        }

    @Test
    fun `days from different months fold into separate aggregates`() =
        runTest {
            val diaryDao =
                FakeDiaryEntryDao(
                    listOf(day("2025-05-01", 2000.0), day("2025-06-01", 1000.0)),
                )
            val aggregateDao = FakeMonthlyAggregateDao()

            RetentionMaintenance(diaryDao, aggregateDao, today).run()

            assertEquals(setOf("2025-05", "2025-06"), aggregateDao.stored.keys)
            assertEquals(2000.0, aggregateDao.stored.getValue("2025-05").avgCalories, 1e-9)
            assertEquals(1000.0, aggregateDao.stored.getValue("2025-06").avgCalories, 1e-9)
        }
}
