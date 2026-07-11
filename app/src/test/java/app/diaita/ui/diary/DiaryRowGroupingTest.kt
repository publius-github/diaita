package app.diaita.ui.diary

import app.diaita.data.db.dao.DiaryEntryWithFood
import app.diaita.data.db.entities.DiaryEntry
import app.diaita.data.db.entities.FoodItem
import app.diaita.data.db.entities.FoodSource
import app.diaita.data.db.entities.MealSlot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class DiaryRowGroupingTest {
    private val food =
        FoodItem(
            id = 1,
            name = "Rice",
            caloriesPer100g = 130.0,
            proteinPer100g = 2.7,
            fatPer100g = 0.3,
            carbsPer100g = 28.0,
            source = FoodSource.SEED,
        )

    private var nextId = 1L

    private fun entry(
        slot: MealSlot = MealSlot.UNSPECIFIED,
        loggedAt: Long = 1000L,
        presetId: Long? = null,
        presetName: String? = null,
        grams: Double = 100.0,
    ) = DiaryEntryWithFood(
        entry =
            DiaryEntry(
                id = nextId++,
                foodItemId = food.id,
                quantityGrams = grams,
                logDate = "2026-07-11",
                loggedAt = loggedAt,
                mealSlot = slot,
                sourcePresetId = presetId,
                sourcePresetName = presetName,
            ),
        food = food,
    )

    private fun atHour(hour: Int): Long =
        LocalDateTime
            .of(2026, 7, 11, hour, 0)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

    @Test
    fun `entries from one preset log collapse into a single row`() {
        val entries =
            listOf(
                entry(presetId = 5, presetName = "Breakfast combo", loggedAt = 1000L),
                entry(presetId = 5, presetName = "Breakfast combo", loggedAt = 1000L),
                entry(loggedAt = 2000L),
            )
        val rows = groupEntriesIntoRows(entries)
        assertEquals(2, rows.size)
        val presetRow = rows[0] as DiaryRow.Preset
        assertEquals("Breakfast combo", presetRow.presetName)
        assertEquals(2, presetRow.entries.size)
        assertTrue(rows[1] is DiaryRow.Food)
    }

    @Test
    fun `same preset logged twice stays two rows`() {
        val entries =
            listOf(
                entry(presetId = 5, presetName = "Combo", loggedAt = 1000L),
                entry(presetId = 5, presetName = "Combo", loggedAt = 2000L),
            )
        assertEquals(2, groupEntriesIntoRows(entries).size)
    }

    @Test
    fun `sections follow meal order and drop empty slots`() {
        val entries =
            listOf(
                entry(slot = MealSlot.DINNER),
                entry(slot = MealSlot.BREAKFAST),
            )
        val sections = buildDiarySections(entries)
        assertEquals(listOf(MealSlot.BREAKFAST, MealSlot.DINNER), sections.map { it.slot })
    }

    @Test
    fun `explicit slots map directly to day parts`() {
        assertEquals(DayPart.MORNING, dayPartOf(entry(slot = MealSlot.BREAKFAST).entry))
        assertEquals(DayPart.DAY, dayPartOf(entry(slot = MealSlot.LUNCH).entry))
        assertEquals(DayPart.EVENING, dayPartOf(entry(slot = MealSlot.DINNER).entry))
    }

    @Test
    fun `snacks fall back to wall clock hour`() {
        assertEquals(DayPart.MORNING, dayPartOf(entry(slot = MealSlot.SNACK, loggedAt = atHour(9)).entry))
        assertEquals(DayPart.DAY, dayPartOf(entry(slot = MealSlot.SNACK, loggedAt = atHour(14)).entry))
        assertEquals(DayPart.EVENING, dayPartOf(entry(slot = MealSlot.SNACK, loggedAt = atHour(21)).entry))
    }

    @Test
    fun `day part calories sum per bucket`() {
        val entries =
            listOf(
                entry(slot = MealSlot.BREAKFAST, grams = 100.0), // 130 kcal
                entry(slot = MealSlot.BREAKFAST, grams = 50.0), // 65 kcal
                entry(slot = MealSlot.DINNER, grams = 200.0), // 260 kcal
            )
        val byPart = buildDayPartCalories(entries)
        assertEquals(195.0, byPart.getValue(DayPart.MORNING), 1e-9)
        assertEquals(260.0, byPart.getValue(DayPart.EVENING), 1e-9)
    }
}
