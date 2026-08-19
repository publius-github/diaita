package app.diaita.util

import kotlin.math.roundToInt

/**
 * Daily water target from the goal-wizard body weight: the common ~30 ml/kg hydration
 * guideline, rounded to 50 ml so the figure reads like a target and not a measurement.
 * Falls back to a flat 2 l when the wizard never ran — the bar must still have a scale.
 */
fun waterTargetMl(weightKg: Double?): Int =
    weightKg
        ?.takeIf { it > 0 }
        ?.let { ((it * WATER_ML_PER_KG / 50.0).roundToInt() * 50) }
        ?: DEFAULT_WATER_TARGET_ML

const val WATER_ML_PER_KG = 30.0
const val DEFAULT_WATER_TARGET_ML = 2000

/** Color zone of the water bar; drinking under the target never warns, only real excess does. */
enum class WaterBand { WITHIN, OVER_AMBER, OVER_RED }

/**
 * Classifies the day's water total against the target plus the user's overshoot slack:
 * blue (WITHIN) up to target + [amberPercent], amber to target + [redPercent], red beyond.
 */
fun waterBand(
    totalMl: Int,
    targetMl: Int,
    amberPercent: Int,
    redPercent: Int,
): WaterBand {
    if (targetMl <= 0) return WaterBand.WITHIN
    return when {
        totalMl > targetMl * (1 + redPercent / 100.0) -> WaterBand.OVER_RED
        totalMl > targetMl * (1 + amberPercent / 100.0) -> WaterBand.OVER_AMBER
        else -> WaterBand.WITHIN
    }
}
