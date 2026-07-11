package app.diaita.ui.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import app.diaita.R
import app.diaita.data.backup.RestoreOutcome
import app.diaita.ui.AppViewModelProvider
import kotlinx.coroutines.launch
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var working by remember { mutableStateOf(false) }
    var pendingRestoreUri by remember { mutableStateOf<Uri?>(null) }

    val exportedMessage = stringResource(R.string.diary_exported)
    val backupExportedMessage = stringResource(R.string.backup_exported)
    val exportFailedMessage = stringResource(R.string.backup_export_failed)
    val restoreFailedInvalid = stringResource(R.string.restore_failed_invalid)
    val restoreFailedNewer = stringResource(R.string.restore_failed_newer)
    val restoreFailedGeneric = stringResource(R.string.restore_failed_generic)

    val csvLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.CreateDocument("text/csv"),
        ) { uri ->
            if (uri == null) return@rememberLauncherForActivityResult
            scope.launch {
                working = true
                val csv = viewModel.exportDiaryCsv()
                context.contentResolver.openOutputStream(uri)?.use { it.write(csv.toByteArray()) }
                working = false
                snackbarHostState.showSnackbar(exportedMessage)
            }
        }

    val backupLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.CreateDocument("application/octet-stream"),
        ) { uri ->
            if (uri == null) return@rememberLauncherForActivityResult
            scope.launch {
                working = true
                val ok = viewModel.exportDatabase(uri)
                working = false
                snackbarHostState.showSnackbar(if (ok) backupExportedMessage else exportFailedMessage)
            }
        }

    val restoreLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.OpenDocument(),
        ) { uri ->
            if (uri != null) pendingRestoreUri = uri
        }

    pendingRestoreUri?.let { uri ->
        AlertDialog(
            onDismissRequest = { pendingRestoreUri = null },
            title = { Text(stringResource(R.string.restore_confirm_title)) },
            text = { Text(stringResource(R.string.restore_confirm_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingRestoreUri = null
                        scope.launch {
                            working = true
                            val outcome = viewModel.restoreDatabase(uri)
                            working = false
                            when (outcome) {
                                RestoreOutcome.Success -> restartApp(context)
                                RestoreOutcome.NotADatabase, RestoreOutcome.Corrupt ->
                                    snackbarHostState.showSnackbar(restoreFailedInvalid)
                                RestoreOutcome.NewerSchema ->
                                    snackbarHostState.showSnackbar(restoreFailedNewer)
                                RestoreOutcome.Failed ->
                                    snackbarHostState.showSnackbar(restoreFailedGeneric)
                            }
                        }
                    },
                ) { Text(stringResource(R.string.restore)) }
            },
            dismissButton = {
                TextButton(onClick = { pendingRestoreUri = null }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.backup_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .padding(16.dp),
        ) {
            if (working) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(16.dp))
            }
            Text(
                stringResource(R.string.backup_full_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = {
                    backupLauncher.launch("diaita-backup-${LocalDate.now()}.db")
                },
                enabled = !working,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.backup_export_db))
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = { restoreLauncher.launch(arrayOf("application/octet-stream", "*/*")) },
                enabled = !working,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.backup_restore))
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                stringResource(R.string.backup_csv_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = { csvLauncher.launch("calorie_counter_export.csv") },
                enabled = !working,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.export_csv))
            }
        }
    }
}

/** Relaunches the app in a fresh process so Room reopens the restored database file. */
private fun restartApp(context: Context) {
    val intent = checkNotNull(context.packageManager.getLaunchIntentForPackage(context.packageName))
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
    context.startActivity(intent)
    Runtime.getRuntime().exit(0)
}
