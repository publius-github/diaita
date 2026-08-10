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

/** Body stats the goal wizard was last completed with; see [SettingsRepository.wizardInputs]. */
data class WizardInputs(
    val male: Boolean,
    val age: Int,
    val heightCm: Double,
    val weightKg: Double,
    /** Stored [app.diaita.util.WorkActivity]/[app.diaita.util.ExerciseLevel] names; null = never set. */
    val work: String? = null,
    val exercise: String? = null,
)

/**
 * Macro calorie split used when the wizard applies a goal. Carbs are derived so the three
 * always sum to 100. The default matches the historical hardcoded 30/25/45.
 */
data class MacroSplit(
    val proteinPercent: Int = DEFAULT_PROTEIN_PERCENT,
    val fatPercent: Int = DEFAULT_FAT_PERCENT,
) {
    val carbsPercent: Int get() = 100 - proteinPercent - fatPercent

    fun proteinGrams(calories: Int): Int = (calories * proteinPercent / 100.0 / 4).toInt()

    fun fatGrams(calories: Int): Int = (calories * fatPercent / 100.0 / 9).toInt()

    fun carbsGrams(calories: Int): Int = (calories * carbsPercent / 100.0 / 4).toInt()

    companion object {
        const val DEFAULT_PROTEIN_PERCENT = 30
        const val DEFAULT_FAT_PERCENT = 25

        /** The picker's preset choices: default, higher-protein, keto-leaning, balanced. */
        val PRESETS =
            listOf(
                MacroSplit(30, 25),
                MacroSplit(40, 30),
                MacroSplit(25, 50),
                MacroSplit(20, 30),
            )
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
