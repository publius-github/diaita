package app.diaita.data.export

import app.diaita.data.db.entities.DiaryEntry
import app.diaita.data.db.entities.FoodItem
import app.diaita.data.db.entities.FoodSource
import app.diaita.data.db.entities.MealPreset
import app.diaita.data.db.entities.MealPresetItem
import app.diaita.data.db.entities.MealSlot
import app.diaita.data.db.entities.WeightEntry
import app.diaita.util.DateUtils
import app.diaita.util.NutrientMath
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeParseException
import java.util.Locale

/**
 * Full-fidelity CSV export/import. One file, four sections ([foods], [diary], [weights],
 * [presets]) so nothing about the user's data is unrecoverable from the export alone —
 * including manual catalog foods, brands, meal slots, presets and weight history.
 *
 * Excel-safety: the file starts with a UTF-8 BOM (so Cyrillic names open correctly) and
 * text cells that could be interpreted as formulas (leading `=`, `+`, `-`, `@`) are
 * prefixed with a single quote, which the importer strips back off.
 */
object AppCsv {
    const val FORMAT_HEADER = "# Diaita export 2"
    const val BOM = "\uFEFF"

    const val SECTION_FOODS = "[foods]"
    const val SECTION_DIARY = "[diary]"
    const val SECTION_WEIGHTS = "[weights]"
    const val SECTION_PRESETS = "[presets]"

    val FOODS_HEADER =
        listOf(
            "ref",
            "seed_key",
            "name",
            "brand",
            "barcode",
            "kcal_100g",
            "protein_100g",
            "fat_100g",
            "carbs_100g",
            "fiber_100g",
            "sugar_100g",
            "salt_100g",
            "serving_g",
            "g_per_ml",
            "g_per_piece",
            "source",
            "off_id",
            "favorite",
        )
    val DIARY_HEADER =
        listOf(
            "date",
            "time",
            "food_ref",
            "food_name",
            "quantity_g",
            "meal_slot",
            "calories",
            "protein_g",
            "fat_g",
            "carbs_g",
            "preset_id",
            "preset_name",
        )
    val WEIGHTS_HEADER = listOf("date", "weight_kg")
    val PRESETS_HEADER = listOf("preset_ref", "name", "meal_slot", "food_ref", "quantity_g")

    // --- Export ---

    fun export(
        foods: List<FoodItem>,
        entries: List<DiaryEntry>,
        weights: List<WeightEntry>,
        presets: List<Pair<MealPreset, List<MealPresetItem>>>,
    ): String {
        val foodsById = foods.associateBy { it.id }
        val lines = mutableListOf(FORMAT_HEADER)

        lines += SECTION_FOODS
        lines += FOODS_HEADER.joinToString(",")
        foods.forEach { food ->
            lines +=
                row(
                    food.id.toString(),
                    text(food.seedKey),
                    text(food.name),
                    text(food.brand),
                    text(food.barcode),
                    num(food.caloriesPer100g),
                    num(food.proteinPer100g),
                    num(food.fatPer100g),
                    num(food.carbsPer100g),
                    optNum(food.fiberPer100g),
                    optNum(food.sugarPer100g),
                    optNum(food.saltPer100g),
                    optNum(food.defaultServingGrams),
                    optNum(food.gramsPerMilliliter),
                    optNum(food.gramsPerPiece),
                    food.source.name,
                    text(food.offId),
                    if (food.isFavorite) "1" else "0",
                )
        }

        lines += SECTION_DIARY
        lines += DIARY_HEADER.joinToString(",")
        entries.forEach { entry ->
            val food = foodsById[entry.foodItemId]
            lines +=
                row(
                    entry.logDate,
                    DateUtils.formatTime(entry.loggedAt),
                    entry.foodItemId.toString(),
                    text(food?.name),
                    num(entry.quantityGrams),
                    entry.mealSlot.name,
                    food?.let { num(NutrientMath.caloriesFor(it, entry.quantityGrams)) } ?: "",
                    food?.let { num(NutrientMath.proteinFor(it, entry.quantityGrams)) } ?: "",
                    food?.let { num(NutrientMath.fatFor(it, entry.quantityGrams)) } ?: "",
                    food?.let { num(NutrientMath.carbsFor(it, entry.quantityGrams)) } ?: "",
                    entry.sourcePresetId?.toString() ?: "",
                    text(entry.sourcePresetName),
                )
        }

        lines += SECTION_WEIGHTS
        lines += WEIGHTS_HEADER.joinToString(",")
        weights.forEach { weight ->
            lines += row(weight.date, num(weight.weightKg))
        }

        lines += SECTION_PRESETS
        lines += PRESETS_HEADER.joinToString(",")
        presets.forEach { (preset, items) ->
            items.forEach { item ->
                lines +=
                    row(
                        preset.id.toString(),
                        text(preset.name),
                        preset.mealSlot.name,
                        item.foodItemId.toString(),
                        num(item.quantityGrams),
                    )
            }
        }

        return BOM + lines.joinToString("\n")
    }

