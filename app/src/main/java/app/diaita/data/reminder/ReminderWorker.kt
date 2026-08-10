package app.diaita.data.reminder

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import app.diaita.AppContainer
import app.diaita.CalorieCounterApplication
import app.diaita.R
import app.diaita.ui.diary.DayPart
import app.diaita.ui.diary.dayPartOf
import app.diaita.util.DateUtils
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first

/**
 * Daily nudges scheduled by [ReminderScheduler]. The [KEY_TYPE] input picks the check:
 * [TYPE_FOOD] posts when nothing has been logged today, [TYPE_WEIGHT] when no weight has
 * been recorded, and the day-part types when the just-ended part of the day logged nothing —
 * either way a quiet day-off for people who already did it.
 */
class ReminderWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val container =
            (applicationContext as? CalorieCounterApplication)?.container
                ?: return Result.failure()
        return try {
            val today = DateUtils.today()
            when (inputData.getString(KEY_TYPE)) {
                TYPE_WEIGHT -> {
                    if (container.weightRepository.forDate(today) == null) {
                        showNotification(R.string.weight_reminder_text, WEIGHT_NOTIFICATION_ID)
                    }
                }
                TYPE_DAY_PART_MORNING -> checkDayPart(container, today, DayPart.MORNING)
                TYPE_DAY_PART_DAY -> checkDayPart(container, today, DayPart.DAY)
                TYPE_DAY_PART_EVENING -> checkDayPart(container, today, DayPart.EVENING)
                else -> {
                    if (container.diaryRepository.entriesForDateOnce(today).isEmpty()) {
                        showNotification(R.string.reminder_text, FOOD_NOTIFICATION_ID)
                    }
                }
            }
            Result.success()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // A transient DB/read failure shouldn't silently eat the day's nudge.
            Result.retry()
        }
    }

    /** Nudges when the just-ended (or, for evening, still-running) [part] has no logged entries. */
    private suspend fun checkDayPart(
        container: AppContainer,
        today: String,
        part: DayPart,
    ) {
        val boundaries = container.settingsRepository.dayPartBoundaries.first()
        val entries = container.diaryRepository.entriesForDateOnce(today)
        val partIsEmpty = entries.none { dayPartOf(it, boundaries) == part }
        if (partIsEmpty) {
            when (part) {
                DayPart.MORNING ->
                    showNotification(R.string.day_part_reminder_morning_text, DAY_PART_MORNING_NOTIFICATION_ID)
                DayPart.DAY ->
                    showNotification(R.string.day_part_reminder_day_text, DAY_PART_DAY_NOTIFICATION_ID)
                DayPart.EVENING ->
                    showNotification(R.string.day_part_reminder_evening_text, DAY_PART_EVENING_NOTIFICATION_ID)
            }
        }
    }

    private fun showNotification(
        textRes: Int,
        notificationId: Int,
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val manager = applicationContext.getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    applicationContext.getString(R.string.reminder_channel),
                    NotificationManager.IMPORTANCE_DEFAULT,
                ),
            )
        }

        val launchIntent =
            applicationContext.packageManager
                .getLaunchIntentForPackage(applicationContext.packageName)
        val contentIntent =
            PendingIntent.getActivity(
                applicationContext,
                0,
                launchIntent ?: Intent(),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )

        val notification =
            NotificationCompat
                .Builder(applicationContext, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(applicationContext.getString(R.string.app_name))
                .setContentText(applicationContext.getString(textRes))
                .setContentIntent(contentIntent)
                .setAutoCancel(true)
                .build()

        manager.notify(notificationId, notification)
    }

    companion object {
        const val CHANNEL_ID = "reminders"
        const val KEY_TYPE = "reminder_type"
        const val TYPE_FOOD = "food"
        const val TYPE_WEIGHT = "weight"
        const val TYPE_DAY_PART_MORNING = "day_part_morning"
        const val TYPE_DAY_PART_DAY = "day_part_day"
        const val TYPE_DAY_PART_EVENING = "day_part_evening"
        const val FOOD_NOTIFICATION_ID = 1
        const val WEIGHT_NOTIFICATION_ID = 2
        const val DAY_PART_MORNING_NOTIFICATION_ID = 3
        const val DAY_PART_DAY_NOTIFICATION_ID = 4
        const val DAY_PART_EVENING_NOTIFICATION_ID = 5
    }
}
