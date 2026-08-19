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

/** Traffic-light zones for a day's fiber/sugar/salt total against its daily limit. */
enum class MicronutrientBand { WITHIN, OVER, FAR_OVER }

/**
 * Daily limits the totals line colors against. Sugar and salt are the WHO ceilings (free
 * sugars under 50 g at a 2000 kcal intake, salt under 5 g). Fiber has no harm ceiling in the
 * guidelines — 25-38 g/day is an adequacy MINIMUM — so its limit is the ~70 g mark commonly
 * cited for GI discomfort, far above the target; a normal high-fiber day must stay green.
 */
fun micronutrientLimitGrams(nutrient: Micronutrient): Double =
    when (nutrient) {
        Micronutrient.FIBER -> FIBER_LIMIT_G
        Micronutrient.SUGAR -> SUGAR_LIMIT_G
        Micronutrient.SALT -> SALT_LIMIT_G
    }

/**
 * WITHIN up to the limit, OVER inside the user's tolerance past it, FAR_OVER beyond —
 * the same three-zone semantics as the macro bars' overshoot handling.
 */
fun micronutrientBand(
    nutrient: Micronutrient,
    grams: Double,
    overTolerancePercent: Int,
): MicronutrientBand {
    val limit = micronutrientLimitGrams(nutrient)
    return when {
        grams <= limit -> MicronutrientBand.WITHIN
        grams <= limit * (1 + overTolerancePercent / 100.0) -> MicronutrientBand.OVER
        else -> MicronutrientBand.FAR_OVER
    }
}

const val FIBER_LIMIT_G = 70.0
const val SUGAR_LIMIT_G = 50.0
const val SALT_LIMIT_G = 5.0