    private fun row(vararg cells: String): String = cells.joinToString(",") { escape(it) }

    // Locale.US keeps the decimal point a '.' — comma-decimal locales would break the CSV columns.
    private fun num(value: Double): String = "%.2f".format(Locale.US, value).trimEnd('0').trimEnd('.')

    private fun optNum(value: Double?): String = value?.let { num(it) } ?: ""

    /** Formula-injection guard for user-editable text: Excel executes leading =, +, -, @. */
    private fun text(value: String?): String {
        if (value.isNullOrEmpty()) return ""
        return if (value.first() in FORMULA_TRIGGERS) "'$value" else value
    }

    private fun escape(field: String): String =
        if (field.contains(',') || field.contains('"') || field.contains('\n')) {
            "\"${field.replace("\"", "\"\"")}\""
        } else {
            field
        }

    private val FORMULA_TRIGGERS = charArrayOf('=', '+', '-', '@', '\t')

    // --- Import ---

    /** Parsed content of a Diaita export file; ids are the file's own refs, not database ids. */
    data class Parsed(
        val foods: List<ParsedFood>,
        val diary: List<ParsedDiaryRow>,
        val weights: List<WeightEntry>,
        val presets: List<ParsedPreset>,
    )

    data class ParsedFood(
        val ref: Long,
        val food: FoodItem,
    )

    data class ParsedDiaryRow(
        val date: String,
        val loggedAt: Long,
        val foodRef: Long,
        val quantityGrams: Double,
        val mealSlot: MealSlot,
        val presetId: Long?,
        val presetName: String?,
    )

    data class ParsedPreset(
        val ref: Long,
        val name: String,
        val mealSlot: MealSlot,
        val items: List<Pair<Long, Double>>,
    )

    sealed class ParseResult {
        data class Success(
            val parsed: Parsed,
        ) : ParseResult()

        data object NotADiaitaExport : ParseResult()

        data class Malformed(
            val line: Int,
        ) : ParseResult()
    }

    fun parse(content: String): ParseResult {
        val body = content.removePrefix(BOM)
        val lines = body.lines()
        if (lines.firstOrNull()?.trim() != FORMAT_HEADER) return ParseResult.NotADiaitaExport

        val foods = mutableListOf<ParsedFood>()
        val diary = mutableListOf<ParsedDiaryRow>()
        val weights = mutableListOf<WeightEntry>()
        val presetRows = mutableListOf<List<String>>()

        var section = ""
        lines.forEachIndexed { index, rawLine ->
            val line = rawLine.trim()
            if (index == 0 || line.isEmpty()) return@forEachIndexed
            if (line.startsWith("[")) {
                section = line
                return@forEachIndexed
            }
            val cells = parseCsvLine(line)
            try {
                when (section) {
                    SECTION_FOODS -> if (!isHeader(cells, FOODS_HEADER)) foods += parseFood(cells)
                    SECTION_DIARY -> if (!isHeader(cells, DIARY_HEADER)) diary += parseDiaryRow(cells)
                    SECTION_WEIGHTS ->
                        if (!isHeader(cells, WEIGHTS_HEADER)) {
                            weights +=
                                WeightEntry(
                                    date = LocalDate.parse(cells[0]).toString(),
                                    weightKg = cells[1].toDouble(),
                                )
                        }
                    SECTION_PRESETS -> if (!isHeader(cells, PRESETS_HEADER)) presetRows += cells
                    else -> return ParseResult.Malformed(index + 1)
                }
            } catch (e: NumberFormatException) {
                return ParseResult.Malformed(index + 1)
            } catch (e: DateTimeParseException) {
                return ParseResult.Malformed(index + 1)
            } catch (e: IndexOutOfBoundsException) {
                return ParseResult.Malformed(index + 1)
            } catch (e: IllegalArgumentException) {
                return ParseResult.Malformed(index + 1)
            }
        }

        val presets =
            try {
                presetRows
                    .groupBy { it[0].toLong() }
                    .map { (ref, rows) ->
                        ParsedPreset(
                            ref = ref,
                            name = untext(rows.first()[1]),
                            mealSlot = MealSlot.valueOf(rows.first()[2]),
                            items = rows.map { it[3].toLong() to it[4].toDouble() },
                        )
                    }
            } catch (e: RuntimeException) {
                return ParseResult.Malformed(lines.size)
            }

        return ParseResult.Success(Parsed(foods, diary, weights, presets))
    }

    private fun isHeader(
        cells: List<String>,
        header: List<String>,
    ): Boolean = cells.firstOrNull() == header.first()

    private fun parseFood(cells: List<String>): ParsedFood =
        ParsedFood(
            ref = cells[0].toLong(),
            food =
                FoodItem(
                    seedKey = untext(cells[1]).ifEmpty { null },
                    name = untext(cells[2]),
                    brand = untext(cells[3]).ifEmpty { null },
                    barcode = untext(cells[4]).ifEmpty { null },
                    caloriesPer100g = cells[5].toDouble(),
                    proteinPer100g = cells[6].toDouble(),
                    fatPer100g = cells[7].toDouble(),
                    carbsPer100g = cells[8].toDouble(),
                    fiberPer100g = cells[9].toDoubleOrNull(),
                    sugarPer100g = cells[10].toDoubleOrNull(),
                    saltPer100g = cells[11].toDoubleOrNull(),
                    defaultServingGrams = cells[12].toDoubleOrNull(),
                    gramsPerMilliliter = cells[13].toDoubleOrNull(),
                    gramsPerPiece = cells[14].toDoubleOrNull(),
                    source = FoodSource.valueOf(cells[15]),
                    offId = untext(cells[16]).ifEmpty { null },
                    isFavorite = cells[17] == "1",
                ),
        )

    private fun parseDiaryRow(cells: List<String>): ParsedDiaryRow {
        val date = LocalDate.parse(cells[0])
        val time = LocalTime.parse(cells[1])
        return ParsedDiaryRow(
            date = date.toString(),
            loggedAt =
                date
                    .atTime(time)
                    .atZone(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli(),
            foodRef = cells[2].toLong(),
            quantityGrams = cells[4].toDouble(),
            mealSlot = MealSlot.valueOf(cells[5]),
            presetId = cells[10].toLongOrNull(),
            presetName = untext(cells[11]).ifEmpty { null },
        )
    }

    /** Strips the formula-injection guard quote the exporter added. */
    private fun untext(value: String): String = value.removePrefix("'")

    /** Minimal RFC-4180-style field splitter (quotes, escaped quotes, commas in quotes). */
    fun parseCsvLine(line: String): List<String> {
        val fields = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val ch = line[i]
            when {
                inQuotes && ch == '"' && i + 1 < line.length && line[i + 1] == '"' -> {
                    current.append('"')
                    i++
                }
                ch == '"' -> inQuotes = !inQuotes
                ch == ',' && !inQuotes -> {
                    fields += current.toString()
                    current.clear()
                }
                else -> current.append(ch)
            }
            i++
        }
        fields += current.toString()
        return fields
    }
}
