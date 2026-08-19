package app.diaita.util

import app.diaita.data.settings.WaterTargetSettings
import kotlin.math.roundToInt

/**
 * Daily drinking target from the goal-wizard body weight: [mlPerKg] (the ~30 ml/kg
 * hydration guideline by default) minus [foodSharePercent], the part of the daily need that
 * arrives with food and metabolism rather than from a glass — the water bar counts taps
 * only, so that share must not sit in its target. Rounded to 50 ml so the figure reads like
 * a target and not a measurement, and floored at [MIN_WATER_TARGET_ML] so extreme settings
 * can't leave the bar without a scale. Falls back to a flat 2 l of total need when the
 * wizard never ran.
 */
fun waterTargetMl(
    weightKg: Double?,
    mlPerKg: Int = WaterTargetSettings.DEFAULT_ML_PER_KG,
    foodSharePercent: Int = 0,
): Int {
    val totalNeedMl = weightKg?.takeIf { it > 0 }?.times(mlPerKg) ?: DEFAULT_WATER_TARGET_ML.toDouble()
    val drinkNeedMl = totalNeedMl * (1 - foodSharePercent.coerceIn(0, 100) / 100.0)
    return ((drinkNeedMl / 50.0).roundToInt() * 50).coerceAtLeast(MIN_WATER_TARGET_ML)
}

/** Total daily need assumed without a wizard weight — before the food share comes off it. */
const val DEFAULT_WATER_TARGET_ML = 2000
const val MIN_WATER_TARGET_ML = 250

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
