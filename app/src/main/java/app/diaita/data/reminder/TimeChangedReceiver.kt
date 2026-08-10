package app.diaita.data.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import app.diaita.AppContainer
import app.diaita.CalorieCounterApplication
import kotlinx.coroutines.launch

/**
 * Re-anchors all reminders after the wall clock or timezone changes — WorkManager delays
 * are relative, so a tz/DST change would otherwise shift every reminder by the offset.
 */
class TimeChangedReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        if (intent.action != Intent.ACTION_TIME_CHANGED &&
            intent.action != Intent.ACTION_TIMEZONE_CHANGED
        ) {
            return
        }
        val app = context.applicationContext as? CalorieCounterApplication ?: return
        val pendingResult = goAsync()
        app.container.appScope.launch {
            try {
                ReminderScheduler.rescheduleAll(context.applicationContext, app.container.settingsRepository)
            } catch (e: Exception) {
                Log.e(AppContainer.TAG, "Reminder rescheduling after time change failed", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
