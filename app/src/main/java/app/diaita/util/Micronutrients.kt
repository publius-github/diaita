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
