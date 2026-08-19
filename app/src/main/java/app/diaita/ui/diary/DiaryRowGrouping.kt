package app.diaita.ui.diary

import app.diaita.data.db.dao.DiaryEntryWithFood
import app.diaita.data.db.entities.DiaryEntry
import app.diaita.data.db.entities.MealSlot
import app.diaita.data.settings.DayPartBoundaries
import app.diaita.data.settings.DayPartOverThresholds
import app.diaita.data.settings.DayPartShares
import app.diaita.util.NutrientMath
import java.time.Instant
import java.time.ZoneId
import kotlin.math.roundToInt

data class DiarySection(
    val slot: MealSlot,
    val rows: List<DiaryRow>,
)

/**
 * Thirds of the day for the balance bar on the Diary screen. The reference share of the
 * daily calorie budget per part is the user-configurable [DayPartShares] (default 30/40/30,
 * a common dietitian guideline for distributing calories across the day).
 */
enum class DayPart { MORNING, DAY, EVENING }

/** The fraction (0..1) of the daily budget this configuration assigns to [part]. */
fun DayPartShares.shareOf(part: DayPart): Float =
    when (part) {
        DayPart.MORNING -> morningPercent
        DayPart.DAY -> dayPercent
        DayPart.EVENING -> eveningPercent
    } / 100f

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
            // Null name stays null here — the UI maps it to a localized fallback resource.
            rows.add(DiaryRow.Preset(entry.sourcePresetName, group))
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
 * unslotted entries fall back to the wall-clock time they were logged at, split by the
 * user-configurable [DayPartBoundaries].
 */
fun dayPartOf(
    entry: DiaryEntry,
    boundaries: DayPartBoundaries = DayPartBoundaries(),
): DayPart =
    when (entry.mealSlot) {
        MealSlot.BREAKFAST -> DayPart.MORNING
        MealSlot.LUNCH -> DayPart.DAY
        MealSlot.DINNER -> DayPart.EVENING
        MealSlot.SNACK, MealSlot.UNSPECIFIED -> {
            val time = Instant.ofEpochMilli(entry.loggedAt).atZone(ZoneId.systemDefault())
            val minutes = time.hour * 60 + time.minute
            when {
                minutes < boundaries.morningEndMinutes -> DayPart.MORNING
                minutes < boundaries.dayEndMinutes -> DayPart.DAY
                else -> DayPart.EVENING
            }
        }
    }

/**
 * A day part's calorie budget for the balance bar: the plain [DayPartShares] slice ([base]),
 * the signed remainder carried in from earlier finished parts ([carriedIn], positive for a
 * deficit, negative for an overshoot), and the effective [target] = base + carry floored
 * at zero so a big earlier overshoot can't promise negative food.
 */
data class DayPartBudget(
    val base: Int,
    val target: Int,
    val carriedIn: Int,
)

/** The part of the day the wall clock currently falls into, per the configured [boundaries]. */
fun currentDayPart(
    nowMinutes: Int,
    boundaries: DayPartBoundaries = DayPartBoundaries(),
): DayPart =
    when {
        nowMinutes < boundaries.morningEndMinutes -> DayPart.MORNING
        nowMinutes < boundaries.dayEndMinutes -> DayPart.DAY
        else -> DayPart.EVENING
    }

/**
 * Per-part calorie budgets for the balance bar. Each part starts from its [DayPartShares]
 * slice of the daily goal; once a part's time window is over (its [DayPartBoundaries] end
 * vs [nowMinutes]), the signed remainder of its target carries forward into the next part —
 * a deficit raises the next target, an overshoot shrinks it (floored at zero), so the bar
 * never promises more food than the day has left. The carry math runs on the un-floored
 * remainder, so an overshoot bigger than the next part's whole slice keeps flowing onward.
 * [applyCarry] is off for past days: a finished day is a factual record, and rolling its
 * deficits into the evening would repaint a merely light day as an evening failure.
 */
fun buildDayPartBudgets(
    goalCalories: Int,
    shares: DayPartShares,
    consumed: Map<DayPart, Double>,
    nowMinutes: Int,
    boundaries: DayPartBoundaries = DayPartBoundaries(),
    applyCarry: Boolean = true,
): Map<DayPart, DayPartBudget> {
    var carry = 0.0
    return DayPart.entries.associateWith { part ->
        val base = goalCalories * shares.shareOf(part).toDouble()
        val carriedIn = if (applyCarry) carry else 0.0
        val rawTarget = base + carriedIn
        val partOver =
            when (part) {
                DayPart.MORNING -> nowMinutes >= boundaries.morningEndMinutes
                DayPart.DAY -> nowMinutes >= boundaries.dayEndMinutes
                DayPart.EVENING -> false
            }
        carry = if (partOver) rawTarget - (consumed[part] ?: 0.0) else 0.0
        DayPartBudget(
            base = base.roundToInt(),
            target = rawTarget.coerceAtLeast(0.0).roundToInt(),
            carriedIn = carriedIn.roundToInt(),
        )
    }
}

/** How hard a day-part segment overshot its target, banded by [DayPartOverThresholds]. */
enum class DayPartOverBand { UNDER, SMALL, MEDIUM, LARGE }

/**
 * UNDER at or below the target. Past it, within [DayPartOverThresholds.amberPercent] the
 * overshoot is SMALL and still renders green — noise, not a signal. Beyond the slack the
 * band depends on the whole day: MEDIUM (amber) while the day total is still within the
 * goal — a lone part overshoot is recoverable; LARGE (red) once [dayOver] — this part's
 * excess is part of a genuinely blown day. A zero target with anything consumed counts as
 * past the slack (there was no room to begin with).
 */
fun dayPartOverBand(
    consumed: Int,
    target: Int,
    thresholds: DayPartOverThresholds,
    dayOver: Boolean,
): DayPartOverBand {
    if (consumed <= target) return DayPartOverBand.UNDER
    val overPercent = if (target <= 0) Double.MAX_VALUE else (consumed - target) * 100.0 / target
    return when {
        overPercent <= thresholds.amberPercent -> DayPartOverBand.SMALL
        dayOver -> DayPartOverBand.LARGE
        else -> DayPartOverBand.MEDIUM
    }
}

/** Total consumed calories per third of the day, for the balance bar. */
fun buildDayPartCalories(
    entries: List<DiaryEntryWithFood>,
    boundaries: DayPartBoundaries = DayPartBoundaries(),
): Map<DayPart, Double> =
    entries
        .groupBy { dayPartOf(it.entry, boundaries) }
        .mapValues { (_, grouped) ->
            grouped.sumOf { NutrientMath.caloriesFor(it.food, it.entry.quantityGrams) }
        }
