package app.diaita.util

import kotlin.math.roundToInt

/** The optional nutrients the catalog may carry per 100 g; foods without data leave them null. */
enum class Micronutrient { FIBER, SUGAR, SALT }

data class MicronutrientTotal(
    val nutrient: Micronutrient,
    val grams: Double,
) {
    /**
     * Salt keeps one decimal — a whole day lands at a few grams, so rounding to integers
     * would throw away most of the signal. Fiber and sugar are tens of grams and round.
     */
    val displayGrams: String
        get() =
            if (nutrient == Micronutrient.SALT) {
                formatWeightForDisplay(grams)
            } else {
                grams.roundToInt().toString()
            }
}

/**
 * Reference daily intakes the progress bars are drawn against. Fiber is a target to reach,
 * sugar and salt are ceilings to stay under — commonly cited adult reference values, not
 * personalized numbers: the goal wizard computes calories and macros, nothing for these.
 */
const val FIBER_TARGET_GRAMS = 30.0
const val SUGAR_LIMIT_GRAMS = 50.0
const val SALT_LIMIT_GRAMS = 5.0

/** A total placed against its reference value, ready to draw as a bar. */
data class MicronutrientProgress(
    val total: MicronutrientTotal,
    val referenceGrams: Double,
    /** True for the ceilings (sugar, salt): passing the reference is bad, not an achievement. */
    val isLimit: Boolean,
) {
    val fraction: Float get() = (total.grams / referenceGrams).toFloat().coerceIn(0f, 1f)

    /** Ceiling passed — the bar turns into a warning. Never set for the fiber target. */
    val exceeded: Boolean get() = isLimit && total.grams > referenceGrams

    /** Target met — only meaningful for fiber. */
    val reached: Boolean get() = !isLimit && total.grams >= referenceGrams

    val referenceDisplayGrams: String get() = MicronutrientTotal(total.nutrient, referenceGrams).displayGrams
}

/** Pairs each present total with its reference value, preserving display order. */
fun micronutrientProgress(totals: List<MicronutrientTotal>): List<MicronutrientProgress> =
    totals.map { total ->
        when (total.nutrient) {
            Micronutrient.FIBER -> MicronutrientProgress(total, FIBER_TARGET_GRAMS, isLimit = false)
            Micronutrient.SUGAR -> MicronutrientProgress(total, SUGAR_LIMIT_GRAMS, isLimit = true)
            Micronutrient.SALT -> MicronutrientProgress(total, SALT_LIMIT_GRAMS, isLimit = true)
        }
    }

/**
 * Daily fiber/sugar/salt totals in display order, dropping the ones nothing logged that day
 * carries data for. A null total means "no food contributed a value" (SQL `SUM` over an
 * all-null column), which must read as absent rather than as a real zero.
 */
fun micronutrientTotals(
    fiberGrams: Double?,
    sugarGrams: Double?,
    saltGrams: Double?,
): List<MicronutrientTotal> =
    listOfNotNull(
        fiberGrams?.let { MicronutrientTotal(Micronutrient.FIBER, it) },
        sugarGrams?.let { MicronutrientTotal(Micronutrient.SUGAR, it) },
        saltGrams?.let { MicronutrientTotal(Micronutrient.SALT, it) },
    )
