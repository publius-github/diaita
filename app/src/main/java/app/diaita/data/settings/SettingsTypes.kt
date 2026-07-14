package app.diaita.data.settings

/** App-wide theme selection; SYSTEM follows the OS light/dark setting. */
enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** Voice of the pace message on the Home card. OFF hides it entirely; BRUTAL takes no prisoners. */
enum class CoachTone { OFF, GENTLE, SNARKY, BRUTAL }

/** Display unit for body weight. Storage stays in kilograms regardless. */
enum class WeightUnit {
    KG,
    LB,
    ;

    fun fromKg(kg: Double): Double = if (this == KG) kg else kg * LB_PER_KG

    fun toKg(value: Double): Double = if (this == KG) value else value / LB_PER_KG

    companion object {
        const val LB_PER_KG = 2.2046226218
    }
}

/**
 * Reference shares of the daily calorie budget per day part. Evening is derived so the
 * three always sum to 100. Defaults follow the 30/40/30 dietitian guideline.
 */
data class DayPartShares(
    val morningPercent: Int = DEFAULT_MORNING_PERCENT,
    val dayPercent: Int = DEFAULT_DAY_PERCENT,
) {
    val eveningPercent: Int get() = 100 - morningPercent - dayPercent

    companion object {
        const val DEFAULT_MORNING_PERCENT = 30
        const val DEFAULT_DAY_PERCENT = 40

        /** Every part must keep at least this share so no segment collapses to nothing. */
        const val MIN_PART_PERCENT = 10

        /** Clamps arbitrary slider input into a valid pair summing with evening to 100. */
        fun sanitized(
            morningPercent: Int,
            dayPercent: Int,
        ): DayPartShares {
            val morning = morningPercent.coerceIn(MIN_PART_PERCENT, 100 - 2 * MIN_PART_PERCENT)
            val day = dayPercent.coerceIn(MIN_PART_PERCENT, 100 - morning - MIN_PART_PERCENT)
            return DayPartShares(morningPercent = morning, dayPercent = day)
        }
    }
}
