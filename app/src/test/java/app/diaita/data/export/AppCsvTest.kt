package app.diaita.data.export

import app.diaita.data.db.entities.DiaryEntry
import app.diaita.data.db.entities.FoodItem
import app.diaita.data.db.entities.FoodSource
import app.diaita.data.db.entities.MealPreset
import app.diaita.data.db.entities.MealPresetItem
import app.diaita.data.db.entities.MealSlot
import app.diaita.data.db.entities.WeightEntry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class AppCsvTest {
    private val defaultLocale = Locale.getDefault()

    @After
    fun restoreLocale() {
        Locale.setDefault(defaultLocale)
    }

    private val rice =
        FoodItem(
            id = 1,
            name = "Rice, cooked \"plain\"",
            brand = "Acme, Inc",
            caloriesPer100g = 130.0,
            proteinPer100g = 2.7,
            fatPer100g = 0.3,
            carbsPer100g = 28.0,
            fiberPer100g = 0.4,
            source = FoodSource.SEED,
            seedKey = "white_rice_cooked",
        )
    private val evilFood =
        FoodItem(
            id = 2,
            name = "=HYPERLINK(\"http://evil\")",
            caloriesPer100g = 100.0,
            proteinPer100g = 0.0,
            fatPer100g = 0.0,
            carbsPer100g = 0.0,
            source = FoodSource.MANUAL,
        )
    private val entry =
        DiaryEntry(
            id = 10,
            foodItemId = 1,
            quantityGrams = 150.0,
            logDate = "2026-08-01",
            loggedAt = 1_754_000_000_000,
            mealSlot = MealSlot.LUNCH,
        )
    private val weight = WeightEntry(date = "2026-08-01", weightKg = 79.5)
    private val preset = MealPreset(id = 5, name = "My breakfast", mealSlot = MealSlot.BREAKFAST)
    private val presetItems = listOf(MealPresetItem(id = 1, presetId = 5, foodItemId = 1, quantityGrams = 60.0))

    private fun exportAll(): String =
        AppCsv.export(
            foods = listOf(rice, evilFood),
            entries = listOf(entry),
            weights = listOf(weight),
            presets = listOf(preset to presetItems),
        )

    @Test
    fun `export starts with BOM and format header`() {
        val csv = exportAll()
        assertTrue(csv.startsWith(AppCsv.BOM))
        assertTrue(csv.removePrefix(AppCsv.BOM).startsWith(AppCsv.FORMAT_HEADER))
    }

    @Test
    fun `formula-looking names are neutralized`() {
        val csv = exportAll()
        assertTrue(csv.contains("'=HYPERLINK"))
    }

    @Test
    fun `round trip preserves foods diary weights and presets`() {
        val result = AppCsv.parse(exportAll())
        val parsed = (result as AppCsv.ParseResult.Success).parsed

        assertEquals(2, parsed.foods.size)
        val parsedRice = parsed.foods.first { it.ref == 1L }.food
        assertEquals(rice.name, parsedRice.name)
        assertEquals(rice.brand, parsedRice.brand)
        assertEquals(rice.fiberPer100g!!, parsedRice.fiberPer100g!!, 1e-9)
        assertEquals(rice.seedKey, parsedRice.seedKey)
        // The formula guard is stripped back off on import.
        assertEquals(
            evilFood.name,
            parsed.foods
                .first { it.ref == 2L }
                .food.name,
        )

        assertEquals(1, parsed.diary.size)
        val parsedEntry = parsed.diary.single()
        assertEquals(entry.logDate, parsedEntry.date)
        assertEquals(entry.quantityGrams, parsedEntry.quantityGrams, 1e-9)
        assertEquals(MealSlot.LUNCH, parsedEntry.mealSlot)
        assertEquals(1L, parsedEntry.foodRef)

        // loggedAt defaults to "now" on both sides, so compare the persisted fields only.
        assertEquals(1, parsed.weights.size)
        assertEquals(weight.date, parsed.weights.single().date)
        assertEquals(weight.weightKg, parsed.weights.single().weightKg, 1e-9)

        assertEquals(1, parsed.presets.size)
        val parsedPreset = parsed.presets.single()
        assertEquals(preset.name, parsedPreset.name)
        assertEquals(MealSlot.BREAKFAST, parsedPreset.mealSlot)
        assertEquals(listOf(1L to 60.0), parsedPreset.items)
    }

    @Test
    fun `decimal point survives comma-decimal default locale`() {
        Locale.setDefault(Locale.forLanguageTag("ru-RU"))
        val csv = exportAll()
        assertTrue(csv.contains("130,")) // column separator right after the int-normalized 130
        assertTrue(csv.contains("79.5"))
    }

    @Test
    fun `foreign files are rejected`() {
        assertTrue(AppCsv.parse("date,food\n2026-01-01,x") is AppCsv.ParseResult.NotADiaitaExport)
    }

    @Test
    fun `quoted fields with commas survive`() {
        assertEquals(
            listOf("a", "b,c", "d\"e"),
            AppCsv.parseCsvLine("a,\"b,c\",\"d\"\"e\""),
        )
    }
}
