package app.diaita.data.settings

/** App-wide theme selection; SYSTEM follows the OS light/dark setting. */
enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** Voice of the pace message on the Home card. OFF hides it entirely; BRUTAL takes no prisoners. */
enum class CoachTone { OFF, GENTLE, SNARKY, BRUTAL }

/**
 * How the Home card shows the day's calorie progress. RING is the original 120dp ring with
 * the figures inside; the other three move the figures into the card header ("X / Y kcal")
 * so the indicator itself can shrink and the dead space left of the big ring goes away.
 */
enum class HomeProgressStyle { RING, COMPACT_RING, GAUGE, BAR }

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
 * How far past its gram target each macro may run before the Diary card flags it. Expressed
 * as a percentage of the target. The asymmetry is deliberate nutrition logic, not taste:
 * protein is a floor whose overshoot is benign far beyond any realistic target (intakes up
 * to ~3.3 g/kg are studied safe, roughly +50% over a typical 2 g/kg goal), fat is the
 * tightest budget (densest at 9 kcal/g, and a fat target set near 30% of energy hits the
 * top of the accepted 20-35% range at about +15%), and carbs are the flexible remainder
 * (4 kcal/g, wide 45-65% accepted range) so they get more slack before the flag.
 */
data class MacroOverTolerances(
    val proteinPercent: Int = DEFAULT_PROTEIN_PERCENT,
    val fatPercent: Int = DEFAULT_FAT_PERCENT,
    val carbsPercent: Int = DEFAULT_CARBS_PERCENT,
) {
    companion object {
        const val DEFAULT_PROTEIN_PERCENT = 50
        const val DEFAULT_FAT_PERCENT = 15
        const val DEFAULT_CARBS_PERCENT = 20
        const val MAX_PERCENT = 100

        fun sanitized(
            proteinPercent: Int,
            fatPercent: Int,
            carbsPercent: Int,
        ): MacroOverTolerances =
            MacroOverTolerances(
                proteinPercent = proteinPercent.coerceIn(0, MAX_PERCENT),
                fatPercent = fatPercent.coerceIn(0, MAX_PERCENT),
                carbsPercent = carbsPercent.coerceIn(0, MAX_PERCENT),
            )
    }
}

/**
 * How far past its daily limit each of fiber/sugar/salt may run before the totals line
 * escalates from yellow to red (within the limit it stays green). The limits themselves are
 * fixed reference values (see [app.diaita.util.micronutrientLimitGrams]); only the slack is
 * user-tunable. The default asymmetry is data-honesty, not taste: the stored sugar is TOTAL
 * sugars (fruit and milk count) while the 50 g WHO figure is about FREE sugars, so sugar
 * gets a wide +50% band before red to keep a fruit-heavy day from screaming; salt counts
 * only what food itself carries (never the shaker) and so already understates; fiber's
 * limit is a generous GI-comfort ceiling, so little extra slack is needed.
 */
data class MicronutrientOverTolerances(
    val fiberPercent: Int = DEFAULT_FIBER_PERCENT,
    val sugarPercent: Int = DEFAULT_SUGAR_PERCENT,
    val saltPercent: Int = DEFAULT_SALT_PERCENT,
) {
    companion object {
        const val DEFAULT_FIBER_PERCENT = 15
        const val DEFAULT_SUGAR_PERCENT = 50
        const val DEFAULT_SALT_PERCENT = 20
        const val MAX_PERCENT = 100

        fun sanitized(
            fiberPercent: Int,
            sugarPercent: Int,
            saltPercent: Int,
        ): MicronutrientOverTolerances =
            MicronutrientOverTolerances(
                fiberPercent = fiberPercent.coerceIn(0, MAX_PERCENT),
                sugarPercent = sugarPercent.coerceIn(0, MAX_PERCENT),
                saltPercent = saltPercent.coerceIn(0, MAX_PERCENT),
            )
    }
}

/**
 * How far past the daily water target the blue bar may run before it warns: within
 * [amberPercent] of overshoot drinking extra is fine and the bar stays blue, past it the
 * bar turns amber, and past [redPercent] red. Wide defaults on purpose — the ~30 ml/kg
 * target is a floor, not a ceiling, so only a genuinely excessive day should recolor.
 */
data class WaterOverThresholds(
    val amberPercent: Int = DEFAULT_AMBER_PERCENT,
    val redPercent: Int = DEFAULT_RED_PERCENT,
) {
    companion object {
        const val DEFAULT_AMBER_PERCENT = 50
        const val DEFAULT_RED_PERCENT = 100
        const val MAX_PERCENT = 200

        /** Clamps slider input; red never sits below amber so the bands can't invert. */
        fun sanitized(
            amberPercent: Int,
            redPercent: Int,
        ): WaterOverThresholds {
            val amber = amberPercent.coerceIn(0, MAX_PERCENT)
            return WaterOverThresholds(
                amberPercent = amber,
                redPercent = redPercent.coerceIn(amber, MAX_PERCENT),
            )
        }
    }
}

/**
 * How much a morning/day/evening segment may overshoot its target before it stops reading
 * green (a small overshoot is noise, not a signal). Past this slack the segment warns in
 * amber; it escalates to red only when the DAY total is over too — a lone part overshoot
 * is recoverable, red means this part's excess is part of a blown day.
 */
data class DayPartOverThresholds(
    val amberPercent: Int = DEFAULT_AMBER_PERCENT,
) {
    companion object {
        const val DEFAULT_AMBER_PERCENT = 10
        const val MAX_PERCENT = 100

        fun sanitized(amberPercent: Int): DayPartOverThresholds =
            DayPartOverThresholds(amberPercent = amberPercent.coerceIn(0, MAX_PERCENT))
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
