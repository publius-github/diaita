package app.diaita

import android.content.Context
import app.diaita.data.backup.DatabaseBackupManager
import app.diaita.data.connectivity.ConnectivityObserver
import app.diaita.data.db.AppDatabase
import app.diaita.data.remote.OpenFoodFactsClient
import app.diaita.data.repository.DiaryRepository
import app.diaita.data.repository.ExportRepository
import app.diaita.data.repository.FoodRepository
import app.diaita.data.repository.GoalRepository
import app.diaita.data.repository.PresetRepository
import app.diaita.data.repository.WeightRepository
import app.diaita.data.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

/** Manual DI container: one instance per process, owned by [CalorieCounterApplication]. */
class AppContainer(
    context: Context,
) {
    val appScope = CoroutineScope(SupervisorJob())

    val database: AppDatabase = AppDatabase.getInstance(context, appScope)
    private val connectivityObserver = ConnectivityObserver(context)

    val foodRepository: FoodRepository by lazy {
        FoodRepository(database.foodItemDao(), OpenFoodFactsClient.api, connectivityObserver)
    }
    val diaryRepository: DiaryRepository by lazy { DiaryRepository(database.diaryEntryDao()) }
    val presetRepository: PresetRepository by lazy { PresetRepository(database, database.mealPresetDao()) }
    val goalRepository: GoalRepository by lazy { GoalRepository(database.dailyGoalDao()) }
    val exportRepository: ExportRepository by lazy {
        ExportRepository(database.diaryEntryDao(), database.foodItemDao())
    }
    val weightRepository: WeightRepository by lazy { WeightRepository(database.weightEntryDao()) }
    val settingsRepository: SettingsRepository by lazy { SettingsRepository(context.applicationContext) }
    val backupManager: DatabaseBackupManager by lazy {
        DatabaseBackupManager(context.applicationContext, database)
    }
}
