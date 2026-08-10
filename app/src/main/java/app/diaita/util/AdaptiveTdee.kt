package app.diaita.util

import kotlin.math.roundToInt

/**
 * TDEE estimated from what actually happened instead of a formula: over a window with
 * logged intake and weigh-ins at both ends, maintenance = average intake minus the energy
 * equivalent of the observed weight change. This self-corrects for metabolism, NEAT and
 * logging bias in a way Mifflin-St Jeor never can.
 */
object AdaptiveTdee {
    /** Commonly used energy density of body-mass change. */
    const val KCAL_PER_KG = 7700.0

    /** Anything shorter is dominated by water weight, not energy balance. */
    const val MIN_SPAN_DAYS = 14

    /** Sparse logging makes average intake meaningless. */
    const val MIN_LOGGED_DAYS = 10

    /** Estimates outside this band mean the inputs are unreliable — return null, not nonsense. */
    val SANE_RANGE = 800..6000

    /**
     * @param dailyIntakes calories of each day that was actually logged inside the window
     * @param startWeightKg weigh-in at (or near) the window start
     * @param endWeightKg weigh-in at (or near) the window end
     * @param spanDays wall-clock length of the window in days
     */
    fun estimate(
        dailyIntakes: List<Double>,
        startWeightKg: Double,
        endWeightKg: Double,
        spanDays: Int,
    ): Int? {
        if (spanDays < MIN_SPAN_DAYS || dailyIntakes.size < MIN_LOGGED_DAYS) return null
        val avgIntake = dailyIntakes.average()
        val dailyImbalance = (endWeightKg - startWeightKg) * KCAL_PER_KG / spanDays
        val tdee = (avgIntake - dailyImbalance).roundToInt()
        return tdee.takeIf { it in SANE_RANGE }
    }
}
