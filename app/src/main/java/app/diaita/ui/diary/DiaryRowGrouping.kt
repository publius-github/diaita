package app.diaita.ui.diary

import app.diaita.data.db.dao.DiaryEntryWithFood
import app.diaita.data.db.entities.MealSlot

data class DiarySection(val slot: MealSlot, val rows: List<DiaryRow>)

private val SECTION_ORDER = listOf(
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
            val group = entries.filter {
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
