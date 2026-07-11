package app.diaita.data.backup

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import app.diaita.data.db.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.RandomAccessFile

sealed interface RestoreOutcome {
    /** Database file replaced — the process must be restarted before any further DB access. */
    data object Success : RestoreOutcome

    data object NotADatabase : RestoreOutcome

    data object NewerSchema : RestoreOutcome

    data object Corrupt : RestoreOutcome

    data object Failed : RestoreOutcome
}

/**
 * Full-database backup: exports the raw SQLite file and restores it back. The backup
 * format is versioned for free by Room's migration chain — restoring an old backup just
 * replays migrations on next open, which is why restore only rejects files whose
 * `user_version` is NEWER than the running app's schema.
 */
class DatabaseBackupManager(
    private val context: Context,
    private val database: AppDatabase,
) {
    suspend fun exportTo(uri: Uri) =
        withContext(Dispatchers.IO) {
            // Fold the WAL into the main file so a single-file copy is complete and consistent.
            database.query("PRAGMA wal_checkpoint(TRUNCATE)", null).use { it.moveToFirst() }
            val dbFile = context.getDatabasePath(AppDatabase.DATABASE_NAME)
            val stream =
                context.contentResolver.openOutputStream(uri)
                    ?: error("Could not open backup destination")
            stream.use { out -> dbFile.inputStream().use { it.copyTo(out) } }
        }

    suspend fun restoreFrom(uri: Uri): RestoreOutcome =
        withContext(Dispatchers.IO) {
            val staging = File(context.cacheDir, "restore-staging.db")
            try {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    staging.outputStream().use { input.copyTo(it) }
                } ?: return@withContext RestoreOutcome.Failed

                if (!hasSqliteHeader(staging)) return@withContext RestoreOutcome.NotADatabase
                val currentVersion = database.openHelper.readableDatabase.version
                if (readUserVersion(staging) > currentVersion) return@withContext RestoreOutcome.NewerSchema
                if (!passesIntegrityCheck(staging)) return@withContext RestoreOutcome.Corrupt

                val dbFile = context.getDatabasePath(AppDatabase.DATABASE_NAME)
                database.close()
                File(dbFile.path + "-wal").delete()
                File(dbFile.path + "-shm").delete()
                staging.copyTo(dbFile, overwrite = true)
                RestoreOutcome.Success
            } catch (e: Exception) {
                RestoreOutcome.Failed
            } finally {
                staging.delete()
            }
        }

    private fun hasSqliteHeader(file: File): Boolean {
        if (file.length() < SQLITE_HEADER_SIZE) return false
        val header = ByteArray(SQLITE_MAGIC.size)
        RandomAccessFile(file, "r").use { it.readFully(header) }
        return header.contentEquals(SQLITE_MAGIC)
    }

    /** `PRAGMA user_version` lives at byte offset 60 of the SQLite header, big-endian. */
    private fun readUserVersion(file: File): Int =
        RandomAccessFile(file, "r").use {
            it.seek(USER_VERSION_OFFSET)
            it.readInt()
        }

    private fun passesIntegrityCheck(file: File): Boolean =
        runCatching {
            SQLiteDatabase.openDatabase(file.path, null, SQLiteDatabase.OPEN_READONLY).use { db ->
                db.rawQuery("PRAGMA integrity_check", null).use { cursor ->
                    cursor.moveToFirst() && cursor.getString(0) == "ok"
                }
            }
        }.getOrDefault(false)

    private companion object {
        val SQLITE_MAGIC = "SQLite format 3\u0000".toByteArray(Charsets.US_ASCII)
        const val SQLITE_HEADER_SIZE = 100L
        const val USER_VERSION_OFFSET = 60L
    }
}
