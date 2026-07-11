package app.diaita.data.export

import app.diaita.data.db.entities.DiaryEntry
import app.diaita.data.db.entities.FoodItem
import app.diaita.data.db.entities.FoodSource
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class DiaryCsvExporterTest {
    private val defaultLocale = Locale.getDefault()

    @After
    fun restoreLocale() {
        Locale.setDefault(defaultLocale)
    }

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

    private fun entry(
        foodItemId: Long = 1,
        presetName: String? = null,
    ) = DiaryEntry(
        id = 1,
        foodItemId = foodItemId,
        quantityGrams = 100.0,
        logDate = "2026-07-11",
        loggedAt = 0L,
        sourcePresetId = presetName?.let { 1L },
        sourcePresetName = presetName,
    )

    @Test
    fun `header plus one row`() {
        val csv = DiaryCsvExporter.toCsv(listOf(entry()), mapOf(1L to food))
        val lines = csv.lines()
        assertEquals(2, lines.size)
        assertEquals("date,time,food,quantity_g,calories,protein_g,fat_g,carbs_g,preset", lines[0])
        assertTrue(lines[1].startsWith("2026-07-11,"))
        assertTrue(lines[1].contains(",Rice,100.0,130.0,2.7,0.3,28.0,"))
    }

    @Test
    fun `fields with commas are quoted`() {
        val trickyFood = food.copy(name = "Rice, cooked \"plain\"")
        val csv = DiaryCsvExporter.toCsv(listOf(entry()), mapOf(1L to trickyFood))
        assertTrue(csv.contains("\"Rice, cooked \"\"plain\"\"\""))
    }

    @Test
    fun `missing food leaves macro cells empty`() {
        val csv = DiaryCsvExporter.toCsv(listOf(entry(foodItemId = 99)), mapOf(1L to food))
        assertTrue(csv.lines()[1].contains(",,,,,"))
    }

    @Test
    fun `preset name lands in the last column`() {
        val csv = DiaryCsvExporter.toCsv(listOf(entry(presetName = "My breakfast")), mapOf(1L to food))
        assertTrue(csv.lines()[1].endsWith(",My breakfast"))
    }

    @Test
    fun `decimal point survives comma-decimal default locale`() {
        Locale.setDefault(Locale.forLanguageTag("ru-RU"))
        val csv = DiaryCsvExporter.toCsv(listOf(entry()), mapOf(1L to food))
        assertTrue(csv.lines()[1].contains("130.0"))
    }
}
