package app.diaita.data.repository

import app.diaita.data.db.dao.DailyGoalDao
import app.diaita.data.db.entities.DailyGoal
import kotlinx.coroutines.flow.Flow

class GoalRepository(
    private val dailyGoalDao: DailyGoalDao,
) {
    fun getGoal(): Flow<DailyGoal?> = dailyGoalDao.getGoal()

    suspend fun setGoal(
        calorieTarget: Int,
        proteinTargetG: Int,
        fatTargetG: Int,
        carbsTargetG: Int,
        weightTargetKg: Double?,
    ) {
        dailyGoalDao.setGoal(
            DailyGoal(
                calorieTarget = calorieTarget,
                proteinTargetG = proteinTargetG,
                fatTargetG = fatTargetG,
                carbsTargetG = carbsTargetG,
                weightTargetKg = weightTargetKg,
            ),
        )
    }
}
