package app.diaita.data.reminder

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit

/**
 * Schedules the daily [ReminderWorker] around [REMINDER_TIME] (WorkManager's periodic
 * timing is approximate by design — a battery-friendly "sometime in the evening").
 */
object ReminderScheduler {
    private const val WORK_NAME = "daily_log_reminder"
    private val REMINDER_TIME: LocalTime = LocalTime.of(20, 0)

    fun schedule(context: Context) {
        val now = LocalDateTime.now()
        var firstRun = now.toLocalDate().atTime(REMINDER_TIME)
        if (!firstRun.isAfter(now)) firstRun = firstRun.plusDays(1)
        val initialDelay = Duration.between(now, firstRun)

        val request =
            PeriodicWorkRequestBuilder<ReminderWorker>(1, TimeUnit.DAYS)
                .setInitialDelay(initialDelay.toMillis(), TimeUnit.MILLISECONDS)
                .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }
}
