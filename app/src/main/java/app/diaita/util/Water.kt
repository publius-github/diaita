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
