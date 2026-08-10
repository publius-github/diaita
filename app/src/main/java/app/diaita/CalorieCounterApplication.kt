package app.diaita

import android.app.Application
import android.util.Log
import app.diaita.data.backup.AutoBackupWorker
import app.diaita.data.reminder.ReminderScheduler
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
                val retentionDays = container.settingsRepository.retentionDays.first()
                container.retentionMaintenance(retentionDays).run()
                Log.i(AppContainer.TAG, "Retention maintenance completed")
            }.onFailure { error ->
                Log.e(AppContainer.TAG, "Retention maintenance failed", error)
            }
        }
        container.appScope.launch {
            // Reminders are scheduled from "now", so time/timezone changes silently shift
            // them; re-anchoring on every process start (plus the TIME_SET/TIMEZONE_CHANGED
            // receiver) keeps the wall-clock times honest.
            runCatching {
                ReminderScheduler.rescheduleAll(
                    this@CalorieCounterApplication,
                    container.settingsRepository,
                )
            }.onFailure { error -> Log.e(AppContainer.TAG, "Reminder rescheduling failed", error) }
        }
        container.appScope.launch {
            runCatching {
                if (container.settingsRepository.autoBackupUri.first() != null) {
                    AutoBackupWorker.schedule(this@CalorieCounterApplication)
                }
            }.onFailure { error -> Log.e(AppContainer.TAG, "Auto-backup scheduling failed", error) }
        }
    }
}
