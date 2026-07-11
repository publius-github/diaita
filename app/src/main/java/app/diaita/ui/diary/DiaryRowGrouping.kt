package app.diaita.ui.diary

import app.diaita.data.db.dao.DiaryEntryWithFood
import app.diaita.data.db.entities.DiaryEntry
import app.diaita.data.db.entities.MealSlot
import app.diaita.util.NutrientMath
import java.time.Instant
import java.time.ZoneId

data class DiarySection(
    val slot: MealSlot,
    val rows: List<DiaryRow>,
)

/**
 * Thirds of the day for the balance bar on the Diary screen. [share] is the healthy-eating
 * reference share of the daily calorie budget: ~30% in the morning (breakfast), ~40% during
 * the day (lunch plus afternoon snack), ~30% in the evening (dinner) — a common dietitian
 * guideline for distributing calories across the day.
 */
enum class DayPart(
    val share: Float,
) {
    MORNING(0.30f),
    DAY(0.40f),
    EVENING(0.30f),
}

private val SECTION_ORDER =
    listOf(
        MealSlot.BREAKFAST,
        MealSlot.LUNCH,
        MealSlot.DINNER,
        MealSlot.SNACK,
        MealSlot.UNSPECIFIED,
    )

/** Entries sharing the same (sourcePresetId, loggedAt) came from one preset log — collapse them into one row. */
fun groupEntriesIntoRows(entries: List<DiaryEntryWithFood>): List<DiaryRow> {
    val rows = mutableListOf<DiaryRow>()
    val consumedIds = mutableSetOf<Long>()
    for (entryWithFood in entries) {
        val entry = entryWithFood.entry
        if (entry.id in consumedIds) continue
        val presetId = entry.sourcePresetId
        if (presetId != null) {
            val group =
                entries.filter {
                    it.entry.sourcePresetId == presetId && it.entry.loggedAt == entry.loggedAt
                }
            group.forEach { consumedIds.add(it.entry.id) }
            rows.add(DiaryRow.Preset(entry.sourcePresetName ?: "Preset", group))
        } else {
            consumedIds.add(entry.id)
            rows.add(DiaryRow.Food(entryWithFood))
        }
    }
    return rows
}

/** Groups the day's entries by meal slot (Breakfast/Lunch/Dinner/Snack/Other), preset-collapsing within each. */
fun buildDiarySections(entries: List<DiaryEntryWithFood>): List<DiarySection> =
    SECTION_ORDER.mapNotNull { slot ->
        val slotEntries = entries.filter { it.entry.mealSlot == slot }
        if (slotEntries.isEmpty()) null else DiarySection(slot, groupEntriesIntoRows(slotEntries))
    }

/**
 * Maps an entry to a third of the day: explicit meal slots map directly, while snacks and
 * unslotted entries fall back to the wall-clock hour they were logged at.
 */
fun dayPartOf(entry: DiaryEntry): DayPart =
    when (entry.mealSlot) {
        MealSlot.BREAKFAST -> DayPart.MORNING
        MealSlot.LUNCH -> DayPart.DAY
        MealSlot.DINNER -> DayPart.EVENING
        MealSlot.SNACK, MealSlot.UNSPECIFIED -> {
            val hour = Instant.ofEpochMilli(entry.loggedAt).atZone(ZoneId.systemDefault()).hour
            when {
                hour < 12 -> DayPart.MORNING
                hour < 17 -> DayPart.DAY
                else -> DayPart.EVENING
            }
        }
    }

/** Total consumed calories per third of the day, for the balance bar. */
fun buildDayPartCalories(entries: List<DiaryEntryWithFood>): Map<DayPart, Double> =
    entries
        .groupBy { dayPartOf(it.entry) }
        .mapValues { (_, grouped) ->
            grouped.sumOf { NutrientMath.caloriesFor(it.food, it.entry.quantityGrams) }
        }
