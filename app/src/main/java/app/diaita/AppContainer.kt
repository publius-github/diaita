package app.diaita

import android.content.Context
import android.util.Log
import androidx.room.withTransaction
import app.diaita.data.backup.DatabaseBackupManager
import app.diaita.data.connectivity.ConnectivityObserver
import app.diaita.data.db.AppDatabase
import app.diaita.data.maintenance.RetentionMaintenance
import app.diaita.data.onboarding.OnboardingGate
import app.diaita.data.remote.OpenFoodFactsClient
import app.diaita.data.repository.DiaryRepository
import app.diaita.data.repository.ExportRepository
import app.diaita.data.repository.FoodRepository
import app.diaita.data.repository.GoalRepository
import app.diaita.data.repository.PresetRepository
import app.diaita.data.repository.WeightRepository
import app.diaita.data.settings.SettingsRepository
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.time.LocalDate

/** Manual DI container: one instance per process, owned by [CalorieCounterApplication]. */
class AppContainer(
    context: Context,
) {
    /**
     * Off-main-thread scope for app-lifetime background work (seed sync, retention).
     * Failures are logged instead of vanishing — a supervisor job alone swallows nothing
     * loudly enough to debug.
     */
    val appScope =
        CoroutineScope(
            SupervisorJob() +
                Dispatchers.Default +
                CoroutineExceptionHandler { _, error ->
                    Log.e(TAG, "Background job failed", error)
                },
        )

    val settingsRepository = SettingsRepository(context.applicationContext)

    private val database: AppDatabase = AppDatabase.getInstance(context, appScope, settingsRepository)
    private val connectivityObserver = ConnectivityObserver(context, appScope)
    private val openFoodFactsClient = OpenFoodFactsClient(BuildConfig.VERSION_NAME)

    val foodRepository: FoodRepository by lazy {
        FoodRepository(database.foodItemDao(), openFoodFactsClient.api, connectivityObserver)
    }
    val diaryRepository: DiaryRepository by lazy { DiaryRepository(database, database.diaryEntryDao()) }
    val presetRepository: PresetRepository by lazy { PresetRepository(database, database.mealPresetDao()) }
    val goalRepository: GoalRepository by lazy { GoalRepository(database.dailyGoalDao()) }
    val exportRepository: ExportRepository by lazy { ExportRepository(database) }
    val weightRepository: WeightRepository by lazy { WeightRepository(database.weightEntryDao()) }
    val backupManager: DatabaseBackupManager by lazy {
        DatabaseBackupManager(context.applicationContext, database)
    }
    val onboardingGate: OnboardingGate by lazy {
        OnboardingGate(settingsRepository, goalRepository, weightRepository, diaryRepository)
    }

    /** The start-of-app retention pass, wired with a real Room transaction runner. */
    fun retentionMaintenance(retentionDays: Int): RetentionMaintenance =
        RetentionMaintenance(
            diaryEntryDao = database.diaryEntryDao(),
            aggregateDao = database.monthlyAggregateDao(),
            today = LocalDate.now(),
            retentionDays = retentionDays,
            foodItemDao = database.foodItemDao(),
            transactionRunner = { block -> database.withTransaction { block() } },
        )

    companion object {
        const val TAG = "Diaita"
    }
}
