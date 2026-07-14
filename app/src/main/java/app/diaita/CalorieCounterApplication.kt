package app.diaita

import android.app.Application
import app.diaita.data.maintenance.RetentionMaintenance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class CalorieCounterApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.appScope.launch {
            runCatching {
                RetentionMaintenance(
                    container.database.diaryEntryDao(),
                    container.database.monthlyAggregateDao(),
                    retentionDays = container.settingsRepository.retentionDays.first(),
                ).run()
            }
        }
    }
}
