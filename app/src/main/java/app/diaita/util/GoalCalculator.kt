package app.diaita.util

enum class ActivityLevel(
    val multiplier: Double,
) {
    SEDENTARY(1.2),
    LIGHT(1.375),
    MODERATE(1.55),
    ACTIVE(1.725),
}

enum class WeightPlan(
    val calorieFactor: Double,
) {
    LOSE(0.85),
    MAINTAIN(1.0),
    GAIN(1.10),
}

/**
 * Daily calorie suggestion via the Mifflin-St Jeor equation. This is a starting-point
 * estimate for the Goals screen, not medical advice — the user can always override it.
 */
object GoalCalculator {
    /** Mifflin-St Jeor resting energy expenditure, kcal/day. */
    fun bmr(
        male: Boolean,
        ageYears: Int,
        heightCm: Double,
        weightKg: Double,
    ): Double = 10.0 * weightKg + 6.25 * heightCm - 5.0 * ageYears + if (male) 5.0 else -161.0

    fun dailyCalories(
        male: Boolean,
        ageYears: Int,
        heightCm: Double,
        weightKg: Double,
        activity: ActivityLevel,
        plan: WeightPlan,
    ): Int = (bmr(male, ageYears, heightCm, weightKg) * activity.multiplier * plan.calorieFactor).toInt()
}
