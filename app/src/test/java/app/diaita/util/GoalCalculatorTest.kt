package app.diaita.util

import org.junit.Assert.assertEquals
import org.junit.Test

class GoalCalculatorTest {
    @Test
    fun `mifflin st jeor reference values`() {
        // 30yo male, 180cm, 80kg: 10*80 + 6.25*180 - 5*30 + 5 = 1780
        assertEquals(1780.0, GoalCalculator.bmr(male = true, ageYears = 30, heightCm = 180.0, weightKg = 80.0), 1e-9)
        // 30yo female, 165cm, 60kg: 10*60 + 6.25*165 - 5*30 - 161 = 1320.25
        assertEquals(1320.25, GoalCalculator.bmr(male = false, ageYears = 30, heightCm = 165.0, weightKg = 60.0), 1e-9)
    }

    @Test
    fun `multiplier combines job base and exercise bonus`() {
        // Desk job with no training is the classic sedentary 1.2.
        assertEquals(1.2, GoalCalculator.activityMultiplier(WorkActivity.SEDENTARY, ExerciseLevel.NONE), 1e-9)
        // Desk job + 3-4 workouts a week lands on the "lightly active" 1.375, not 1.55.
        assertEquals(1.375, GoalCalculator.activityMultiplier(WorkActivity.SEDENTARY, ExerciseLevel.MODERATE), 1e-9)
    }

    @Test
    fun `multiplier is capped at the extremely active ceiling`() {
        // Heavy labor + daily training would be 1.65 + 0.4 = 2.05 uncapped.
        assertEquals(
            GoalCalculator.MAX_ACTIVITY_MULTIPLIER,
            GoalCalculator.activityMultiplier(WorkActivity.HEAVY, ExerciseLevel.INTENSE),
            1e-9,
        )
    }

    @Test
    fun `tdee scales bmr by the combined multiplier`() {
        assertEquals(
            1780.0 * 1.375,
            GoalCalculator.tdee(
                male = true,
                ageYears = 30,
                heightCm = 180.0,
                weightKg = 80.0,
                work = WorkActivity.SEDENTARY,
                exercise = ExerciseLevel.MODERATE,
            ),
            1e-9,
        )
    }

    @Test
    fun `daily calories applies fixed plan deltas on top of tdee`() {
        // Multiplier 1.2 + 0.3 = 1.5 keeps even the aggressive deficit above the 1500 kcal
        // male floor, so every delta is visible unclamped.
        fun calories(plan: WeightPlan): Int =
            GoalCalculator.dailyCalories(
                male = true,
                ageYears = 30,
                heightCm = 180.0,
                weightKg = 80.0,
                work = WorkActivity.SEDENTARY,
                exercise = ExerciseLevel.FREQUENT,
                plan = plan,
            )

        val tdee = (1780.0 * 1.5).toInt()
        assertEquals(tdee, calories(WeightPlan.MAINTAIN))
        assertEquals(tdee - 500, calories(WeightPlan.LOSE_STEADY))
        assertEquals(tdee - 1000, calories(WeightPlan.LOSE_FAST))
        assertEquals(tdee + 300, calories(WeightPlan.GAIN_LEAN))
        assertEquals(tdee + 500, calories(WeightPlan.GAIN_FAST))
    }

    @Test
    fun `weight loss never suggests less than the safe minimum`() {
        // 60yo female, 150cm, 45kg, sedentary: BMR 926.5, TDEE ~1112 — a steady loss would
        // land at ~612 kcal, so the suggestion is raised to the 1200 kcal floor.
        val female =
            GoalCalculator.dailyCalories(
                male = false,
                ageYears = 60,
                heightCm = 150.0,
                weightKg = 45.0,
                work = WorkActivity.SEDENTARY,
                exercise = ExerciseLevel.NONE,
                plan = WeightPlan.LOSE_STEADY,
            )
        assertEquals(GoalCalculator.MIN_CALORIES_FEMALE, female)

        // 30yo male, 180cm, 80kg, sedentary: TDEE 2136 — an aggressive loss lands at 1136,
        // below the 1500 kcal male floor.
        val male =
            GoalCalculator.dailyCalories(
                male = true,
                ageYears = 30,
                heightCm = 180.0,
                weightKg = 80.0,
                work = WorkActivity.SEDENTARY,
                exercise = ExerciseLevel.NONE,
                plan = WeightPlan.LOSE_FAST,
            )
        assertEquals(GoalCalculator.MIN_CALORIES_MALE, male)
    }
}
