package app.diaita.util

/**
 * Occupational activity, taken separately from exercise. A desk worker who trains three
 * times a week is only "lightly active" overall — folding the job into one generic scale
 * (the classic 1.2..1.9 multipliers) overestimates desk workers, so work and exercise
 * each contribute their own share of the multiplier.
 */
enum class WorkActivity(
    val base: Double,
) {
    SEDENTARY(1.2),
    STANDING(1.35),
    PHYSICAL(1.5),
    HEAVY(1.65),
}

/**
 * Planned exercise on top of the job. Each weekly session burns roughly 300-500 kcal,
 * which averaged over the week is ~0.04-0.05 of BMR per session — the bonuses below
 * follow that: e.g. sedentary job + 3-4 sessions lands at 1.375, matching what
 * dual-input TDEE calculators give for that combination.
 */
enum class ExerciseLevel(
    val bonus: Double,
) {
    NONE(0.0),
    LIGHT(0.1),
    MODERATE(0.175),
    FREQUENT(0.3),
    INTENSE(0.4),
}

/**
 * Fixed kcal/day adjustment on top of TDEE. NIH guidance puts the safe deficit range at
 * 500-1000 kcal/day (0.5-1 kg/week): steady = the recommended lower end, aggressive = the
 * upper end. Gain surpluses are smaller because excess beyond ~500 kcal is mostly fat.
 */
enum class WeightPlan(
    val calorieDelta: Int,
) {
    // Declaration order is the on-screen order: most aggressive loss at the top,
    // maintain in the middle, most aggressive gain at the bottom.
    LOSE_FAST(-1000),
    LOSE_STEADY(-500),
    MAINTAIN(0),
    GAIN_LEAN(300),
    GAIN_FAST(500),
}

/**
 * Daily calorie suggestion via the Mifflin-St Jeor equation. This is a starting-point
 * estimate for the Goals screen, not medical advice — the user can always override it.
 */
object GoalCalculator {
    /**
     * Commonly cited minimum daily intakes below which nutrient needs are hard to meet
     * (roughly 1200 kcal for women, 1500 kcal for men). Suggestions never go below these.
     */
    const val MIN_CALORIES_FEMALE = 1200
    const val MIN_CALORIES_MALE = 1500

    /** Ceiling of the classic activity scale ("extremely active"): heavy labor plus daily training. */
    const val MAX_ACTIVITY_MULTIPLIER = 1.9

    /** Mifflin-St Jeor resting energy expenditure, kcal/day. */
    fun bmr(
        male: Boolean,
        ageYears: Int,
        heightCm: Double,
        weightKg: Double,
    ): Double = 10.0 * weightKg + 6.25 * heightCm - 5.0 * ageYears + if (male) 5.0 else -161.0

    /** Combined activity multiplier: job base plus exercise bonus, capped at the classic scale's top. */
    fun activityMultiplier(
        work: WorkActivity,
        exercise: ExerciseLevel,
    ): Double = (work.base + exercise.bonus).coerceAtMost(MAX_ACTIVITY_MULTIPLIER)

    /** Total daily energy expenditure: BMR scaled by the combined activity multiplier. */
    fun tdee(
        male: Boolean,
        ageYears: Int,
        heightCm: Double,
        weightKg: Double,
        work: WorkActivity,
        exercise: ExerciseLevel,
    ): Double = bmr(male, ageYears, heightCm, weightKg) * activityMultiplier(work, exercise)

    fun dailyCalories(
        male: Boolean,
        ageYears: Int,
        heightCm: Double,
        weightKg: Double,
        work: WorkActivity,
        exercise: ExerciseLevel,
        plan: WeightPlan,
    ): Int {
        val target = tdee(male, ageYears, heightCm, weightKg, work, exercise) + plan.calorieDelta
        val minimum = if (male) MIN_CALORIES_MALE else MIN_CALORIES_FEMALE
        return target.toInt().coerceAtLeast(minimum)
    }
}
