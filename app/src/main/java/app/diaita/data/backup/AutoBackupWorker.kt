package app.diaita.data.backup

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import app.diaita.AppContainer
import app.diaita.DiaitaApplication
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.util.concurrent.TimeUnit

/**
 * Weekly automatic backup into the user-chosen SAF folder. Each run creates a fresh
 * dated file (never overwrites), so the folder accumulates restore points; the user
 * prunes old ones at their own pace.
 */
class AutoBackupWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val container =
            (applicationContext as? DiaitaApplication)?.container
                ?: return Result.failure()
        val treeUriString =
            container.settingsRepository.autoBackupUri.first()
                ?: return Result.success()
        return try {
            val treeUri = Uri.parse(treeUriString)
            val parentDocument =
                DocumentsContract.buildDocumentUriUsingTree(
                    treeUri,
                    DocumentsContract.getTreeDocumentId(treeUri),
                )
            val fileUri =
                DocumentsContract.createDocument(
                    applicationContext.contentResolver,
                    parentDocument,
                    "application/octet-stream",
                    "diaita-auto-backup-${LocalDate.now()}.db",
                ) ?: return Result.retry()
            container.backupManager.exportTo(fileUri)
            container.settingsRepository.setLastBackupAt(System.currentTimeMillis())
            Result.success()
        } catch (e: CancellationException) {
            throw e
        } catch (e: SecurityException) {
            // The persisted folder permission was revoked — turn auto-backup off instead
            // of failing silently forever.
            Log.w(AppContainer.TAG, "Auto-backup folder permission lost; disabling", e)
            container.settingsRepository.setAutoBackupUri(null)
            cancel(applicationContext)
            Result.failure()
        } catch (e: Exception) {
            Log.e(AppContainer.TAG, "Auto-backup failed", e)
            Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME = "auto_backup"

        fun schedule(context: Context) {
            val request =
                PeriodicWorkRequestBuilder<AutoBackupWorker>(7, TimeUnit.DAYS).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
