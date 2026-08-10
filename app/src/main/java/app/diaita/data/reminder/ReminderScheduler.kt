package app.diaita.data.reminder

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import app.diaita.data.settings.DayPartBoundaries
import app.diaita.data.settings.SettingsRepository
import kotlinx.coroutines.flow.first
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit

/**
 * Schedules the daily [ReminderWorker] instances around the user-configured times
 * (WorkManager's periodic timing is approximate by design — battery-friendly "around then").
 */
object ReminderScheduler {
    private const val FOOD_WORK_NAME = "daily_log_reminder"
    private const val WEIGHT_WORK_NAME = "daily_weight_reminder"
    private const val DAY_PART_MORNING_WORK_NAME = "day_part_reminder_morning"
    private const val DAY_PART_DAY_WORK_NAME = "day_part_reminder_day"
    private const val EVENING_WORK_NAME = "day_part_reminder_evening"

    /**
     * The evening part only "closes" at midnight, when a nudge is useless — so the evening
     * check fires at a fixed late-evening time while there is still a chance to log.
     */
    const val EVENING_CHECK_MINUTES = 21 * 60

    /**
     * Re-anchors every enabled reminder to the current clock and settings. Called on app
     * start and from [TimeChangedReceiver] after TIME_SET/TIMEZONE_CHANGED, because the
     * initial delays are computed from "now" and silently drift across such changes.
     */
    suspend fun rescheduleAll(
        context: Context,
        settings: SettingsRepository,
    ) {
        if (settings.reminderEnabled.first()) {
            scheduleFoodReminder(context, settings.reminderTimeMinutes.first())
        } else {
            cancelFoodReminder(context)
        }
        if (settings.weightReminderEnabled.first()) {
            scheduleWeightReminder(context, settings.weightReminderTimeMinutes.first())
        } else {
            cancelWeightReminder(context)
        }
        if (settings.dayPartRemindersEnabled.first()) {
            scheduleDayPartReminders(context, settings.dayPartBoundaries.first())
        } else {
            cancelDayPartReminders(context)
        }
        if (settings.eveningReminderEnabled.first()) {
            scheduleEveningReminder(context)
        } else {
            cancelEveningReminder(context)
        }
    }

    fun scheduleEveningReminder(context: Context) =
        schedule(context, EVENING_WORK_NAME, EVENING_CHECK_MINUTES, ReminderWorker.TYPE_DAY_PART_EVENING)

    fun cancelEveningReminder(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(EVENING_WORK_NAME)
    }

    fun scheduleFoodReminder(
        context: Context,
        timeMinutes: Int,
    ) = schedule(context, FOOD_WORK_NAME, timeMinutes, ReminderWorker.TYPE_FOOD)

    fun cancelFoodReminder(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(FOOD_WORK_NAME)
    }

    fun scheduleWeightReminder(
        context: Context,
        timeMinutes: Int,
    ) = schedule(context, WEIGHT_WORK_NAME, timeMinutes, ReminderWorker.TYPE_WEIGHT)

    fun cancelWeightReminder(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WEIGHT_WORK_NAME)
    }

    /**
     * Two daily checks firing right when a day part closes (the user-configured boundaries):
     * each nudges only when the part that just ended has nothing logged.
     */
    fun scheduleDayPartReminders(
        context: Context,
        boundaries: DayPartBoundaries,
    ) {
        schedule(
            context,
            DAY_PART_MORNING_WORK_NAME,
            boundaries.morningEndMinutes,
            ReminderWorker.TYPE_DAY_PART_MORNING,
        )
        schedule(
            context,
            DAY_PART_DAY_WORK_NAME,
            boundaries.dayEndMinutes,
            ReminderWorker.TYPE_DAY_PART_DAY,
        )
    }

    fun cancelDayPartReminders(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(DAY_PART_MORNING_WORK_NAME)
        WorkManager.getInstance(context).cancelUniqueWork(DAY_PART_DAY_WORK_NAME)
    }

    private fun schedule(
        context: Context,
        workName: String,
        timeMinutes: Int,
        type: String,
    ) {
        val reminderTime = LocalTime.of(timeMinutes / 60, timeMinutes % 60)
        val now = LocalDateTime.now()
        var firstRun = now.toLocalDate().atTime(reminderTime)
        if (!firstRun.isAfter(now)) firstRun = firstRun.plusDays(1)
        val initialDelay = Duration.between(now, firstRun)

        val request =
            PeriodicWorkRequestBuilder<ReminderWorker>(1, TimeUnit.DAYS)
                .setInitialDelay(initialDelay.toMillis(), TimeUnit.MILLISECONDS)
                .setInputData(workDataOf(ReminderWorker.KEY_TYPE to type))
                .build()
        // The initial delay is computed from "now", so re-scheduling (e.g. after a time change)
        // must re-enqueue rather than UPDATE — UPDATE keeps the original enqueue clock.
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            workName,
            ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE,
            request,
        )
    }
}
