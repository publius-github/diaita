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
    fun `daily calories applies activity and plan factors`() {
        val maintain =
            GoalCalculator.dailyCalories(
                male = true,
                ageYears = 30,
                heightCm = 180.0,
                weightKg = 80.0,
                activity = ActivityLevel.SEDENTARY,
                plan = WeightPlan.MAINTAIN,
            )
        assertEquals((1780.0 * 1.2).toInt(), maintain)

        val lose =
            GoalCalculator.dailyCalories(
                male = true,
                ageYears = 30,
                heightCm = 180.0,
                weightKg = 80.0,
                activity = ActivityLevel.SEDENTARY,
                plan = WeightPlan.LOSE,
            )
        assertEquals((1780.0 * 1.2 * 0.85).toInt(), lose)
    }
}
