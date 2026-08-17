package app.diaita.util

/**
 * Scale factor applied to a dish's raw ingredient weights when [eatenGrams] of the cooked
 * dish (weighing [cookedWeightGrams] in total) were eaten. Values above 1 are legal — the
 * user may eat more than one batch. Null when either weight is missing or non-positive,
 * which callers treat as "don't log anything".
 */
fun dishPortionFactor(
    cookedWeightGrams: Double?,
    eatenGrams: Double,
): Double? {
    if (cookedWeightGrams == null || cookedWeightGrams <= 0 || eatenGrams <= 0) return null
    return eatenGrams / cookedWeightGrams
}
